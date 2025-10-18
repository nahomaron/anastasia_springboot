import http from 'k6/http';
import { check, sleep } from 'k6';

//  Load environment variables
const BASE_URL = __ENV.TEST_BASE_URL || 'http://localhost:8080/api/v1';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || ''; // Optional: JWT token if needed

// 💡 Basic load scenario (light traffic)
export const options = {
  vus: 5,             // number of virtual users
  duration: '30s',    // total test duration
  thresholds: {
    // Note: The overall test still fails if this is crossed, but we'll see the reason now.
    http_req_failed: ['rate<0.01'],        // <1% requests should fail
    http_req_duration: ['p(95)<500'],      // 95% of requests <500ms
  },
};

export default function () {
  // Example endpoint — adjust based on your app
  const url = `${BASE_URL}/registrar/members`;

  // Conditionally set Authorization header
  const headers = AUTH_TOKEN
    ? { headers: { Authorization: `Bearer ${AUTH_TOKEN}` } }
    : {};

  const res = http.get(url, headers);

  // --- DEBUGGING LOGIC ADDED HERE ---

  // 1. Check if the status is NOT 200.
  if (res.status !== 200) {
    // 2. If it's not 200, log the exact status code to the console for debugging.
    console.error(`Request Failed: URL: ${url} | Status: ${res.status}`);

    // Optionally log the response body if it might contain an error message
    // console.error(`Response Body: ${res.body}`);
  }

  // --- STANDARD CHECKS ---
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1); // think time between requests
}
