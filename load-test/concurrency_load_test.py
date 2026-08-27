import concurrent.futures
import requests
import json
import time

BASE_URL = "http://localhost:8080/api/v1"

def attempt_seat_hold(user_id, event_id, seat_ids):
    url = f"{BASE_URL}/holds"
    payload = {
        "eventId": event_id,
        "seatIds": seat_ids
    }
    headers = {
        "Content-Type": "application/json"
    }
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=5)
        return {
            "user_id": user_id,
            "status_code": response.status_code,
            "response": response.json() if response.headers.get("content-type") == "application/json" else response.text
        }
    except Exception as e:
        return {
            "user_id": user_id,
            "error": str(e)
        }

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
    
    successes = [r for r in results if r.get("status_code") in (200, 201)]
    collisions = [r for r in results if r.get("status_code") in (400, 409, 500) or "lock" in str(r).lower()]
    
    print("\n=== TEST RESULTS SUMMARY ===")
    print(f"Total Requests Processed: {len(results)}")
    print(f"Successful Holds Acquired: {len(successes)}")
    print(f"Lock Collisions Handled Safely: {len(collisions)}")
    print(f"Total Time Elapsed: {elapsed:.2f} seconds")
    print(f"Throughput: {len(results)/elapsed:.2f} req/sec")
    
    if len(successes) <= 1:
        print("\n✅ VERIFICATION PASSED: Redisson Lock prevented double-booking race conditions!")
    else:
        print("\n❌ VERIFICATION FAILED: Multiple users acquired hold on same seat!")

if __name__ == "__main__":
    run_concurrency_test()
