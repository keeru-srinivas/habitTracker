from firebase_admin import auth
from firebase_config import db
from models import (
    UserCreate,
    HabitCreate,
    HabitUpdate,
    HabitEntryCreate,
    HabitEntryUpdate,
    HabitCheckRequest,
    MediaItemCreate,
    MediaItemUpdate,
    MediaCompleteRequest,
)
from datetime import datetime, date, timedelta, timezone
from typing import List, Optional, Any
import hashlib
import secrets

from util.timeUtils import isoUtcNow, utcNow, utcToday


async def saveDeviceEvent(
    userId: str,
    eventName: str,
    ipAddress: str,
    userAgent: str,
    location: dict | None = None,
) -> dict:
    """Store a lightweight device/login event for audits and device history."""
    try:
        docRef = db.collection("deviceEvents").document()
        eventDoc = {
            "id": docRef.id,
            "userId": userId,
            "eventName": eventName,
            "ipAddress": ipAddress,
            "userAgent": userAgent,
            "location": location or {},
            "createdAt": utcNow(),
        }
        docRef.set(eventDoc)
        return eventDoc
    except Exception as e:
        raise Exception(f"Error saving device event: {str(e)}")


async def getUserDeviceEvents(userId: str, limit: int = 50) -> list[dict]:
    """Fetch recent device/login events for one user, newest first."""
    try:
        docs = (
            db.collection("deviceEvents")
            .where("userId", "==", userId)
            .order_by("createdAt", direction="DESCENDING")
            .limit(limit)
            .stream()
        )
        events = []
        for doc in docs:
            event = doc.to_dict() or {}
            if "id" not in event:
                event["id"] = doc.id
            events.append(event)
        return events
    except Exception as e:
        raise Exception(f"Error getting device events: {str(e)}")


def parseStoredDate(value: Any) -> date:
    """Normalize Firestore/string/datetime values to a calendar date."""
    if value is None:
        raise ValueError("Missing date")
    if isinstance(value, date) and not isinstance(value, datetime):
        return value
    if isinstance(value, datetime):
        return value.date()
    s = str(value)
    if "T" in s:
        s = s.split("T", 1)[0]
    return date.fromisoformat(s)


# Password utilities
def hashPassword(password: str) -> str:
    """Hash password using SHA256"""
    salt = secrets.token_hex(16)
    return hashlib.sha256((password + salt).encode()).hexdigest() + ":" + salt

def verifyPassword(password: str, passwordHash: str) -> bool:
    """Verify password against hash"""
    try:
        hashSegment, salt = passwordHash.split(":")
        return hashlib.sha256((password + salt).encode()).hexdigest() == hashSegment
    except Exception:
        return False

# User functions
async def createUser(userData: UserCreate) -> dict:
    """Create a new user in Firebase Auth and Firestore"""
    try:
        userRecord = auth.create_user(
            email=userData.email,
            password=userData.password
        )
        
        passwordHash = hashPassword(userData.password)
        
        # Get name from userData, default to empty string if not provided
        userName = userData.name if userData.name else ""
        
        userDoc = {
            "id": userRecord.uid,
            "email": userData.email,
            "name": userName,
            "passwordHash": passwordHash,
            "createdAt": utcNow()
        }
        
        db.collection("users").document(userRecord.uid).set(userDoc)
        
        return {
            "id": userRecord.uid,
            "email": userData.email,
            "name": userName,
            "createdAt": userDoc["createdAt"]
        }
    except Exception as e:
        raise Exception(f"Error creating user: {str(e)}")

async def getUser(userId: str) -> dict:
    """Get user by ID"""
    try:
        userDoc = db.collection("users").document(userId).get()
        if not userDoc.exists:
            raise Exception("User not found")
        
        userData = userDoc.to_dict()
        return {
            "id": userData["id"],
            "email": userData["email"],
            "name": userData.get("name", ""),
            "createdAt": userData["createdAt"]
        }
    except Exception as e:
        raise Exception(f"Error getting user: {str(e)}")

async def updateUserName(userId: str, name: str) -> dict:
    """Update user's name"""
    try:
        userRef = db.collection("users").document(userId)
        userDoc = userRef.get()
        
        if not userDoc.exists:
            raise Exception("User not found")
        
        userRef.update({"name": name})
        
        # Return updated user
        updatedData = userRef.get().to_dict()
        return {
            "id": updatedData["id"],
            "email": updatedData["email"],
            "name": updatedData.get("name", ""),
            "createdAt": updatedData["createdAt"]
        }
    except Exception as e:
        raise Exception(f"Error updating user name: {str(e)}")

async def getUserByEmail(email: str) -> dict:
    """Get user by email - checks Firestore first, then Firebase Auth if not found"""
    try:
        # First, try to find in Firestore
        usersRef = db.collection("users")
        query = usersRef.where("email", "==", email).limit(1)
        docs = list(query.stream())
        
        if docs:
            userData = docs[0].to_dict()
            return {
                "id": userData["id"],
                "email": userData["email"],
                "name": userData.get("name", ""),
                "createdAt": userData["createdAt"]
            }
        
        # If not found in Firestore, check Firebase Auth
        try:
            userRecord = auth.get_user_by_email(email)
            # User exists in Auth but not in Firestore - create Firestore document
            userDoc = {
                "id": userRecord.uid,
                "email": userRecord.email,
                "name": userRecord.display_name if userRecord.display_name else "",
                "passwordHash": "",  # Can't retrieve password hash
                "createdAt": utcNow()
            }
            
            db.collection("users").document(userRecord.uid).set(userDoc)
            
            return {
                "id": userRecord.uid,
                "email": userRecord.email,
                "name": userDoc["name"],
                "createdAt": userDoc["createdAt"]
            }
        except Exception:
            # User doesn't exist in Auth either
            raise Exception("User not found")
            
    except Exception as e:
        raise Exception(f"Error getting user: {str(e)}")

# Habit functions
async def createHabit(habitData: HabitCreate) -> dict:
    """Create a new habit"""
    try:
        userDoc = db.collection("users").document(habitData.userId).get()
        if not userDoc.exists:
            raise Exception("User not found")
        
        habitDoc = {
            "id": "",
            "userId": habitData.userId,
            "title": habitData.title,
            "frequency": habitData.frequency.value,
            "startDate": habitData.startDate.isoformat(),
            "isArchived": False
        }
        
        docRef = db.collection("habits").document()
        habitDoc["id"] = docRef.id
        docRef.set(habitDoc)
        
        return habitDoc
    except Exception as e:
        raise Exception(f"Error creating habit: {str(e)}")

async def getHabit(habitId: str) -> dict:
    """Get habit by ID"""
    try:
        habitDoc = db.collection("habits").document(habitId).get()
        if not habitDoc.exists:
            raise Exception("Habit not found")
        
        return habitDoc.to_dict()
    except Exception as e:
        raise Exception(f"Error getting habit: {str(e)}")

async def getUserHabits(userId: str, includeArchived: bool = False) -> List[dict]:
    """Get all habits for a user"""
    try:
        habitsRef = db.collection("habits")
        query = habitsRef.where("userId", "==", userId)
        
        if not includeArchived:
            query = query.where("isArchived", "==", False)
        
        habits = []
        for doc in query.stream():
            habits.append(doc.to_dict())
        
        return habits
    except Exception as e:
        raise Exception(f"Error getting user habits: {str(e)}")

async def updateHabit(habitId: str, habitUpdate: HabitUpdate) -> dict:
    """Update a habit"""
    try:
        habitRef = db.collection("habits").document(habitId)
        habitDoc = habitRef.get()
        
        if not habitDoc.exists:
            raise Exception("Habit not found")
        
        updateData = {}
        if habitUpdate.title is not None:
            updateData["title"] = habitUpdate.title
        if habitUpdate.frequency is not None:
            updateData["frequency"] = habitUpdate.frequency.value
        if habitUpdate.isArchived is not None:
            updateData["isArchived"] = habitUpdate.isArchived
        if habitUpdate.startDate is not None:
            updateData["startDate"] = habitUpdate.startDate.isoformat()
        
        habitRef.update(updateData)
        
        return habitRef.get().to_dict()
    except Exception as e:
        raise Exception(f"Error updating habit: {str(e)}")

async def deleteHabit(habitId: str) -> bool:
    """Delete a habit and all its entries in Firestore."""
    try:
        entriesRef = db.collection("habitEntries").where("habitId", "==", habitId)
        for doc in entriesRef.stream():
            doc.reference.delete()
        db.collection("habits").document(habitId).delete()
        return True
    except Exception as e:
        raise Exception(f"Error deleting habit: {str(e)}")

# Habit Entry functions
async def createHabitEntry(entryData: HabitEntryCreate) -> dict:
    """Create a new habit entry"""
    try:
        habitDoc = db.collection("habits").document(entryData.habitId).get()
        if not habitDoc.exists:
            raise Exception("Habit not found")
        
        entryDoc = {
            "id": "",
            "habitId": entryData.habitId,
            "date": entryData.entryDate.isoformat(),
            "completed": entryData.completed,
            "completedAt": isoUtcNow() if entryData.completed else None
        }
        
        docRef = db.collection("habitEntries").document()
        entryDoc["id"] = docRef.id
        docRef.set(entryDoc)
        
        return entryDoc
    except Exception as e:
        raise Exception(f"Error creating habit entry: {str(e)}")

async def checkHabit(checkRequest: HabitCheckRequest) -> dict:
    """Check a habit for a specific date (daily or weekly check)"""
    try:
        habitDoc = db.collection("habits").document(checkRequest.habitId).get()
        if not habitDoc.exists:
            raise Exception("Habit not found")
        
        habitData = habitDoc.to_dict()
        frequency = habitData.get("frequency", "daily")
        startDate = parseStoredDate(habitData.get("startDate"))
        
        # Single source of truth: UTC calendar date from server clock (see GET /api/clock).
        checkDate = utcToday()

        # Validate date based on frequency (all dates are UTC calendar YYYY-MM-DD).
        if frequency == "weekly":
            daysSinceStart = (checkDate - startDate).days
            if daysSinceStart < 0:
                raise Exception(
                    f"UTC today ({checkDate.isoformat()}) is before habit start ({startDate.isoformat()}). "
                    f"PUT /api/habits/{{id}} with startDate on or before today (UTC), or wait."
                )

            if checkRequest.completed:
                weekMonday = utcIsoWeekMonday(checkDate)
                weekSunday = utcIsoWeekSunday(checkDate)
                week_slice = await getHabitEntries(
                    checkRequest.habitId, weekMonday, weekSunday
                )
                for row in week_slice:
                    row_date = parseStoredDate(row.get("date"))
                    if row_date == checkDate:
                        continue
                    if row.get("completed"):
                        raise Exception(
                            f"This UTC ISO week (Mon {weekMonday.isoformat()} – Sun {weekSunday.isoformat()}) "
                            f"is already completed on {row_date.isoformat()}. Undo there first, or wait until "
                            f"next Monday 00:00 UTC for a new week."
                        )
        else:  # daily
            if checkDate < startDate:
                raise Exception(
                    f"UTC today ({checkDate.isoformat()}) is before habit start ({startDate.isoformat()}). "
                    f"Use PUT /api/habits/{{id}} to set startDate to today or earlier in UTC (GET /api/clock)."
                )
        
        # Check if entry already exists for this date
        entriesRef = db.collection("habitEntries")
        existingEntryQuery = entriesRef.where("habitId", "==", checkRequest.habitId)\
                                 .where("date", "==", checkDate.isoformat())\
                                 .limit(1)
        
        existingEntrySnapshots = list(existingEntryQuery.stream())
        
        if existingEntrySnapshots:
            # Update existing entry
            entryRef = existingEntrySnapshots[0].reference
            updateData = {
                "completed": checkRequest.completed,
                "completedAt": isoUtcNow() if checkRequest.completed else None
            }
            entryRef.update(updateData)
            entryData = entryRef.get().to_dict()
        else:
            # Create new entry
            entryData = await createHabitEntry(HabitEntryCreate(
                habitId=checkRequest.habitId,
                entryDate=checkDate,
                completed=checkRequest.completed
            ))
        
        return entryData
    except Exception as e:
        raise Exception(f"Error checking habit: {str(e)}")

async def getHabitEntry(entryId: str) -> dict:
    """Get habit entry by ID"""
    try:
        entryDoc = db.collection("habitEntries").document(entryId).get()
        if not entryDoc.exists:
            raise Exception("Habit entry not found")
        
        return entryDoc.to_dict()
    except Exception as e:
        raise Exception(f"Error getting habit entry: {str(e)}")

async def getHabitEntries(habitId: str, startDate: Optional[date] = None, endDate: Optional[date] = None) -> List[dict]:
    """Get all entries for a habit, optionally filtered by date range"""
    try:
        entriesRef = db.collection("habitEntries")
        query = entriesRef.where("habitId", "==", habitId)
        
        entries = []
        for doc in query.stream():
            entryData = doc.to_dict()
            entryDate = date.fromisoformat(entryData.get("date"))
            
            if startDate and entryDate < startDate:
                continue
            if endDate and entryDate > endDate:
                continue
            
            entries.append(entryData)
        
        entries.sort(key=lambda row: row.get("date", ""))
        
        return entries
    except Exception as e:
        raise Exception(f"Error getting habit entries: {str(e)}")

async def getUserEntriesForDate(userId: str, checkDate: date) -> List[dict]:
    """Get all habit entries for a user on a specific date"""
    try:
        habitsRef = db.collection("habits")
        habitsQuery = habitsRef.where("userId", "==", userId).where("isArchived", "==", False)
        
        habitIds = [doc.id for doc in habitsQuery.stream()]
        
        if not habitIds:
            return []
        
        entries = []
        entriesRef = db.collection("habitEntries")
        
        for habitId in habitIds:
            query = entriesRef.where("habitId", "==", habitId)\
                             .where("date", "==", checkDate.isoformat())\
                             .limit(1)
            
            for doc in query.stream():
                entries.append(doc.to_dict())
        
        return entries
    except Exception as e:
        raise Exception(f"Error getting user entries for date: {str(e)}")

async def updateHabitEntry(entryId: str, entryUpdate: HabitEntryUpdate) -> dict:
    """Update a habit entry"""
    try:
        entryRef = db.collection("habitEntries").document(entryId)
        entryDoc = entryRef.get()
        
        if not entryDoc.exists:
            raise Exception("Habit entry not found")
        
        updateData = {}
        if entryUpdate.completed is not None:
            updateData["completed"] = entryUpdate.completed
            if entryUpdate.completed:
                updateData["completedAt"] = isoUtcNow()
            else:
                updateData["completedAt"] = None
        
        if entryUpdate.completedAt is not None:
            dt = entryUpdate.completedAt
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=timezone.utc)
            updateData["completedAt"] = dt.astimezone(timezone.utc).isoformat().replace("+00:00", "Z")
        
        entryRef.update(updateData)
        
        return entryRef.get().to_dict()
    except Exception as e:
        raise Exception(f"Error updating habit entry: {str(e)}")


def utcIsoWeekMonday(d: date) -> date:
    """ISO week (UTC): Monday 00:00 … Sunday end-of-day. Python weekday: Mon=0 … Sun=6."""
    return d - timedelta(days=d.weekday())


def utcIsoWeekSunday(d: date) -> date:
    return utcIsoWeekMonday(d) + timedelta(days=6)


def iterUtcIsoWeekMondaysInRange(lo: date, hi: date):
    """Yield each ISO week Monday for weeks that overlap [lo, hi] (inclusive)."""
    wm_start = utcIsoWeekMonday(lo)
    wm_end = utcIsoWeekMonday(hi)
    cur = wm_start
    while cur <= wm_end:
        yield cur
        cur += timedelta(days=7)


def week_has_completed(wm: date, byDate: dict) -> bool:
    for i in range(7):
        k = (wm + timedelta(days=i)).isoformat()
        row = byDate.get(k)
        if row and row.get("completed"):
            return True
    return False


def week_skip_only(wm: date, byDate: dict) -> bool:
    """At least one explicit incomplete entry in the week, and no completed day."""
    any_done = False
    any_incomplete = False
    for i in range(7):
        k = (wm + timedelta(days=i)).isoformat()
        row = byDate.get(k)
        if not row:
            continue
        if row.get("completed"):
            any_done = True
        else:
            any_incomplete = True
    return any_incomplete and not any_done


def week_eligible_for_range(
    wm: date, ws: date, habitStart: date, eff_start: date, eff_end: date
) -> bool:
    overlap_lo = max(wm, habitStart, eff_start)
    overlap_hi = min(ws, eff_end)
    return overlap_lo <= overlap_hi


async def getHabitProgress(habitId: str, rangeStart: date, rangeEnd: date) -> dict:
    """Aggregate done / missed / skipped and streaks for UI (UTC calendar dates)."""
    habit = await getHabit(habitId)
    title = habit.get("title", "")
    freq = habit.get("frequency", "daily")
    habitStart = parseStoredDate(habit.get("startDate"))

    effStart = max(rangeStart, habitStart)
    effEnd = rangeEnd

    fetchLo = min(rangeStart, rangeEnd - timedelta(days=13))
    # Weekly heatmap needs entries for the last 14 ISO weeks (strip ends at rangeEnd's week).
    fetchLo = min(fetchLo, utcIsoWeekMonday(rangeEnd) - timedelta(days=13 * 7))
    fetchLo = max(fetchLo, habitStart)
    entries = await getHabitEntries(habitId, fetchLo, rangeEnd)

    byDate: dict = {}
    for row in entries:
        raw = row.get("date")
        if not raw:
            continue
        key = raw[:10] if isinstance(raw, str) else str(raw)[:10]
        byDate[key] = row

    todayRef = utcToday()

    def classifyDay(d: date) -> str:
        if d < habitStart:
            return "beforeStart"
        if freq == "weekly":
            wm = utcIsoWeekMonday(d)
            ws = wm + timedelta(days=6)
            completed_on: Optional[date] = None
            for i in range(7):
                dd = wm + timedelta(days=i)
                if dd < habitStart:
                    continue
                dk = dd.isoformat()
                row = byDate.get(dk)
                if row and row.get("completed"):
                    completed_on = dd
                    break
            if completed_on is not None:
                return "done" if d == completed_on else "offSchedule"
            if ws < todayRef:
                return "missed"
            if wm <= todayRef <= ws:
                return "pendingWeek"
            return "missed"
        k = d.isoformat()
        if k not in byDate:
            return "missed"
        if byDate[k].get("completed"):
            return "done"
        return "skipped"

    last14Days: List[dict] = []
    last14Weeks: Optional[List[dict]] = None

    if freq == "weekly":

        def classifyWeekStrip(wm: date) -> str:
            ws = wm + timedelta(days=6)
            if ws < habitStart:
                return "beforeStart"
            if week_has_completed(wm, byDate):
                return "done"
            if ws < todayRef:
                return "missed"
            if week_skip_only(wm, byDate):
                return "skipped"
            return "pendingWeek"

        end_wm = utcIsoWeekMonday(rangeEnd)
        last14Weeks = []
        for offset in range(14):
            wm = end_wm - timedelta(days=7 * (13 - offset))
            last14Weeks.append({"calendarDate": wm, "status": classifyWeekStrip(wm)})
    else:
        stripStart = rangeEnd - timedelta(days=13)
        for offset in range(14):
            d = stripStart + timedelta(days=offset)
            last14Days.append({"calendarDate": d, "status": classifyDay(d)})

    if effStart > effEnd:
        return {
            "habitId": habitId,
            "title": title,
            "frequency": freq,
            "rangeStart": rangeStart,
            "rangeEnd": rangeEnd,
            "habitStartDate": habitStart,
            "scheduledOpportunities": 0,
            "doneCount": 0,
            "missedCount": 0,
            "skippedCount": 0,
            "completionRate": 0.0,
            "currentStreak": 0,
            "bestStreak": 0,
            "last14Days": last14Days,
            "last14Weeks": last14Weeks,
        }

    doneCount = 0
    missedCount = 0
    skippedCount = 0
    scheduledOpportunities = 0

    if freq == "weekly":
        for wm in iterUtcIsoWeekMondaysInRange(effStart, effEnd):
            ws = wm + timedelta(days=6)
            if not week_eligible_for_range(wm, ws, habitStart, effStart, effEnd):
                continue
            scheduledOpportunities += 1
            if week_has_completed(wm, byDate):
                doneCount += 1
            elif ws < todayRef:
                missedCount += 1
            elif week_skip_only(wm, byDate):
                skippedCount += 1

        currentStreak = 0
        end_week_mon = utcIsoWeekMonday(min(effEnd, todayRef))
        wm = end_week_mon
        while wm >= utcIsoWeekMonday(effStart):
            ws = wm + timedelta(days=6)
            if not week_eligible_for_range(wm, ws, habitStart, effStart, effEnd):
                break
            if week_has_completed(wm, byDate):
                currentStreak += 1
                wm -= timedelta(days=7)
            else:
                break

        bestStreak = 0
        run = 0
        for wm in iterUtcIsoWeekMondaysInRange(effStart, effEnd):
            ws = wm + timedelta(days=6)
            if not week_eligible_for_range(wm, ws, habitStart, effStart, effEnd):
                run = 0
                continue
            if week_has_completed(wm, byDate):
                run += 1
                bestStreak = max(bestStreak, run)
            else:
                run = 0

    else:
        scheduledOpportunities = (effEnd - effStart).days + 1
        walk = effStart
        while walk <= effEnd:
            k = walk.isoformat()
            if k not in byDate:
                missedCount += 1
            elif byDate[k].get("completed"):
                doneCount += 1
            else:
                skippedCount += 1
            walk += timedelta(days=1)

        currentStreak = 0
        walk = effEnd
        while walk >= effStart:
            k = walk.isoformat()
            if k in byDate and byDate[k].get("completed"):
                currentStreak += 1
                walk -= timedelta(days=1)
            else:
                break

        bestStreak = 0
        run = 0
        walk = effStart
        while walk <= effEnd:
            k = walk.isoformat()
            if k in byDate and byDate[k].get("completed"):
                run += 1
                bestStreak = max(bestStreak, run)
            else:
                run = 0
            walk += timedelta(days=1)

    rate = doneCount / scheduledOpportunities if scheduledOpportunities else 0.0

    return {
        "habitId": habitId,
        "title": title,
        "frequency": freq,
        "rangeStart": rangeStart,
        "rangeEnd": rangeEnd,
        "habitStartDate": habitStart,
        "scheduledOpportunities": scheduledOpportunities,
        "doneCount": doneCount,
        "missedCount": missedCount,
        "skippedCount": skippedCount,
        "completionRate": round(rate, 3),
        "currentStreak": currentStreak,
        "bestStreak": bestStreak,
        "last14Days": last14Days,
        "last14Weeks": last14Weeks,
    }


def _mediaCollection(userId: str, kind: str):
    """Firestore subcollection: users/{userId}/movies or users/{userId}/books."""
    return db.collection("users").document(userId).collection(kind)


def _normalizeMediaDoc(doc) -> dict:
    data = doc.to_dict() or {}
    if "id" not in data:
        data["id"] = doc.id
    return data


async def createMediaItem(userId: str, kind: str, itemData: MediaItemCreate) -> dict:
    """Create a movie or book watch/read list item under the user."""
    try:
        userDoc = db.collection("users").document(userId).get()
        if not userDoc.exists:
            raise Exception("User not found")

        docRef = _mediaCollection(userId, kind).document()
        itemDoc = {
            "id": docRef.id,
            "userId": userId,
            "title": itemData.title.strip(),
            "completed": False,
            "rating": None,
            "review": None,
            "completedAt": None,
            "createdAt": utcNow(),
        }
        docRef.set(itemDoc)
        return itemDoc
    except Exception as e:
        label = "movie" if kind == "movies" else "book"
        raise Exception(f"Error creating {label}: {str(e)}")


async def getMediaItem(userId: str, kind: str, itemId: str) -> dict:
    try:
        doc = _mediaCollection(userId, kind).document(itemId).get()
        if not doc.exists:
            label = "Movie" if kind == "movies" else "Book"
            raise Exception(f"{label} not found")
        return _normalizeMediaDoc(doc)
    except Exception as e:
        raise Exception(f"Error getting media item: {str(e)}")


async def getUserMediaItems(userId: str, kind: str) -> List[dict]:
    try:
        items = []
        for doc in _mediaCollection(userId, kind).stream():
            items.append(_normalizeMediaDoc(doc))
        items.sort(key=lambda row: row.get("createdAt") or "", reverse=True)
        return items
    except Exception as e:
        label = "movies" if kind == "movies" else "books"
        raise Exception(f"Error getting user {label}: {str(e)}")


async def updateMediaItem(
    userId: str, kind: str, itemId: str, itemUpdate: MediaItemUpdate
) -> dict:
    try:
        itemRef = _mediaCollection(userId, kind).document(itemId)
        itemDoc = itemRef.get()
        if not itemDoc.exists:
            label = "Movie" if kind == "movies" else "Book"
            raise Exception(f"{label} not found")

        updateData = {}
        if itemUpdate.title is not None:
            updateData["title"] = itemUpdate.title.strip()
        if itemUpdate.rating is not None:
            updateData["rating"] = itemUpdate.rating
        if itemUpdate.review is not None:
            updateData["review"] = itemUpdate.review

        if updateData:
            itemRef.update(updateData)

        return _normalizeMediaDoc(itemRef.get())
    except Exception as e:
        raise Exception(f"Error updating media item: {str(e)}")


async def completeMediaItem(
    userId: str,
    kind: str,
    itemId: str,
    completeRequest: MediaCompleteRequest,
    *,
    requireReview: bool,
) -> dict:
    try:
        itemRef = _mediaCollection(userId, kind).document(itemId)
        itemDoc = itemRef.get()
        if not itemDoc.exists:
            label = "Movie" if kind == "movies" else "Book"
            raise Exception(f"{label} not found")

        if completeRequest.completed:
            if completeRequest.rating is None:
                raise Exception("rating is required (1-5) when marking as completed")
            if requireReview and not (completeRequest.review or "").strip():
                raise Exception("review is required when marking a movie as watched")
            updateData = {
                "completed": True,
                "rating": completeRequest.rating,
                "review": (completeRequest.review or "").strip(),
                "completedAt": utcNow(),
            }
        else:
            updateData = {
                "completed": False,
                "rating": None,
                "review": None,
                "completedAt": None,
            }

        itemRef.update(updateData)
        return _normalizeMediaDoc(itemRef.get())
    except Exception as e:
        raise Exception(f"Error completing media item: {str(e)}")


async def deleteMediaItem(userId: str, kind: str, itemId: str) -> bool:
    try:
        itemRef = _mediaCollection(userId, kind).document(itemId)
        if not itemRef.get().exists:
            label = "Movie" if kind == "movies" else "Book"
            raise Exception(f"{label} not found")
        itemRef.delete()
        return True
    except Exception as e:
        raise Exception(f"Error deleting media item: {str(e)}")


async def createMovie(userId: str, itemData: MediaItemCreate) -> dict:
    return await createMediaItem(userId, "movies", itemData)


async def getMovie(userId: str, movieId: str) -> dict:
    return await getMediaItem(userId, "movies", movieId)


async def getUserMovies(userId: str) -> List[dict]:
    return await getUserMediaItems(userId, "movies")


async def updateMovie(userId: str, movieId: str, itemUpdate: MediaItemUpdate) -> dict:
    return await updateMediaItem(userId, "movies", movieId, itemUpdate)


async def completeMovie(userId: str, movieId: str, completeRequest: MediaCompleteRequest) -> dict:
    return await completeMediaItem(
        userId, "movies", movieId, completeRequest, requireReview=True
    )


async def deleteMovie(userId: str, movieId: str) -> bool:
    return await deleteMediaItem(userId, "movies", movieId)


async def createBook(userId: str, itemData: MediaItemCreate) -> dict:
    return await createMediaItem(userId, "books", itemData)


async def getBook(userId: str, bookId: str) -> dict:
    return await getMediaItem(userId, "books", bookId)


async def getUserBooks(userId: str) -> List[dict]:
    return await getUserMediaItems(userId, "books")


async def updateBook(userId: str, bookId: str, itemUpdate: MediaItemUpdate) -> dict:
    return await updateMediaItem(userId, "books", bookId, itemUpdate)


async def completeBook(userId: str, bookId: str, completeRequest: MediaCompleteRequest) -> dict:
    return await completeMediaItem(
        userId, "books", bookId, completeRequest, requireReview=False
    )


async def deleteBook(userId: str, bookId: str) -> bool:
    return await deleteMediaItem(userId, "books", bookId)
