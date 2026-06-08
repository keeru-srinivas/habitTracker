"""Fetch thought-of-the-day from a public quotes API (ZenQuotes)."""

import logging
from typing import Tuple

import httpx

log = logging.getLogger(__name__)

ZEN_TODAY_URL = "https://zenquotes.io/api/today"
QUOTABLE_RANDOM = "https://api.quotable.io/random?maxLength=240"
USER_AGENT = "HabitTrackerBackend/1.0 (github; +https://github.com)"


async def fetch_daily_thought() -> Tuple[str, str, str]:
    """
    Returns (text, author, source_label).
    Tries ZenQuotes 'today' first, then Quotable random; on failure uses a local fallback.
    """
    async with httpx.AsyncClient(
        timeout=httpx.Timeout(12.0),
        headers={"User-Agent": USER_AGENT},
        follow_redirects=True,
    ) as client:
        try:
            r = await client.get(ZEN_TODAY_URL)
            r.raise_for_status()
            data = r.json()
            if isinstance(data, list) and data:
                row = data[0]
                q = str(row.get("q", "")).strip().strip('"')
                a = str(row.get("a", "")).strip()
                if q:
                    return q, a, "zenquotes.io"
        except Exception as e:
            log.warning("ZenQuotes today failed: %s", e)

        try:
            r = await client.get(QUOTABLE_RANDOM)
            r.raise_for_status()
            row = r.json()
            if isinstance(row, dict):
                q = str(row.get("content", "")).strip()
                a = str(row.get("author", "")).strip()
                if q:
                    return q, a, "quotable.io"
        except Exception as e:
            log.warning("Quotable fallback failed: %s", e)

    return (
        "Small steps each day add up. You’ve got this.",
        "",
        "fallback",
    )
