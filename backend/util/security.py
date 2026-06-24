"""Firebase ID token verification and resource ownership checks."""
from __future__ import annotations

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from firebase_admin import auth as firebaseAuth

import util.dbUtils as dbUtils

securityBearer = HTTPBearer(
    scheme_name="BearerAuth",
    description="Paste accessToken from POST /api/auth/login or /api/auth/signup",
)


def verifyFirebaseToken(
    credentials: HTTPAuthorizationCredentials = Depends(securityBearer),
) -> dict:
    """FastAPI dependency: validates Firebase ID JWT from Authorization: Bearer."""
    token = credentials.credentials
    try:
        decodedToken = firebaseAuth.verify_id_token(token)
        return {
            "uid": decodedToken["uid"],
            "email": decodedToken.get("email"),
        }
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired access token",
        )


async def requireHabitOwner(uid: str, habitId: str) -> dict:
    habitRow = await dbUtils.getHabit(habitId)
    if habitRow.get("userId") != uid:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not your habit")
    return habitRow


async def requireEntryOwner(uid: str, entryId: str) -> dict:
    entryRow = await dbUtils.getHabitEntry(entryId)
    await requireHabitOwner(uid, entryRow["habitId"])
    return entryRow


async def requireMovieOwner(uid: str, movieId: str) -> dict:
    movieRow = await dbUtils.getMovie(uid, movieId)
    if movieRow.get("userId") != uid:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not your movie")
    return movieRow


async def requireBookOwner(uid: str, bookId: str) -> dict:
    bookRow = await dbUtils.getBook(uid, bookId)
    if bookRow.get("userId") != uid:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not your book")
    return bookRow
