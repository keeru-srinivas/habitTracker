"""Server-side calls to Firebase Identity Toolkit (email/password sign-in)."""
from __future__ import annotations

import httpx

from config import firebaseWebApiKey

signInWithPasswordUrl = (
    "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword"
)


async def signInWithEmailPassword(email: str, password: str) -> dict:
    """
    Returns Firebase REST JSON including idToken, refreshToken, expiresIn, localId.
    Raises ValueError with a human-readable message on failure.
    """
    if not firebaseWebApiKey:
        raise ValueError(
            "Server misconfiguration: set FIREBASE_WEB_API_KEY for login and signup tokens"
        )
    async with httpx.AsyncClient() as httpClient:
        httpResponse = await httpClient.post(
            f"{signInWithPasswordUrl}?key={firebaseWebApiKey}",
            json={
                "email": email.strip(),
                "password": password,
                "returnSecureToken": True,
            },
            timeout=30.0,
        )
    payload = httpResponse.json() if httpResponse.content else {}
    if not httpResponse.is_success:
        errorPayload = payload.get("error") or {}
        message = errorPayload.get("message", httpResponse.text or "Sign-in failed")
        raise ValueError(message)
    return payload
