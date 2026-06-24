from datetime import date, timedelta
from pathlib import Path
from typing import Literal, Optional
from urllib.parse import unquote

from fastapi import Depends, FastAPI, HTTPException, Query, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, PlainTextResponse
from starlette.templating import Jinja2Templates

import util.dbUtils as dbUtils
from config import firebaseWebApiKey
from util.authIdentity import signInWithEmailPassword
from util.deviceTracking import extractClientIp, lookupIpLocation, readUserAgent
from models import (
    AuthCredentials,
    AuthTokenResponse,
    DailyThoughtResponse,
    DeviceEventResponse,
    HabitCheckRequest,
    HabitCreate,
    HabitEntryCreate,
    HabitEntryUpdate,
    HabitUpdate,
    MediaCompleteRequest,
    MediaItemCreate,
    MediaItemResponse,
    MediaItemUpdate,
    ServerClockResponse,
    UserCreate,
    HabitProgressResponse,
)
from util.dailyThought import fetch_daily_thought
from util.security import (
    requireBookOwner,
    requireEntryOwner,
    requireHabitOwner,
    requireMovieOwner,
    verifyFirebaseToken,
)
from util.timeUtils import isoUtcNow, utcToday

_APP_DESCRIPTION = """
## Authentication

Clients **do not** talk to Firebase directly for login. Use only these HTTP APIs:

- **`POST /api/auth/signup`** — create account (Firestore + Firebase Auth) and receive **`accessToken`**
- **`POST /api/auth/login`** — receive **`accessToken`** for an existing account

Send every protected request with:

`Authorization: Bearer <accessToken>`

The token is a standard Firebase ID JWT verified on the server (`verifyFirebaseToken`). Same header works from Android, web, or scripts.

**Time:** use **`GET /api/clock`** for the authoritative UTC calendar date. Habit check-ins (`POST /api/habits/check`) assign the entry day from the server — do not send a client `date`.

## Habits, progress, and weekly rules

- All entry **`date`** values are **UTC calendar days** (same as `GET /api/clock` → `utcCalendarDate`).
- **Daily** habits: one opportunity per UTC day; progress rollups and **`last14Days`** in `GET /api/habits/{id}/progress` are day-based.
- **Weekly** habits: one completed check per **UTC ISO week** (Monday through Sunday). A new week starts **Monday 00:00 UTC**. `GET /api/habits/{id}/progress` returns weekly rollups and **`last14Weeks`** (14 dots, each **`calendarDate`** = that week’s Monday); **`last14Days`** is an empty array for weekly habits.

## Inspiration

- **`GET /api/thought`** (also **`GET /thought`**) — public; returns a thought / quote (proxied from ZenQuotes or Quotable, with a local fallback). Optional query **`format=text`** for a plain-text body.

## Movies and books (watch / read lists)

Separate from habits. Each user owns two Firestore **subcollections**:

- **`users/{userId}/movies`** — watchlist items
- **`users/{userId}/books`** — read-list items

Document fields: **`id`**, **`userId`**, **`title`**, **`completed`**, **`rating`** (1–5 or null), **`review`**, **`completedAt`**, **`createdAt`**.

| Action | Movies | Books |
|--------|--------|-------|
| Add | `POST /api/movies` `{ "title": "..." }` | `POST /api/books` `{ "title": "..." }` |
| List | `GET /api/users/{userId}/movies` | `GET /api/users/{userId}/books` |
| Get / update / delete | `GET`/`PUT`/`DELETE /api/movies/{movieId}` | `GET`/`PUT`/`DELETE /api/books/{bookId}` |
| Mark done | `POST /api/movies/{movieId}/complete` | `POST /api/books/{bookId}/complete` |

**Completing** (`completed: true`): **`rating`** (1–5) is **required** for both. **`review`** is **required for movies**, **optional for books**.  
**Undo** (`completed: false`): clears **`rating`**, **`review`**, and **`completedAt`**.  
Use **`PUT`** to edit **`title`**, **`rating`**, or **`review`** on an existing item without changing completion time logic on movies (title-only updates are fine anytime).

The dev web UI at **`GET /app`** includes movies and books sections (star picker + review modal for movies).

## Cross-origin (CORS)

**All origins** are allowed (`Access-Control-Allow-Origin: *`). `allow_credentials` is **false** (required for `*`); use the **`Authorization: Bearer`** header for auth, not cookies. Browsers on **localhost**, other sites, and **native** apps (which do not use CORS) can call the API.
"""

_OPENAPI_TAGS = [
    {
        "name": "auth",
        "description": "Sign up and log in. Returns **accessToken**; use it as the Bearer token everywhere else.",
    },
    {"name": "health", "description": "Liveness check."},
    {
        "name": "users",
        "description": "Profiles and lookups (requires Bearer token; self-only).",
    },
    {
        "name": "habits",
        "description": (
            "Create and manage habits. **`GET /api/habits/{id}/progress`** returns aggregates over optional "
            "`startDate`/`endDate` (defaults: 42 UTC days ending today). Weekly habits use ISO weeks and **`last14Weeks`**."
        ),
    },
    {
        "name": "habit-entries",
        "description": (
            "Completion rows and **`POST /api/habits/check`** (server assigns UTC day). Weekly: at most one "
            "`completed: true` per ISO week unless you update another day after undoing."
        ),
    },
    {
        "name": "inspiration",
        "description": "Thought / quote of the day (**`GET /api/thought`**). No authentication.",
    },
    {
        "name": "movies",
        "description": (
            "Personal watchlist stored under **`users/{userId}/movies`**. "
            "Marking watched requires **rating** (1–5) and **review**."
        ),
    },
    {
        "name": "books",
        "description": (
            "Personal read list stored under **`users/{userId}/books`**. "
            "Marking read requires **rating** (1–5); **review** is optional."
        ),
    },
]

app = FastAPI(
    title="Habit Tracker API",
    version="1.1.0",
    description=_APP_DESCRIPTION,
    openapi_tags=_OPENAPI_TAGS,
    swagger_ui_parameters={"persistAuthorization": True},
    servers=[
        {
            "url": "https://habit.thatinsaneguy.com",
            "description": "Production (TLS)",
        },
        {
            "url": "http://127.0.0.1:9210",
            "description": "Local dev (default HABITTRACKER_PORT; override in .env)",
        },
    ],
    contact={
        "name": "Habit Tracker",
        "url": "https://habit.thatinsaneguy.com",
    },
    license_info={
        "name": "MIT",
        "url": "https://opensource.org/licenses/MIT",
    },
)


def buildAuthTokenResponse(data: dict) -> AuthTokenResponse:
    return AuthTokenResponse(
        accessToken=data["idToken"],
        expiresIn=str(data.get("expiresIn", "")),
        userId=data["localId"],
    )

# CORS: allow any origin (localhost, file:// dev, mobile WebViews, other domains).
# `allow_credentials` must be False when using `*` — OK here because auth uses
# `Authorization: Bearer`, not cookies.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
    max_age=86400,
)

_APP_DIR = Path(__file__).resolve().parent
templates = Jinja2Templates(directory=str(_APP_DIR / "templates"))


@app.get(
    "/",
    response_class=HTMLResponse,
    include_in_schema=False,
    summary="Landing page",
)
async def rootPage(request: Request):
    return templates.TemplateResponse(
        request,
        "landing.html",
        {"request": request},
    )


@app.get(
    "/app",
    response_class=HTMLResponse,
    include_in_schema=False,
    summary="Web habit UI",
)
async def appPage(request: Request):
    return templates.TemplateResponse(
        request,
        "app.html",
        {
            "request": request,
            "hasAuthBackend": bool(firebaseWebApiKey),
        },
    )


@app.get(
    "/api/health",
    tags=["health"],
    summary="Liveness probe",
    description="Returns `{\"status\":\"ok\"}`. No authentication.",
)
async def health():
    return {"status": "ok"}


@app.get(
    "/api/clock",
    response_model=ServerClockResponse,
    tags=["health"],
    summary="Server UTC clock",
    description=(
        "Authoritative **UTC** calendar date and timestamp. Use **`utcCalendarDate`** when querying "
        "`/api/habits/{id}/entries` so the UI matches **`POST /api/habits/check`** (server assigns the entry day)."
    ),
)
async def serverClock():
    return ServerClockResponse(
        utcCalendarDate=utcToday(),
        utcDateTime=isoUtcNow(),
        timezone="UTC",
    )


@app.get(
    "/api/thought",
    tags=["inspiration"],
    summary="Thought or quote of the day",
    description=(
        "Public. Fetches from **ZenQuotes** (`/api/today`) when possible, otherwise **Quotable** (`/random`), "
        "then a short built-in line if both fail. Use **`format=text`** for `text/plain` "
        "(quote and optional author)."
    ),
    responses={
        200: {
            "description": "JSON or plain text",
            "content": {
                "application/json": {},
                "text/plain": {"schema": {"type": "string", "example": '"Example" — Author'}},
            },
        }
    },
)
@app.get(
    "/thought",
    tags=["inspiration"],
    summary="Thought or quote of the day (alias)",
    description="Same as **`GET /api/thought`**.",
    responses={
        200: {
            "description": "JSON or plain text",
            "content": {
                "application/json": {},
                "text/plain": {"schema": {"type": "string"}},
            },
        }
    },
)
async def daily_thought(
    response_format: Literal["json", "text"] = Query(
        "json",
        alias="format",
        description="`json` (default) or `text` for a plain-text body.",
    ),
):
    text, author, src = await fetch_daily_thought()
    if response_format == "text":
        if author:
            body = f'"{text}" — {author}'
        else:
            body = text
        return PlainTextResponse(body)
    return DailyThoughtResponse(text=text, author=author, source=src)


# --- Auth (public) -----------------------------------------------------------

@app.post(
    "/api/auth/login",
    response_model=AuthTokenResponse,
    tags=["auth"],
    summary="Log in",
    description=(
        "Verify email and password; returns **accessToken**. "
        "Use `Authorization: Bearer <accessToken>` on all protected routes."
    ),
)
async def authLogin(body: AuthCredentials, request: Request):
    try:
        data = await signInWithEmailPassword(body.email, body.password)
    except ValueError as e:
        msg = str(e)
        if "misconfiguration" in msg.lower() or "FIREBASE_WEB_API_KEY" in msg:
            raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, detail=msg)
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, detail=msg)
    try:
        ipAddress = extractClientIp(request)
        location = await lookupIpLocation(ipAddress)
        await dbUtils.saveDeviceEvent(
            userId=data["localId"],
            eventName="login",
            ipAddress=ipAddress,
            userAgent=readUserAgent(request),
            location=location,
        )
    except Exception:
        # Never fail login if telemetry storage/geolocation is unavailable.
        pass
    return buildAuthTokenResponse(data)


@app.post(
    "/api/auth/signup",
    status_code=status.HTTP_201_CREATED,
    response_model=AuthTokenResponse,
    tags=["auth"],
    summary="Sign up",
    description=(
        "Create Firebase Auth + Firestore user, then return **accessToken** "
        "(same as `POST /api/users` followed by login)."
    ),
)
async def authSignup(userData: UserCreate, request: Request):
    try:
        await dbUtils.createUser(userData)
    except Exception as e:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, detail=str(e))
    try:
        data = await signInWithEmailPassword(userData.email, userData.password)
    except ValueError as e:
        msg = str(e)
        if "misconfiguration" in msg.lower() or "FIREBASE_WEB_API_KEY" in msg:
            raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, detail=msg)
        raise HTTPException(
            status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"Account created but token issuance failed: {msg}",
        )
    try:
        ipAddress = extractClientIp(request)
        location = await lookupIpLocation(ipAddress)
        await dbUtils.saveDeviceEvent(
            userId=data["localId"],
            eventName="signup",
            ipAddress=ipAddress,
            userAgent=readUserAgent(request),
            location=location,
        )
    except Exception:
        # Never fail signup if telemetry storage/geolocation is unavailable.
        pass
    return buildAuthTokenResponse(data)


# --- Registration (no token; prefer /api/auth/signup) ------------------------

@app.post(
    "/api/users",
    status_code=status.HTTP_201_CREATED,
    tags=["users"],
    summary="Create user (no token returned)",
    description="Creates the account only. Prefer **POST /api/auth/signup** to also receive accessToken.",
)
async def createUser(userData: UserCreate):
    try:
        user = await dbUtils.createUser(userData)
        return user
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


# --- Authenticated users -----------------------------------------------------

@app.get(
    "/api/me",
    tags=["users"],
    summary="Current user profile",
    description="Firestore profile for the Bearer token UID.",
)
async def getMe(currentUser: dict = Depends(verifyFirebaseToken)):
    try:
        return await dbUtils.getUser(currentUser["uid"])
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.get(
    "/api/me/devices",
    response_model=list[DeviceEventResponse],
    tags=["users"],
    summary="Recent device login events",
    description=(
        "Returns the authenticated user's latest device/auth events "
        "(IP, user-agent, coarse IP location)."
    ),
)
async def getMyDeviceEvents(
    limit: int = Query(
        50,
        ge=1,
        le=200,
        description="Maximum number of newest events to return (1-200).",
    ),
    currentUser: dict = Depends(verifyFirebaseToken),
):
    try:
        return await dbUtils.getUserDeviceEvents(currentUser["uid"], limit)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get(
    "/api/users/{userId}",
    tags=["users"],
    summary="Get user by id",
    description="**userId** must equal the token UID (self-only).",
)
async def getUser(userId: str, currentUser: dict = Depends(verifyFirebaseToken)):
    if userId != currentUser["uid"]:
        raise HTTPException(status_code=403, detail="You can only access your own profile")
    try:
        return await dbUtils.getUser(userId)
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.get(
    "/api/users/email/{email}",
    tags=["users"],
    summary="Look up user by email",
    description="**email** must match the token’s email (URL-encoded if needed).",
)
async def getUserByEmail(email: str, currentUser: dict = Depends(verifyFirebaseToken)):
    email = unquote(email).strip().lower()
    tokenEmail = (currentUser.get("email") or "").strip().lower()
    if email != tokenEmail:
        raise HTTPException(status_code=403, detail="You can only look up your own email")
    try:
        return await dbUtils.getUserByEmail(email)
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.put(
    "/api/users/{userId}/name",
    tags=["users"],
    summary="Update display name",
    description="Body: `{\"name\": \"...\"}`. **userId** must match the token.",
)
async def updateUserName(
    userId: str,
    name: dict,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    if userId != currentUser["uid"]:
        raise HTTPException(status_code=403, detail="You can only update your own profile")
    try:
        userName = name.get("name", "")
        return await dbUtils.updateUserName(userId, userName)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


# --- Habits ------------------------------------------------------------------

@app.post(
    "/api/habits",
    status_code=status.HTTP_201_CREATED,
    tags=["habits"],
    summary="Create habit",
    description=(
        "**frequency** `daily` | `weekly`. **startDate** must be a UTC calendar date (see **`GET /api/clock`**). "
        "`userId` in the body is ignored; the owner is the Bearer UID."
    ),
)
async def createHabit(
    habitData: HabitCreate,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    try:
        payload = HabitCreate(
            title=habitData.title,
            frequency=habitData.frequency,
            startDate=habitData.startDate,
            userId=currentUser["uid"],
        )
        return await dbUtils.createHabit(payload)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get(
    "/api/habits/{habitId}",
    tags=["habits"],
    summary="Get habit by id",
    description="Returns one habit document (must own the habit).",
)
async def getHabit(habitId: str, currentUser: dict = Depends(verifyFirebaseToken)):
    await requireHabitOwner(currentUser["uid"], habitId)
    try:
        return await dbUtils.getHabit(habitId)
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.get(
    "/api/users/{userId}/habits",
    tags=["habits"],
    summary="List habits for user",
    description="**userId** must match the token. Optional **includeArchived** (default false).",
)
async def getUserHabits(
    userId: str,
    includeArchived: bool = False,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    if userId != currentUser["uid"]:
        raise HTTPException(status_code=403, detail="You can only list your own habits")
    try:
        return await dbUtils.getUserHabits(userId, includeArchived)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.put(
    "/api/habits/{habitId}",
    tags=["habits"],
    summary="Update habit",
    description="Partial update: **title**, **frequency**, **startDate**, **isArchived**.",
)
async def updateHabit(
    habitId: str,
    habitUpdate: HabitUpdate,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireHabitOwner(currentUser["uid"], habitId)
    try:
        return await dbUtils.updateHabit(habitId, habitUpdate)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.delete(
    "/api/habits/{habitId}",
    status_code=status.HTTP_204_NO_CONTENT,
    tags=["habits"],
    summary="Delete habit",
    description="Removes the habit and related entries (see `util/dbUtils.deleteHabit`).",
)
async def deleteHabit(habitId: str, currentUser: dict = Depends(verifyFirebaseToken)):
    await requireHabitOwner(currentUser["uid"], habitId)
    try:
        await dbUtils.deleteHabit(habitId)
        return None
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get(
    "/api/habits/{habitId}/progress",
    response_model=HabitProgressResponse,
    tags=["habits"],
    summary="Progress stats for a UTC date range",
    description=(
        "Defaults: **endDate** = today UTC, **startDate** = 41 days earlier (42-day window). "
        "**Daily:** `scheduledOpportunities`, streaks, and missed/skipped are **per UTC day** in range. "
        "**last14Days** = fourteen consecutive UTC days ending at **endDate** (heatmap oldest→newest). "
        "**Weekly:** rollups are **per UTC ISO week** (Mon–Sun); "
        "**done** for a week = any day that week has `completed: true`. "
        "**last14Weeks** = fourteen ISO weeks ending at the week that contains **endDate**; "
        "each snapshot’s **calendarDate** is that week’s **Monday** ( **`last14Days` is `[]`** ). "
        "**missed** / **skipped** meanings match `util/dbUtils.getHabitProgress` (daily vs weekly branches)."
    ),
)
async def getHabitProgressRoute(
    habitId: str,
    startDate: Optional[date] = None,
    endDate: Optional[date] = None,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireHabitOwner(currentUser["uid"], habitId)
    try:
        end = endDate if endDate is not None else utcToday()
        start = startDate if startDate is not None else end - timedelta(days=41)
        if start > end:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="startDate must be on or before endDate",
            )
        raw = await dbUtils.getHabitProgress(habitId, start, end)
        return HabitProgressResponse.model_validate(raw)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


# --- Habit entries -----------------------------------------------------------

@app.post(
    "/api/habit-entries",
    status_code=status.HTTP_201_CREATED,
    tags=["habit-entries"],
    summary="Create habit entry (explicit date)",
    description=(
        "Prefer **`POST /api/habits/check`** for normal check-ins (server UTC day). "
        "Use this when supplying **`date`** yourself for backfills or tooling."
    ),
)
async def createHabitEntry(
    entryData: HabitEntryCreate,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireHabitOwner(currentUser["uid"], entryData.habitId)
    try:
        return await dbUtils.createHabitEntry(entryData)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post(
    "/api/habits/check",
    tags=["habit-entries"],
    summary="Check habit (server UTC day)",
    description=(
        "The entry’s calendar **`date`** and **`completedAt`** are set on the server only. "
        "The day bucket is **today’s date in UTC** (same as **`GET /api/clock`** → `utcCalendarDate`). "
        "Do not send a client `date`. Align habit **`startDate`** with UTC (see clock endpoint). "
        "**Weekly** habits allow at most one **`completed: true`** per **UTC ISO week** (Mon–Sun); "
        "after Sunday (UTC), the next window starts Monday 00:00 UTC."
    ),
)
async def checkHabit(
    checkRequest: HabitCheckRequest,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireHabitOwner(currentUser["uid"], checkRequest.habitId)
    try:
        return await dbUtils.checkHabit(checkRequest)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get(
    "/api/habit-entries/{entryId}",
    tags=["habit-entries"],
    summary="Get habit entry by id",
    description="Must own the parent habit for this entry.",
)
async def getHabitEntry(entryId: str, currentUser: dict = Depends(verifyFirebaseToken)):
    await requireEntryOwner(currentUser["uid"], entryId)
    try:
        return await dbUtils.getHabitEntry(entryId)
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.get(
    "/api/habits/{habitId}/entries",
    tags=["habit-entries"],
    summary="List entries for a habit",
    description="Optional **startDate** / **endDate** (UTC calendar dates) filter.",
)
async def getHabitEntries(
    habitId: str,
    startDate: Optional[date] = None,
    endDate: Optional[date] = None,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireHabitOwner(currentUser["uid"], habitId)
    try:
        return await dbUtils.getHabitEntries(habitId, startDate, endDate)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get(
    "/api/users/{userId}/entries/{checkDate}",
    tags=["habit-entries"],
    summary="Entries for all habits on one UTC day",
    description="**userId** must match the token. **checkDate** is a UTC calendar date.",
)
async def getUserEntriesForDate(
    userId: str,
    checkDate: date,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    if userId != currentUser["uid"]:
        raise HTTPException(status_code=403, detail="You can only access your own entries")
    try:
        return await dbUtils.getUserEntriesForDate(userId, checkDate)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.put(
    "/api/habit-entries/{entryId}",
    tags=["habit-entries"],
    summary="Update habit entry",
    description="Sets **completed** / **completedAt**; does not re-validate weekly ISO-week uniqueness (avoid conflicting entries via API discipline).",
)
async def updateHabitEntry(
    entryId: str,
    entryUpdate: HabitEntryUpdate,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireEntryOwner(currentUser["uid"], entryId)
    try:
        return await dbUtils.updateHabitEntry(entryId, entryUpdate)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


# --- Movies (users/{userId}/movies subcollection) ----------------------------

@app.post(
    "/api/movies",
    status_code=status.HTTP_201_CREATED,
    response_model=MediaItemResponse,
    tags=["movies"],
    summary="Add movie to watchlist",
    description="Creates a row in **`users/{userId}/movies`** with `completed: false`.",
)
async def createMovie(
    body: MediaItemCreate,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    try:
        return await dbUtils.createMovie(currentUser["uid"], body)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get(
    "/api/users/{userId}/movies",
    response_model=list[MediaItemResponse],
    tags=["movies"],
    summary="List movies for user",
    description="**userId** must match the Bearer token UID. Newest first.",
)
async def getUserMovies(
    userId: str,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    if userId != currentUser["uid"]:
        raise HTTPException(status_code=403, detail="You can only list your own movies")
    try:
        return await dbUtils.getUserMovies(userId)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get(
    "/api/movies/{movieId}",
    response_model=MediaItemResponse,
    tags=["movies"],
    summary="Get movie by id",
    description="Must own the movie (lives under your `users/{userId}/movies` subcollection).",
)
async def getMovie(movieId: str, currentUser: dict = Depends(verifyFirebaseToken)):
    await requireMovieOwner(currentUser["uid"], movieId)
    try:
        return await dbUtils.getMovie(currentUser["uid"], movieId)
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.put(
    "/api/movies/{movieId}",
    response_model=MediaItemResponse,
    tags=["movies"],
    summary="Update movie title or review",
    description="Partial update: **title**, **rating** (1–5), and/or **review**.",
)
async def updateMovie(
    movieId: str,
    body: MediaItemUpdate,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireMovieOwner(currentUser["uid"], movieId)
    try:
        return await dbUtils.updateMovie(currentUser["uid"], movieId, body)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post(
    "/api/movies/{movieId}/complete",
    response_model=MediaItemResponse,
    tags=["movies"],
    summary="Mark movie watched or unwatched",
    description=(
        "When **completed** is true, **rating** (1–5) and **review** are required. "
        "When false, clears completion, rating, and review."
    ),
)
async def completeMovie(
    movieId: str,
    body: MediaCompleteRequest,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireMovieOwner(currentUser["uid"], movieId)
    try:
        return await dbUtils.completeMovie(currentUser["uid"], movieId, body)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.delete(
    "/api/movies/{movieId}",
    status_code=status.HTTP_204_NO_CONTENT,
    tags=["movies"],
    summary="Delete movie from watchlist",
    description="Permanently removes the document from **`users/{userId}/movies`**.",
)
async def deleteMovie(movieId: str, currentUser: dict = Depends(verifyFirebaseToken)):
    await requireMovieOwner(currentUser["uid"], movieId)
    try:
        await dbUtils.deleteMovie(currentUser["uid"], movieId)
        return None
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


# --- Books (users/{userId}/books subcollection) ------------------------------

@app.post(
    "/api/books",
    status_code=status.HTTP_201_CREATED,
    response_model=MediaItemResponse,
    tags=["books"],
    summary="Add book to read list",
    description="Creates a row in **`users/{userId}/books`** with `completed: false`.",
)
async def createBook(
    body: MediaItemCreate,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    try:
        return await dbUtils.createBook(currentUser["uid"], body)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get(
    "/api/users/{userId}/books",
    response_model=list[MediaItemResponse],
    tags=["books"],
    summary="List books for user",
    description="**userId** must match the Bearer token UID. Newest first.",
)
async def getUserBooks(
    userId: str,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    if userId != currentUser["uid"]:
        raise HTTPException(status_code=403, detail="You can only list your own books")
    try:
        return await dbUtils.getUserBooks(userId)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.get(
    "/api/books/{bookId}",
    response_model=MediaItemResponse,
    tags=["books"],
    summary="Get book by id",
    description="Must own the book (lives under your `users/{userId}/books` subcollection).",
)
async def getBook(bookId: str, currentUser: dict = Depends(verifyFirebaseToken)):
    await requireBookOwner(currentUser["uid"], bookId)
    try:
        return await dbUtils.getBook(currentUser["uid"], bookId)
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))


@app.put(
    "/api/books/{bookId}",
    response_model=MediaItemResponse,
    tags=["books"],
    summary="Update book title or review",
    description="Partial update: **title**, **rating** (1–5), and/or **review**.",
)
async def updateBook(
    bookId: str,
    body: MediaItemUpdate,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireBookOwner(currentUser["uid"], bookId)
    try:
        return await dbUtils.updateBook(currentUser["uid"], bookId, body)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post(
    "/api/books/{bookId}/complete",
    response_model=MediaItemResponse,
    tags=["books"],
    summary="Mark book read or unread",
    description=(
        "When **completed** is true, **rating** (1–5) is required; **review** is optional. "
        "When false, clears completion, rating, and review."
    ),
)
async def completeBook(
    bookId: str,
    body: MediaCompleteRequest,
    currentUser: dict = Depends(verifyFirebaseToken),
):
    await requireBookOwner(currentUser["uid"], bookId)
    try:
        return await dbUtils.completeBook(currentUser["uid"], bookId, body)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.delete(
    "/api/books/{bookId}",
    status_code=status.HTTP_204_NO_CONTENT,
    tags=["books"],
    summary="Delete book from read list",
    description="Permanently removes the document from **`users/{userId}/books`**.",
)
async def deleteBook(bookId: str, currentUser: dict = Depends(verifyFirebaseToken)):
    await requireBookOwner(currentUser["uid"], bookId)
    try:
        await dbUtils.deleteBook(currentUser["uid"], bookId)
        return None
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


if __name__ == "__main__":
    import uvicorn

    from config import DEBUG, HABITTRACKER_PORT

    # Import path required for reload; reload follows DEBUG in config (.env).
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=HABITTRACKER_PORT,
        reload=DEBUG,
    )
