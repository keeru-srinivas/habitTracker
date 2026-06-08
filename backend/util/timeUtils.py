"""UTC helpers — habit calendar logic and API timestamps use UTC for consistency."""
from __future__ import annotations

from datetime import datetime, date, timezone


def utcNow() -> datetime:
    return datetime.now(timezone.utc)


def utcToday() -> date:
    """Calendar date in UTC (used when client omits check date)."""
    return utcNow().date()


def isoUtcNow() -> str:
    """RFC 3339 instant in UTC with Z suffix for stored timestamps."""
    return utcNow().isoformat().replace("+00:00", "Z")
