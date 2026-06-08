# Habit Tracker (API server)

This folder (`backend/`) is the **FastAPI** service: code, `venv`, `.env`, **`deploy.sh`**, **`nginx-habittracker.conf`**, **`LICENSE`**, and **`.gitignore`** for the Python app.

The parent repo also contains **`HabitTracker App/`** (Android client).

FastAPI + Firebase Authentication and Firestore. Clients call **your** HTTP API only: signup/login return `accessToken`; protected routes use `Authorization: Bearer <accessToken>`.

### Documentation & URLs

| Where | URL |
|-------|-----|
| **Swagger UI** | `/docs` |
| **ReDoc** | `/redoc` |
| **OpenAPI JSON** | `/openapi.json` |
| **Production** (after deploy) | `https://habit.thatinsaneguy.com` — same paths (`/docs`, `/api/*`, `/app`) |
| **Local** | `http://localhost:8010` (or your `HABITTRACKER_PORT`) |

In **Swagger**, use **Authorize** and paste `Bearer <accessToken>` from login/signup.

### CORS

The API allows **any origin** (`Access-Control-Allow-Origin: *`). Auth uses **`Authorization: Bearer`**, not cookies, so cross-site browser clients and **native** apps work. Do not rely on `credentials: 'include'` for cross-origin cookie auth.

## Features

- **Auth**: `POST /api/auth/signup` and `POST /api/auth/login` — backend creates users and exchanges credentials for a Firebase ID JWT (`accessToken`). No Firebase SDK on the client.
- **Device telemetry**: auth routes also store a lightweight `deviceEvents` record (IP, user-agent, coarse location lookup) so you can review device sign-ins and map approximate locations.
- **Users & habits**: CRUD habits, check off days, list entries (Bearer required).
- **Web UI (dev)**: `GET /app` uses plain `fetch` only (same flow as a mobile app hitting your REST API).

## Project structure

| File | Role |
|------|------|
| `main.py` | FastAPI app, routes, Jinja templates for `/` and `/app` |
| `util/authIdentity.py` | Server-side Identity Toolkit sign-in (`/api/auth/*`) |
| `util/security.py` | Bearer extraction, `verifyFirebaseToken`, habit/entry ownership |
| `util/dbUtils.py` | Firestore CRUD |
| `util/timeUtils.py` | UTC “today” / RFC3339 timestamps |
| `config.py` | Env loading; `firebaseWebApiKey`, `firebaseAuthDomain`, `firebaseProjectId`, credential paths |
| `firebase_config.py` | Firebase Admin init; Firestore client |
| `models.py` | Pydantic models |
| `templates/landing.html`, `templates/app.html` | Web UI |
| `util/dailyThought.py` | Quote/thought proxy for `GET /api/thought` |
| `deploy.sh` | Production: venv, PM2, nginx, certbot (run from this directory) |
| `nginx-habittracker.conf` | Nginx template (used by `deploy.sh`) |
| `ecosystem.config.cjs` | PM2 process file for uvicorn |
| `LICENSE` | MIT |
| `.gitignore` | Python / env / Firebase key patterns for this app |

## Setup

### Prerequisites

- Python 3.8+
- Firebase project with **Firestore** and **Authentication** (email/password)
- Service account JSON for the Admin SDK

### Install

```bash
pip install -r requirements.txt
```

### Credentials and `.env`

1. Download the service account key from Firebase Console and save it (e.g. `firebase-config.json`). **Do not commit it.** Add `firebase-config.json` to `.gitignore`.

2. Create `.env` (keys can stay uppercase; they map to camelCase variables in `config.py`):

```env
FIREBASE_CREDENTIALS_PATH=firebase-config.json
FIREBASE_DATABASE_URL=https://your-project-id.firebaseio.com

# Web API key — required so the server can mint tokens for POST /api/auth/login and /api/auth/signup
FIREBASE_WEB_API_KEY=your-web-api-key
# Optional if project id is set elsewhere — defaults to <projectId>.firebaseapp.com
FIREBASE_AUTH_DOMAIN=your-project.firebaseapp.com
FIREBASE_PROJECT_ID=your-project-id

DEBUG=True

# HTTP port for dev (`python main.py`) — default 8010; matches nginx + PM2 if unchanged
HABITTRACKER_PORT=8010
```

Get `FIREBASE_WEB_API_KEY` from Firebase Console → Project settings → Your apps → Web app.

### Run the API

```bash
python main.py
```

Listens on **`HABITTRACKER_PORT`** (**8010** by default — same “lane” as production nginx → uvicorn). With **`DEBUG=true`** in `.env`, uvicorn **reloads** when files change. Set **`DEBUG=false`** in production.

Or without going through `main.py`:

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8010
```

- API: `http://localhost:8010`
- Swagger: `http://localhost:8010/docs`
- ReDoc: `http://localhost:8010/redoc`
- Web app: `http://localhost:8010/app`

## Production deployment (nginx + HTTPS)

Run **`deploy.sh` from this directory** (`backend/`). It creates/uses `venv` here, starts **PM2** + **uvicorn** on `127.0.0.1:8010`, and installs **nginx** + **certbot** when executed as root. **`nginx-habittracker.conf`** lives beside the script.

**Public hostname:** `habit.thatinsaneguy.com` (override with `DOMAIN=...`). Point DNS **A** (or **AAAA**) at the server; **80** and **443** must reach nginx.

**Server prerequisites:** Python 3 with `venv`, **nginx**, **Node + PM2** (`npm install -g pm2`), plus firewall allowing HTTP/HTTPS.

```bash
cd /path/to/habitTracker/backend
export CERTBOT_EMAIL='your-email@example.com'   # Let's Encrypt registration
sudo ./deploy.sh
```

Useful overrides: `HABITTRACKER_PORT` (default **8010**, must match `sed` substitution in deploy), `SKIP_SSL=1` (HTTP only), `SKIP_NGINX=1` (app + PM2 only). Optional `CERTBOT_EMAIL` in `.env` in this folder.

After a successful run: **https://habit.thatinsaneguy.com/** (landing + `/app`), **https://habit.thatinsaneguy.com/docs**, all **`/api/*`** routes on the same origin.

## Authentication model

1. **`POST /api/auth/signup`** — body: `email`, `password`, optional `name`. Creates Firebase Auth + Firestore user and returns **`accessToken`**, **`userId`**, **`expiresIn`**, **`tokenType`**.
2. **`POST /api/auth/login`** — body: `email`, `password`. Returns the same token payload for existing users.
3. **Protected routes** — header: `Authorization: Bearer <accessToken>` (Firebase ID JWT verified on the server).
4. **Legacy** `POST /api/users` — creates an account **without** returning a token (use `/api/auth/signup` instead for mobile/web).
5. `POST /api/habits` ignores client `userId`; the UID comes from the token.

## API overview

Public:

- `GET /api/health`
- `GET /api/thought` or `GET /thought` — thought / quote of the day (JSON; add `?format=text` for plain text). Proxies ZenQuotes/Quotable; no API key.
- `GET /api/clock` — server **UTC** calendar date and time (use for UI + `startDate`; checks use this day)
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/users` (legacy; no token in response)

All other `/api/*` routes require a valid **accessToken**. Examples:

- `GET /api/me` — current user profile from Firestore
- `GET /api/me/devices?limit=50` — your recent auth/device events (`eventName`, `ipAddress`, `userAgent`, `location`, `createdAt`)
- `GET /api/users/{userId}` — only if `userId` matches token UID
- `GET /api/users/email/{email}` — only for your own email
- `PUT /api/users/{userId}/name`
- Habits: `POST /api/habits`, `GET /api/habits/{habitId}`, `GET /api/users/{userId}/habits`, `PUT`/`DELETE` habit, `GET /api/habits/{habitId}/progress` (aggregates + heatmap strips — see below)
- Entries: `POST /api/habit-entries`, `POST /api/habits/check`, `GET`/`PUT` entries, date queries

Full detail: **`/docs`** (Swagger), **`/redoc`**, **`/openapi.json`**.

### `GET /api/thought` / `GET /thought`

Public. JSON: `text`, `author`, `source`. Query **`?format=text`** returns `text/plain`.

### `GET /api/habits/{habitId}/progress`

Query params: optional **`startDate`** / **`endDate`** (UTC `YYYY-MM-DD`). Defaults: **endDate** = today UTC, **startDate** = 41 days earlier (42-day inclusive window).

Response highlights:

| Field | Daily habit | Weekly habit |
|--------|-------------|--------------|
| Rollups | Per **UTC day** in range | Per **UTC ISO week** (Mon–Sun) overlapping range |
| **`last14Days`** | 14 consecutive UTC days ending at **endDate** | **`[]`** (empty) |
| **`last14Weeks`** | **`null`** / omitted | 14 ISO weeks ending at the week containing **endDate**; each **`calendarDate`** is that week’s **Monday** |
| Streaks | Consecutive successful **days** | Consecutive successful **weeks** |

Weekly habits: at most one **`completed: true`** per ISO week on **`POST /api/habits/check`** (see Notes).

## Usage examples (curl)

### Sign up (returns accessToken)

```bash
curl -s -X POST "http://localhost:8010/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"securepassword123","name":"Jane"}'
```

### Log in

```bash
curl -s -X POST "http://localhost:8010/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"securepassword123"}'
```

Use `accessToken` from the JSON response as `<ACCESS_TOKEN>` below.

### Current user

```bash
curl "http://localhost:8010/api/me" \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### Create habit (user id comes from token)

```bash
curl -X POST "http://localhost:8010/api/habits" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{"title":"Morning Exercise","frequency":"daily","startDate":"2024-01-01","userId":""}'
```

### Check habit for today

```bash
curl -X POST "http://localhost:8010/api/habits/check" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -d '{"habitId":"habit_id_here","completed":true}'
```

Do **not** send `date`. The server assigns the entry’s calendar day as **today in UTC** (see `GET /api/clock` → `utcCalendarDate`). `completedAt` is always set server-side (RFC 3339 UTC).

### Server UTC clock

```bash
curl -s "http://localhost:8010/api/clock"
```

## Data shapes (camelCase in JSON)

- **User**: `id`, `email`, `name`, `createdAt`
- **Habit**: `id`, `userId`, `title`, `frequency` (`daily` | `weekly`), `startDate`, `isArchived`
- **HabitEntry**: `id`, `habitId`, `date`, `completed`, `completedAt`
- **HabitProgressResponse**: rollups, `last14Days`, optional `last14Weeks` (weekly habits); see schema in `/docs`
- **DailyThoughtResponse** (`/api/thought`): `text`, `author`, `source`

## Notes

- All habit **check-ins** and entry **`date`** buckets use the server’s **UTC calendar day** (`GET /api/clock`). Set **`startDate`** using that same UTC date string so “today” checks succeed.
- **Weekly** habits use **UTC ISO weeks** (Monday through Sunday as UTC calendar dates): at most one **`completed: true`** per week from **`POST /api/habits/check`**; the next window starts the following **Monday 00:00 UTC** (implementation: `utcIsoWeekMonday` / `checkHabit` in `util/dbUtils.py`).
- **`PUT /api/habit-entries/{id}`** does not enforce weekly uniqueness; prefer check-ins through **`POST /api/habits/check`** for correct rules.
- Dates use `YYYY-MM-DD` (interpreted as UTC calendar dates).
