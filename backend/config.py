import os
from dotenv import load_dotenv

load_dotenv()

# Default to firebase-config.json
default_path = "firebase-config.json"
FIREBASE_CREDENTIALS_PATH = os.getenv("FIREBASE_CREDENTIALS_PATH", default_path)
FIREBASE_DATABASE_URL = os.getenv("FIREBASE_DATABASE_URL", "")
DEBUG = os.getenv("DEBUG", "False").lower() == "true"
