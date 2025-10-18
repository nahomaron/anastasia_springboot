import http from 'k6/http';
import { check, sleep } from 'k6';

// Load environment variables
const BASE_URL = __ENV.TEST_BASE_URL || 'http://localhost:8080/api/v1';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || ''; // Optional: JWT token if needed

// 💡 Soak Test Scenario (Long-term stability and reliability)
// Goal: Run a moderate, constant load for an extended period to uncover performance
// degradation, resource leaks (CPU/Memory), and database connection pooling issues.
export const options = {
  stages: [
    // 1. Ramp up to 20 VUs over 5 minutes
    { duration: '5m', target: 20 },
    // 2. Stay at 20 VUs for 4 hours (the soak period)
    { duration: '4h', target: 20 },
    // 3. Ramp down quickly
    { duration: '5m', target: 0 },
  ],
  thresholds: {
    // Over a 4+ hour test, thresholds must be tight to ensure stability.
    http_req_failed: ['rate<0.005'],       // <0.5% requests should fail
    http_req_duration: ['p(95)<750'],      // 95% of requests must be faster than 750ms (allows for occasional slow requests due to background tasks)
    'http_req_duration{expected_response:true}': ['p(99)<1200'], // 99% must be under 1.2s
  },
  // Set execution to be slightly longer than the stages duration for safety
  maxDuration: '4h15m',
};

export default function () {
  // Example endpoint — adjust based on your app
  const url = `${BASE_URL}/members`;

  // Conditionally set Authorization header
  const headers = AUTH_TOKEN
    ? { headers: { Authorization: `Bearer ${AUTH_TOKEN}` } }
    : {};

  const res = http.get(url, headers);

  // --- DEBUGGING LOGIC ---
  if (res.status !== 200) {
    console.error(`[SOAK] Request Failed: URL: ${url} | Status: ${res.status}`);
    // console.error(`Response Body: ${res.body}`);
  }

  // --- STANDARD CHECKS ---
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 750ms (95th percentile target)': (r) => r.timings.duration < 750,
  });

  // Long think time to simulate more realistic user pauses over a long period
  sleep(5);
}
