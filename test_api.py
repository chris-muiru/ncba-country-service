#!/usr/bin/env python3
"""
NCBA Country Info Service - API Test Script
Tests all REST endpoints and the full SOAP integration flow.

Usage:
    python3 test_api.py
    python3 test_api.py --base-url http://localhost:8080
"""

import json
import sys
import time
import argparse
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8080"


def color(text, code):
    return f"\033[{code}m{text}\033[0m"


def green(t): return color(t, "92")
def red(t):   return color(t, "91")
def cyan(t):  return color(t, "96")
def yellow(t):return color(t, "93")
def bold(t):  return color(t, "1")


results = []


def request(method, path, body=None, expected_status=None):
    url = BASE_URL + path
    data = json.dumps(body).encode() if body else None
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            status = resp.status
            raw = resp.read().decode()
            body_out = json.loads(raw) if raw else {}
            return status, body_out, None
    except urllib.error.HTTPError as e:
        raw = e.read().decode()
        body_out = json.loads(raw) if raw else {}
        return e.code, body_out, None
    except Exception as ex:
        return None, None, str(ex)


def run_test(name, method, path, body=None, expected_status=200, check=None):
    print(f"  {cyan('→')} {name}")
    status, resp, err = request(method, path, body, expected_status)

    if err:
        print(f"    {red('FAIL')} — connection error: {err}")
        results.append((name, False, f"connection error: {err}"))
        return None

    status_ok = status == expected_status
    check_ok = True
    check_msg = ""

    if check and status_ok:
        try:
            check(resp)
        except AssertionError as e:
            check_ok = False
            check_msg = str(e)

    passed = status_ok and check_ok

    status_display = green(str(status)) if status_ok else red(str(status))
    result_display = green("PASS") if passed else red("FAIL")

    detail = ""
    if not status_ok:
        detail = f" (expected {expected_status})"
    if not check_ok:
        detail += f" — assertion: {check_msg}"

    print(f"    {result_display} — HTTP {status_display}{detail}")

    if resp and not passed:
        print(f"    Response: {json.dumps(resp, indent=6)[:300]}")

    results.append((name, passed, detail))
    return resp


def section(title):
    print(f"\n{bold('━━━ ' + title + ' ━━━')}")


def main():
    global BASE_URL
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://localhost:8080")
    args = parser.parse_args()
    BASE_URL = args.base_url.rstrip("/")

    print(bold(f"\nNCBA Country Info Service — API Tests"))
    print(f"Target: {cyan(BASE_URL)}\n")

    # ─── Health Check ──────────────────────────────────────────────────────────
    section("Health & Actuator")

    run_test(
        "Health endpoint is UP",
        "GET", "/actuator/health",
        expected_status=200,
        check=lambda r: (
            r.get("status") == "UP" or
            (_ for _ in ()).throw(AssertionError(f"status={r.get('status')}"))
        )
    )

    run_test("Metrics endpoint accessible", "GET", "/actuator/metrics", expected_status=200)

    # ─── Input Validation ──────────────────────────────────────────────────────
    section("Input Validation")

    run_test(
        "Empty name returns 400",
        "POST", "/api/country",
        body={"name": ""},
        expected_status=400
    )

    run_test(
        "Missing name field returns 400",
        "POST", "/api/country",
        body={},
        expected_status=400
    )

    run_test(
        "Whitespace-only name returns 400",
        "POST", "/api/country",
        body={"name": "   "},
        expected_status=400
    )

    # ─── SOAP Integration + Save ───────────────────────────────────────────────
    section("SOAP Integration — Fetch & Save Country")

    print(f"  {yellow('Note: these calls hit the live SOAP service — may take a few seconds')}")

    kenya = run_test(
        "POST Kenya (lowercase) → sentence case → SOAP → save",
        "POST", "/api/country",
        body={"name": "kenya"},
        expected_status=201,
        check=lambda r: (
            assert_field(r, "name", "Kenya"),
            assert_field_nonempty(r, "capitalCity"),
            assert_field_nonempty(r, "isoCode"),
            assert_field_nonempty(r, "phoneCode"),
            assert_list_nonempty(r, "languages"),
        )
    )

    tanzania = run_test(
        "POST Tanzania (UPPERCASE) → sentence case → SOAP → save",
        "POST", "/api/country",
        body={"name": "TANZANIA"},
        expected_status=201,
        check=lambda r: (
            assert_field(r, "name", "Tanzania"),
            assert_field_nonempty(r, "capitalCity"),
        )
    )

    uganda = run_test(
        "POST Uganda → sentence case → SOAP → save",
        "POST", "/api/country",
        body={"name": "Uganda"},
        expected_status=201,
        check=lambda r: assert_field_nonempty(r, "id")
    )

    # ─── Read Operations ───────────────────────────────────────────────────────
    section("CRUD — Read")

    all_countries = run_test(
        "GET all countries — returns list",
        "GET", "/api/country",
        expected_status=200,
        check=lambda r: (
            isinstance(r, list) or
            (_ for _ in ()).throw(AssertionError(f"expected list, got {type(r).__name__}"))
        )
    )

    if all_countries and len(all_countries) > 0:
        count = len(all_countries)
        print(f"    {yellow(f'ℹ {count} countries in database')}")

    kenya_id = kenya.get("id") if kenya else None
    if kenya_id:
        run_test(
            f"GET /api/country/{kenya_id} — fetch Kenya by ID",
            "GET", f"/api/country/{kenya_id}",
            expected_status=200,
            check=lambda r: assert_field(r, "name", "Kenya")
        )

    run_test(
        "GET /api/country/99999 — not found returns 400",
        "GET", "/api/country/99999",
        expected_status=400
    )

    # ─── Update ────────────────────────────────────────────────────────────────
    section("CRUD — Update")

    if kenya_id and kenya:
        updated_body = {
            "name": "Kenya",
            "capitalCity": "Nairobi (Updated)",
            "phoneCode": kenya.get("phoneCode"),
            "continentCode": kenya.get("continentCode"),
            "currencyIsoCode": kenya.get("currencyIsoCode"),
            "countryFlag": kenya.get("countryFlag"),
            "isoCode": kenya.get("isoCode"),
            "languages": kenya.get("languages", [])
        }
        run_test(
            f"PUT /api/country/{kenya_id} — update capitalCity",
            "PUT", f"/api/country/{kenya_id}",
            body=updated_body,
            expected_status=200,
            check=lambda r: assert_field(r, "capitalCity", "Nairobi (Updated)")
        )
    else:
        print(f"  {yellow('SKIP')} — no Kenya ID available (POST may have failed)")

    # ─── Delete ────────────────────────────────────────────────────────────────
    section("CRUD — Delete")

    uganda_id = uganda.get("id") if uganda else None
    if uganda_id:
        run_test(
            f"DELETE /api/country/{uganda_id} — returns 204",
            "DELETE", f"/api/country/{uganda_id}",
            expected_status=204
        )
        run_test(
            f"GET /api/country/{uganda_id} after delete — returns 400",
            "GET", f"/api/country/{uganda_id}",
            expected_status=400
        )
    else:
        print(f"  {yellow('SKIP')} — no Uganda ID available")

    # ─── Summary ───────────────────────────────────────────────────────────────
    print(f"\n{bold('━━━ Results ━━━')}")
    passed = sum(1 for _, ok, _ in results if ok)
    failed = sum(1 for _, ok, _ in results if not ok)
    total = len(results)

    for name, ok, detail in results:
        icon = green("✓") if ok else red("✗")
        print(f"  {icon} {name}")

    print(f"\n  {bold(green(str(passed)) + '/' + str(total) + ' passed')}", end="")
    if failed:
        print(f"  {red(str(failed) + ' failed')}", end="")
    print()

    if failed:
        sys.exit(1)


# ─── Assertion helpers ──────────────────────────────────────────────────────

def assert_field(obj, field, expected):
    actual = obj.get(field)
    if actual != expected:
        raise AssertionError(f"{field}: expected '{expected}', got '{actual}'")


def assert_field_nonempty(obj, field):
    val = obj.get(field)
    if not val:
        raise AssertionError(f"{field} is empty or missing")


def assert_list_nonempty(obj, field):
    val = obj.get(field)
    if not isinstance(val, list) or len(val) == 0:
        raise AssertionError(f"{field} should be a non-empty list")


if __name__ == "__main__":
    main()
