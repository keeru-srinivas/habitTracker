from pydantic import BaseModel, EmailStr
from typing import Optional
from datetime import datetime, date
from enum import Enum

class Frequency(str, Enum):
    DAILY = "daily"
    WEEKLY = "weekly"

class UserCreate(BaseModel):
    email: EmailStr
    password: str
    name: Optional[str] = None

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
    habitId: str
    date: date
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
    habitId: str
    date: Optional[date] = None  # If not provided, uses today
    completed: bool
