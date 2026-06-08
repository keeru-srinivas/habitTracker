from __future__ import annotations

import ipaddress
from typing import Optional

import httpx
from fastapi import Request


def _firstHeaderValue(raw: str | None) -> str:
    if not raw:
        return ""
    return raw.split(",", 1)[0].strip()


def extractClientIp(request: Request) -> str:
    """
    Resolve the client IP, preferring proxy-forwarded headers.
    """
    candidates = [
        _firstHeaderValue(request.headers.get("cf-connecting-ip")),
        _firstHeaderValue(request.headers.get("x-forwarded-for")),
        _firstHeaderValue(request.headers.get("x-real-ip")),
    ]
    if request.client and request.client.host:
        candidates.append(request.client.host.strip())

    for value in candidates:
        if value:
            return value
    return "unknown"


def _isPublicIp(ipAddress: str) -> bool:
    try:
        ip = ipaddress.ip_address(ipAddress)
        return not (ip.is_private or ip.is_loopback or ip.is_link_local)
    except ValueError:
        return False


async def lookupIpLocation(ipAddress: str) -> dict:
    """
    Lookup coarse geolocation for a public IP address.
    Returns an empty dict for unknown/private addresses or lookup failures.
    """
    if not _isPublicIp(ipAddress):
        return {}

    url = f"https://ipapi.co/{ipAddress}/json/"
    try:
        async with httpx.AsyncClient(timeout=4.0) as client:
            response = await client.get(url)
        if not response.is_success:
            return {}
        payload = response.json()
        if payload.get("error"):
            return {}
        return {
            "city": payload.get("city") or "",
            "region": payload.get("region") or "",
            "country": payload.get("country_name") or "",
            "countryCode": payload.get("country_code") or "",
            "latitude": payload.get("latitude"),
            "longitude": payload.get("longitude"),
        }
    except Exception:
        return {}


def readUserAgent(request: Request) -> str:
    return (request.headers.get("user-agent") or "").strip()
