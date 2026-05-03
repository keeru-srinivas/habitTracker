import json
import os
from dotenv import load_dotenv

load_dotenv()

# --- HTTP port --------------------------------------------------------------
# Default **8010** — habit-tracker lane (clear of 8000 clutter); pairs with nginx
# `proxy_pass` + PM2 `ecosystem.config.cjs`. Override: `HABITTRACKER_PORT=9001`
HABITTRACKER_PORT = int(os.getenv("HABITTRACKER_PORT", "8010"))

# Default to firebase-config.json (env key stays FIREBASE_CREDENTIALS_PATH)
defaultPath = "firebase-config.json"
firebaseCredentialsPath = os.getenv("FIREBASE_CREDENTIALS_PATH", defaultPath)
firebaseDatabaseUrl = os.getenv("FIREBASE_DATABASE_URL", "")
# Default True so `python main.py` runs uvicorn with reload; set DEBUG=false in production.
DEBUG = os.getenv("DEBUG", "true").lower() == "true"


def loadProjectIdFromCredentials() -> str:
    path = firebaseCredentialsPath
    if not path or not os.path.exists(path):
        return ""
    try:
        with open(path, encoding="utf-8") as f:
            return (json.load(f).get("project_id") or "").strip()
    except Exception:
        return ""


# Web client (Firebase JS SDK) — API key from Firebase Console → Project settings → Your apps
firebaseProjectId = (os.getenv("FIREBASE_PROJECT_ID") or loadProjectIdFromCredentials() or "").strip()
firebaseWebApiKey = (os.getenv("FIREBASE_WEB_API_KEY") or "").strip()
firebaseAuthDomain = (
    (os.getenv("FIREBASE_AUTH_DOMAIN") or "").strip()
    or (f"{firebaseProjectId}.firebaseapp.com" if firebaseProjectId else "")
)
