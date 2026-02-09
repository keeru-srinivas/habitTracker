# Habit Tracker Backend

A simple Python backend API for tracking habits with Firebase Admin integration. Supports daily and weekly habit tracking with automatic entry management.

## Features

- **User Management**: Create and manage users with Firebase Authentication
- **Habit Management**: Create, update, and archive habits with daily or weekly frequency
- **Habit Checking**: Automatic daily/weekly habit checking with entry creation/updates
- **Entry Tracking**: Track completion status with timestamps for each habit entry

## Project Structure

- `main.py` - FastAPI application with all API endpoints
- `admin.py` - Flask admin panel to view all data
- `dbUtils.py` - All database operations (users, habits, entries)
- `models.py` - Pydantic models for request/response validation
- `firebase_config.py` - Firebase Admin SDK initialization
- `config.py` - Configuration management
- `templates/admin.html` - Admin panel HTML template

## Setup

### Prerequisites

- Python 3.8+
- Firebase project with Firestore enabled
- Firebase Admin SDK credentials JSON file

### Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Set up Firebase credentials:
   - Download your Firebase service account key JSON file from Firebase Console
   - Place it in the project root as `firebase-config.json`
   - Or set the path in `.env` file

3. Create a `.env` file in the project root (optional):
```env
FIREBASE_CREDENTIALS_PATH=firebase-config.json
FIREBASE_DATABASE_URL=https://your-project-id.firebaseio.com
DEBUG=True
```

### Running the Server

**FastAPI Backend:**
```bash
python main.py
```

Or using uvicorn directly:
```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at `http://localhost:8000`

API documentation available at:
- Swagger UI: `http://localhost:8000/docs`
- ReDoc: `http://localhost:8000/redoc`

**Flask Admin Panel:**
```bash
python admin.py
```

The admin panel will be available at `http://localhost:5000`

The admin panel displays:
- Total users with their data
- All habits with details
- All habit entries
- Statistics dashboard
- All data in one tabbed interface

## API Endpoints

### Users

- `POST /api/users` - Create a new user
- `GET /api/users/{userId}` - Get user by ID

### Habits

- `POST /api/habits` - Create a new habit
- `GET /api/habits/{habitId}` - Get habit by ID
- `GET /api/users/{userId}/habits` - Get all habits for a user
- `PUT /api/habits/{habitId}` - Update a habit
- `DELETE /api/habits/{habitId}` - Delete a habit

### Habit Entries

- `POST /api/habit-entries` - Create a habit entry manually
- `POST /api/habits/check` - Check a habit (creates/updates entry for date)
- `GET /api/habit-entries/{entryId}` - Get habit entry by ID
- `GET /api/habits/{habitId}/entries` - Get all entries for a habit
- `GET /api/users/{userId}/entries/{date}` - Get user's entries for a date
- `PUT /api/habit-entries/{entryId}` - Update a habit entry

## Data Models

### User
- `id`: string (Firebase Auth UID)
- `email`: string
- `passwordHash`: string
- `createdAt`: datetime

### Habit
- `id`: string
- `userId`: string
- `title`: string
- `frequency`: "daily" | "weekly"
- `startDate`: date (YYYY-MM-DD)
- `isArchived`: boolean

### HabitEntry
- `id`: string
- `habitId`: string
- `date`: date (YYYY-MM-DD)
- `completed`: boolean
- `completedAt`: datetime (optional)

## Usage Examples

### Create a User
```bash
curl -X POST "http://localhost:8000/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securepassword123"
  }'
```

### Create a Daily Habit
```bash
curl -X POST "http://localhost:8000/api/habits" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Morning Exercise",
    "frequency": "daily",
    "startDate": "2024-01-01",
    "userId": "user_id_here"
  }'
```

### Create a Weekly Habit
```bash
curl -X POST "http://localhost:8000/api/habits" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Weekly Review",
    "frequency": "weekly",
    "startDate": "2024-01-01",
    "userId": "user_id_here"
  }'
```

### Check a Habit (Daily/Weekly)
```bash
curl -X POST "http://localhost:8000/api/habits/check" \
  -H "Content-Type: application/json" \
  -d '{
    "habitId": "habit_id_here",
    "date": "2024-01-15",
    "completed": true
  }'
```

If `date` is not provided, it defaults to today's date.

## Notes

- Weekly habits can only be checked on the same day of the week as the start date
- Daily habits can be checked any day from the start date onwards
- The system automatically creates or updates entries when checking habits
- All dates should be in YYYY-MM-DD format
- All field names use camelCase convention