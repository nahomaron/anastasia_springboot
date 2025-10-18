import http from 'k6/http';
import { check, sleep } from 'k6';

// Load environment variables
const BASE_URL = __ENV.TEST_BASE_URL || 'http://localhost:8080/api/v1';
const AUTH_TOKEN = __ENV.AUTH_TOKEN || ''; // Optional: JWT token if needed

// 💡 Stress Test Scenario (Finding the system's breaking point and max capacity)
// Goal: Gradually ramp up Virtual Users (VUs) until the system fails or performance
// significantly degrades, determining the maximum user load the system can handle.
export const options = {
  stages: [
    // 1. Initial warm-up load (5 minutes at 10 VUs)
    { duration: '5m', target: 10 },
    // 2. Gradual increase to medium load (10 minutes up to 50 VUs)
    { duration: '10m', target: 50 },
    // 3. High load (10 minutes up to 150 VUs) - Where we expect performance degradation to start
    { duration: '10m', target: 150 },
    // 4. Extreme load (5 minutes up to 300 VUs) - Pushing to the breaking point
    { duration: '5m', target: 300 },
    // 5. Ramp down quickly (1 minute)
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    // Note: For a stress test, we expect some failure/slowdown at the highest loads.
    // The key is to monitor the *point* at which these thresholds are breached.
    http_req_failed: ['rate<0.10'],        // We tolerate up to 10% errors overall
    http_req_duration: ['p(90)<1000'],     // 90% of requests should be under 1s
    // A specific high-load threshold to monitor when the system hits capacity
    'http_req_duration{stage:extreme}': ['p(50)<2000'], // Half of the requests in the final stage must be under 2s
  },
  // Total test duration is 31 minutes
};

export default function () {
  // Example endpoint — adjust based on your app
  const url = `${BASE_URL}/registrar/members`;

  // Conditionally set Authorization header
  const headers = AUTH_TOKEN
    ? { headers: { Authorization: `Bearer ${AUTH_TOKEN}` } }
    : {};

  const res = http.get(url, headers);

  // --- DEBUGGING LOGIC ---
  if (res.status !== 200) {
    console.error(`[STRESS] Request Failed: URL: ${url} | Status: ${res.status} | VU: ${__VU}`);
    // console.error(`Response Body: ${res.body}`);
  }

  // --- STANDARD CHECKS ---
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 1000ms (90th percentile target)': (r) => r.timings.duration < 1000,
  });

  // Short sleep time to maximize load intensity during high-stress stages
  sleep(0.5);
}
