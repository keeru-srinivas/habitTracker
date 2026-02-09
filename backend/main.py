from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from models import (
    UserCreate,
    HabitCreate, HabitUpdate,
    HabitEntryCreate, HabitEntryUpdate,
    HabitCheckRequest
)
import dbUtils
from datetime import date
from typing import List

app = FastAPI(title="Habit Tracker API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

from firebase_config import db, authClient

@app.get("/")
async def root():
    return {"message": "Habit Tracker API", "status": "running"}

# User endpoints
@app.post("/api/users", status_code=status.HTTP_201_CREATED)
async def createUser(userData: UserCreate):
    try:
        user = await dbUtils.createUser(userData)
        return user
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/api/users/{userId}")
async def getUser(userId: str):
    try:
        user = await dbUtils.getUser(userId)
        return user
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@app.get("/api/users/email/{email}")
async def getUserByEmail(email: str):
    try:
        # URL decode email in case it's encoded
        from urllib.parse import unquote
        email = unquote(email)
        user = await dbUtils.getUserByEmail(email)
        return user
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@app.put("/api/users/{userId}/name")
async def updateUserName(userId: str, name: dict):
    """Update user's name"""
    try:
        userName = name.get("name", "")
        user = await dbUtils.updateUserName(userId, userName)
        return user
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Habit endpoints
@app.post("/api/habits", status_code=status.HTTP_201_CREATED)
async def createHabit(habitData: HabitCreate):
    try:
        habit = await dbUtils.createHabit(habitData)
        return habit
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/api/habits/{habitId}")
async def getHabit(habitId: str):
    try:
        habit = await dbUtils.getHabit(habitId)
        return habit
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@app.get("/api/users/{userId}/habits")
async def getUserHabits(userId: str, includeArchived: bool = False):
    try:
        habits = await dbUtils.getUserHabits(userId, includeArchived)
        return habits
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.put("/api/habits/{habitId}")
async def updateHabit(habitId: str, habitUpdate: HabitUpdate):
    try:
        habit = await dbUtils.updateHabit(habitId, habitUpdate)
        return habit
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.delete("/api/habits/{habitId}", status_code=status.HTTP_204_NO_CONTENT)
async def deleteHabit(habitId: str):
    try:
        await dbUtils.deleteHabit(habitId)
        return None
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

# Habit Entry endpoints
@app.post("/api/habit-entries", status_code=status.HTTP_201_CREATED)
async def createHabitEntry(entryData: HabitEntryCreate):
    try:
        entry = await dbUtils.createHabitEntry(entryData)
        return entry
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/api/habits/check")
async def checkHabit(checkRequest: HabitCheckRequest):
    try:
        entry = await dbUtils.checkHabit(checkRequest)
        return entry
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/api/habit-entries/{entryId}")
async def getHabitEntry(entryId: str):
    try:
        entry = await dbUtils.getHabitEntry(entryId)
        return entry
    except Exception as e:
        raise HTTPException(status_code=404, detail=str(e))

@app.get("/api/habits/{habitId}/entries")
async def getHabitEntries(habitId: str, startDate: date = None, endDate: date = None):
    try:
        entries = await dbUtils.getHabitEntries(habitId, startDate, endDate)
        return entries
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/api/users/{userId}/entries/{checkDate}")
async def getUserEntriesForDate(userId: str, checkDate: date):
    try:
        entries = await dbUtils.getUserEntriesForDate(userId, checkDate)
        return entries
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.put("/api/habit-entries/{entryId}")
async def updateHabitEntry(entryId: str, entryUpdate: HabitEntryUpdate):
    try:
        entry = await dbUtils.updateHabitEntry(entryId, entryUpdate)
        return entry
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
