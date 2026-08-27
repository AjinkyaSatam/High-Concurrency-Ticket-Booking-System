# TicketVerse Concurrency Load Test Suite 🧪

This test suite simulates high-concurrency race conditions where 50+ threads attempt to acquire Redis distributed locks on identical seat IDs simultaneously.

## Running the Test

1. Ensure the Spring Boot backend is running on `http://localhost:8080`.
2. Ensure Redis 7 container is healthy.
3. Run the Python load test script:

```bash
python load-test/concurrency_load_test.py
```

## Expected Behavior
- **Exactly 1 request** will succeed in acquiring the Redis distributed multi-lock.
- **49+ requests** will fail gracefully with high-concurrency collision responses.
- **Zero double bookings** or database lock leaks will occur.
