import datetime as dt

from pydantic import BaseModel, ConfigDict, EmailStr, Field
from typing import List, Optional
from datetime import datetime, date
from enum import Enum

class Frequency(str, Enum):
    DAILY = "daily"
    WEEKLY = "weekly"

class UserCreate(BaseModel):
    email: EmailStr
    password: str
    name: Optional[str] = None


class AuthCredentials(BaseModel):
    """Email + password for POST /api/auth/login."""

    email: EmailStr
    password: str


class AuthTokenResponse(BaseModel):
    """Returned by login and signup; send accessToken as Authorization: Bearer."""

    accessToken: str
    expiresIn: str
    tokenType: str = "Bearer"
    userId: str

class UserResponse(BaseModel):
    id: str
    email: str
    name: Optional[str] = None
    createdAt: datetime
    
    class Config:
        from_attributes = True

class HabitCreate(BaseModel):
    title: str
    frequency: Frequency
    startDate: date
    userId: str

class HabitUpdate(BaseModel):
    title: Optional[str] = None
    frequency: Optional[Frequency] = None
    startDate: Optional[date] = None
    isArchived: Optional[bool] = None

class HabitResponse(BaseModel):
    id: str
    userId: str
    title: str
    frequency: Frequency
    startDate: date
    isArchived: bool
    
    class Config:
        from_attributes = True

class HabitEntryCreate(BaseModel):
    """JSON body still uses `date`; Python field name avoids shadowing datetime.date."""

    model_config = ConfigDict(populate_by_name=True)

    habitId: str
    entryDate: dt.date = Field(alias="date")
    completed: bool = False

class HabitEntryUpdate(BaseModel):
    completed: Optional[bool] = None
    completedAt: Optional[datetime] = None

class HabitEntryResponse(BaseModel):
    id: str
    habitId: str
    date: date
    completed: bool
    completedAt: Optional[datetime] = None
    
    class Config:
        from_attributes = True

class HabitCheckRequest(BaseModel):
    """Check-in uses server UTC only: entry day = UTC calendar date at request time; see GET /api/clock."""

    habitId: str
    completed: bool = Field(
        ...,
        description=(
            "When true, records completion for UTC today. Weekly habits: at most one true per UTC ISO week "
            "(Mon-Sun); conflicts return 400."
        ),
    )


class ServerClockResponse(BaseModel):
    """Authoritative server time for aligning UI lists with POST /api/habits/check (UTC only)."""

    utcCalendarDate: dt.date
    utcDateTime: str
    timezone: str = "UTC"


class DailyThoughtResponse(BaseModel):
    """Thought / quote of the day from `GET /api/thought` (proxied from public APIs)."""

    text: str = Field(..., description="Quote or inspirational line.")
    author: str = Field("", description="Author name when provided by the upstream API.")
    source: str = Field(
        ...,
        description="Where the line came from: e.g. zenquotes.io, quotable.io, or fallback.",
    )


class ProgressDaySnapshot(BaseModel):
    """One heatmap cell (UTC): either a calendar day (daily habit strip) or an ISO week (weekly strip)."""

    calendarDate: dt.date = Field(
        ...,
        description="Daily strip: that UTC day. Weekly strip (last14Weeks): that ISO week’s Monday.",
    )
    status: str = Field(
        ...,
        description=(
            "Heatmap meaning depends on parent list: daily **last14Days** uses "
            "done | missed | skipped | beforeStart | offSchedule | pendingWeek; "
            "weekly **last14Weeks** uses done | missed | skipped | pendingWeek | beforeStart."
        ),
    )


class HabitProgressResponse(BaseModel):
    """Rollups for a habit over [rangeStart, rangeEnd] (UTC). Streaks and rates follow frequency rules in OpenAPI / README."""

    habitId: str
    title: str
    frequency: str
    rangeStart: dt.date
    rangeEnd: dt.date
    habitStartDate: dt.date
    scheduledOpportunities: int = Field(
        ...,
        description="Daily: days in range on/after habit start. Weekly: ISO weeks (Mon-Sun UTC) overlapping that window.",
    )
    doneCount: int
    missedCount: int
    skippedCount: int = Field(
        ...,
        description="Explicit entries with completed=false (no daily row / week rollup marked incomplete where applicable).",
    )
    completionRate: float
    currentStreak: int = Field(
        ...,
        description="Consecutive successful periods ending at range: daily = days, weekly = ISO weeks.",
    )
    bestStreak: int
    last14Days: List[ProgressDaySnapshot] = Field(
        ...,
        description=(
            "Daily habits: 14 UTC days ending at rangeEnd (oldest→newest in API array order). "
            "Weekly habits: empty []; use last14Weeks."
        ),
    )
    last14Weeks: Optional[List[ProgressDaySnapshot]] = Field(
        None,
        description=(
            "Weekly habits only: 14 ISO weeks (Mon–Sun UTC) ending at the week containing rangeEnd; "
            "each calendarDate is that week’s Monday. Omitted or null for daily habits."
        ),
    )
