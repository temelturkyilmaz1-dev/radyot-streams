#!/usr/bin/env python3
import concurrent.futures
import datetime as dt
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

CATALOG = Path("stations.json")
OUTPUT = Path("health.json")
TIMEOUT_SECONDS = 12
READ_BYTES = 16_384
USER_AGENT = "RadyoT-HealthCheck/1.0"


def utc_now():
    return dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def request(url, read_bytes=READ_BYTES):
    headers = {
        "User-Agent": USER_AGENT,
        "Accept": "*/*",
        "Icy-MetaData": "1",
        "Connection": "close",
    }
    req = urllib.request.Request(url, headers=headers, method="GET")
    started = time.monotonic()
    with urllib.request.urlopen(req, timeout=TIMEOUT_SECONDS) as response:
        body = response.read(read_bytes)
        return response.status, response.geturl(), dict(response.headers), body, int((time.monotonic() - started) * 1000)


def check_url(url):
    result = {"url": url, "ok": False, "checkedAt": utc_now()}
    try:
        status, final_url, headers, body, elapsed = request(url)
        content_type = headers.get("Content-Type", "").split(";", 1)[0].strip().lower()
        playlist = url.lower().split("?", 1)[0].endswith((".m3u8", ".m3u")) or b"#EXTM3U" in body[:1024]
        if not body:
            raise ValueError("Sunucu boş yanıt verdi")

        detail = "stream-bytes"
        if playlist:
            text = body.decode("utf-8", errors="replace")
            if "#EXTM3U" not in text:
                raise ValueError("Geçerli playlist başlığı bulunamadı")
            candidates = [line.strip() for line in text.splitlines() if line.strip() and not line.lstrip().startswith("#")]
            if candidates:
                media_url = urllib.parse.urljoin(final_url, candidates[0])
                child_status, _, _, child_body, _ = request(media_url, 2048)
                if child_status >= 400 or not child_body:
                    raise ValueError("Playlist açıldı ancak ilk medya öğesi alınamadı")
                detail = "playlist-and-media"

        result.update(
            ok=200 <= status < 400,
            httpStatus=status,
            finalUrl=final_url,
            responseMs=elapsed,
            contentType=content_type,
            validation=detail,
        )
    except urllib.error.HTTPError as exc:
        result.update(httpStatus=exc.code, error=f"HTTP {exc.code}")
    except Exception as exc:
        result["error"] = f"{type(exc).__name__}: {exc}"[:300]
    return result


def main():
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    previous = {}
    if OUTPUT.exists():
        try:
            previous = json.loads(OUTPUT.read_text(encoding="utf-8")).get("stations", {})
        except (OSError, ValueError):
            pass

    jobs = []
    for station in catalog["stations"]:
        if station.get("enabled", True):
            for index, url in enumerate(station.get("urls", [])):
                jobs.append((station, index, url))

    checked = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=12) as pool:
        futures = {pool.submit(check_url, url): (station, index) for station, index, url in jobs}
        for future in concurrent.futures.as_completed(futures):
            station, index = futures[future]
            checked.setdefault(station["id"], {})[index] = future.result()

    station_health = {}
    online = 0
    for station in catalog["stations"]:
        results_by_index = checked.get(station["id"], {})
        results = [results_by_index[index] for index in sorted(results_by_index)]
        active_index = next((index for index, item in enumerate(results) if item["ok"]), None)
        is_online = active_index is not None
        previous_failures = int(previous.get(station["id"], {}).get("consecutiveFailures", 0))
        failures = 0 if is_online else previous_failures + 1
        if is_online:
            online += 1
        station_health[station["id"]] = {
            "name": station["name"],
            "status": "online" if is_online else ("offline" if failures >= 3 else "warning"),
            "activeUrlIndex": active_index,
            "consecutiveFailures": failures,
            "urls": results,
        }

    total = len(station_health)
    output = {
        "schemaVersion": 1,
        "generatedAt": utc_now(),
        "summary": {"total": total, "online": online, "unavailable": total - online},
        "stations": station_health,
    }
    OUTPUT.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
