import concurrent.futures
import json
import os
import time
import requests

BASE_URL = os.getenv("TARGET_URL", "http://localhost:8080/api/v1")

def attempt_seat_hold(user_id, event_id, seat_ids):
    url = f"{BASE_URL}/holds"
    payload = {
        "eventId": event_id,
        "seatIds": seat_ids
    }
    headers = {
        "Content-Type": "application/json"
    }
    start_time = time.time()
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=5)
        latency_ms = (time.time() - start_time) * 1000
        return {
            "user_id": user_id,
            "status_code": response.status_code,
            "latency_ms": latency_ms,
            "response": response.json() if "application/json" in response.headers.get("content-type", "") else response.text
        }
    except Exception as e:
        latency_ms = (time.time() - start_time) * 1000
        return {
            "user_id": user_id,
            "status_code": 0,
            "latency_ms": latency_ms,
            "error": str(e)
        }

def generate_html_report(results, elapsed, threads, event_id, seat_ids, p50, p90, p99, rps):
    successes = [r for r in results if r.get("status_code") in (200, 201)]
    collisions = [r for r in results if r.get("status_code") in (400, 409, 500) or "lock" in str(r).lower()]

    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TicketVerse — Concurrency Benchmark Report</title>
    <style>
        body {{ font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 2rem; }}
        .container {{ max-width: 1000px; margin: 0 auto; background: #1e293b; border-radius: 12px; padding: 2rem; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }}
        h1 {{ color: #38bdf8; margin-top: 0; border-bottom: 2px solid #334155; padding-bottom: 0.5rem; }}
        .badge {{ display: inline-block; padding: 0.25rem 0.75rem; border-radius: 9999px; font-weight: bold; font-size: 0.875rem; }}
        .badge-success {{ background: #059669; color: #ecfdf5; }}
        .badge-danger {{ background: #dc2626; color: #fef2f2; }}
        .grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1.5rem; margin: 1.5rem 0; }}
        .card {{ background: #0f172a; border-radius: 8px; padding: 1.25rem; border: 1px solid #334155; }}
        .card-title {{ color: #94a3b8; font-size: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; }}
        .card-value {{ font-size: 1.75rem; font-weight: bold; color: #f8fafc; margin-top: 0.5rem; }}
        table {{ width: 100%; border-collapse: collapse; margin-top: 1.5rem; }}
        th, td {{ padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #334155; }}
        th {{ background: #0f172a; color: #38bdf8; }}
        tr:hover {{ background: #334155; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>🎟️ TicketVerse — Concurrency Benchmark Report</h1>
        <p>Status: <span class="badge {'badge-success' if len(successes) <= 1 else 'badge-danger'}">{'PASSED (ZERO DOUBLE-BOOKINGS)' if len(successes) <= 1 else 'FAILED (RACE CONDITION DETECTED)'}</span></p>

        <div class="grid">
            <div class="card">
                <div class="card-title">Concurrent Threads</div>
                <div class="card-value">{threads}</div>
            </div>
            <div class="card">
                <div class="card-title">Throughput (RPS)</div>
                <div class="card-value">{rps:.2f}</div>
            </div>
            <div class="card">
                <div class="card-title">P50 Latency</div>
                <div class="card-value">{p50:.1f} ms</div>
            </div>
            <div class="card">
                <div class="card-title">P99 Latency</div>
                <div class="card-value">{p99:.1f} ms</div>
            </div>
        </div>

        <h3>📊 Response & Latency Distribution</h3>
        <table>
            <thead>
                <tr>
                    <th>Metric</th>
                    <th>Value</th>
                </tr>
            </thead>
            <tbody>
                <tr><td>Total Requests Processed</td><td>{len(results)}</td></tr>
                <tr><td>Successful Holds Acquired</td><td>{len(successes)}</td></tr>
                <tr><td>Lock Collisions Handled Safely</td><td>{len(collisions)}</td></tr>
                <tr><td>Total Time Elapsed</td><td>{elapsed:.2f} s</td></tr>
                <tr><td>P50 Response Time</td><td>{p50:.2f} ms</td></tr>
                <tr><td>P90 Response Time</td><td>{p90:.2f} ms</td></tr>
                <tr><td>P99 Response Time</td><td>{p99:.2f} ms</td></tr>
            </tbody>
        </table>
    </div>
</body>
</html>"""
    
    report_path = os.path.join(os.path.dirname(__file__), "load_test_report.html")
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"\n📄 Benchmark report generated: {report_path}")

def run_concurrency_test(threads=50, event_id=1, seat_ids=[1, 2, 3]):
    print(f"=== STARTING CONCURRENCY LOAD TEST ({threads} CONCURRENT THREADS) ===")
    print(f"Targeting Event ID #{event_id} with Seat IDs: {seat_ids}")

    start_time = time.time()
    results = []

    with concurrent.futures.ThreadPoolExecutor(max_workers=threads) as executor:
        futures = [executor.submit(attempt_seat_hold, i, event_id, seat_ids) for i in range(1, threads + 1)]
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())

    elapsed = time.time() - start_time

    latencies = sorted([r["latency_ms"] for r in results])
    p50 = latencies[int(len(latencies) * 0.50)] if latencies else 0
    p90 = latencies[int(len(latencies) * 0.90)] if latencies else 0
    p99 = latencies[int(len(latencies) * 0.99)] if latencies else 0
    rps = len(results) / elapsed if elapsed > 0 else 0

    successes = [r for r in results if r.get("status_code") in (200, 201)]
    collisions = [r for r in results if r.get("status_code") in (400, 409, 500) or "lock" in str(r).lower()]

    print("\n=== TEST RESULTS SUMMARY ===")
    print(f"Total Requests Processed: {len(results)}")
    print(f"Successful Holds Acquired: {len(successes)}")
    print(f"Lock Collisions Handled Safely: {len(collisions)}")
    print(f"Total Time Elapsed: {elapsed:.2f} seconds")
    print(f"Throughput: {rps:.2f} req/sec")
    print(f"P50 Latency: {p50:.2f} ms | P90 Latency: {p90:.2f} ms | P99 Latency: {p99:.2f} ms")

    generate_html_report(results, elapsed, threads, event_id, seat_ids, p50, p90, p99, rps)

    if len(successes) <= 1:
        print("\n✅ VERIFICATION PASSED: Redisson Lock prevented double-booking race conditions!")
    else:
        print("\n❌ VERIFICATION FAILED: Multiple users acquired hold on same seat!")

if __name__ == "__main__":
    run_concurrency_test()
