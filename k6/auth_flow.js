import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// ---- Config (env-driven, no code edits needed) ----
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const SIGNUP = __ENV.SIGNUP_ENDPOINT || '/auth/sign-up';
const ACTIVATE = __ENV.ACTIVATE_ENDPOINT || '/auth/activate-account';
const TEST_ACTIVATION = __ENV.TEST_ACTIVATION_ENDPOINT || '/auth/test/activation-token';
const LOGIN = __ENV.LOGIN_ENDPOINT || '/auth/login';

// ---- Metrics & thresholds ----
export let options = {
  // Simple, safe defaults (tweak later)
  vus: Number(__ENV.VUS || 10),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],          // <1% failures
    http_req_duration: ['p(95)<800'],        // P95 < 800ms
  },
};

// Custom metric for login latency
const loginTrend = new Trend('auth_login_duration');
const errorRate = new Rate('auth_error_rate');

// Utility
function jsonHeaders() {
  return { headers: { 'Content-Type': 'application/json' } };
}

function randomEmail() {
  const n = Math.floor(Math.random() * 1e9);
  return `k6_user_${n}@mail.com`;
}

// Setup: sign-up -> fetch activation token -> activate -> login once
export function setup() {
  const email = randomEmail();
  const password = 'Password@123';

  group('signup', () => {
    const payload = JSON.stringify({
      fullName: 'K6 User',
      email,
      password,
      confirmPassword: password,
    });
    const res = http.post(`${BASE_URL}${SIGNUP}`, payload, jsonHeaders());
    check(res, {
      'signup status is 201': r => r.status === 201,
    }) || fail(`Signup failed: ${res.status} ${res.body}`);
  });

  let token = '';
  group('fetch-activation-token', () => {
    const res = http.get(`${BASE_URL}${TEST_ACTIVATION}?email=${encodeURIComponent(email)}`);
    check(res, {
      'activation token retrieved': r => r.status === 200 && r.body && r.body.length > 10,
    }) || fail(`Activation token fetch failed: ${res.status} ${res.body}`);
    token = res.body;
  });

  group('activate-account', () => {
    const res = http.get(`${BASE_URL}${ACTIVATE}?token=${encodeURIComponent(token)}`);
    check(res, {
      'activation ok (200)': r => r.status === 200,
    }) || fail(`Activation failed: ${res.status} ${res.body}`);
  });

  // Optional: one sanity login during setup
  group('initial-login', () => {
    const res = http.post(
      `${BASE_URL}${LOGIN}`,
      JSON.stringify({ email, password }),
      jsonHeaders(),
    );
    check(res, {
      'initial login 200': r => r.status === 200,
      'has access_token': r => (r.json('access_token') || '').length > 10,
    }) || fail(`Initial login failed: ${res.status} ${res.body}`);
  });

  // Share creds to all VUs
  return { email, password };
}

// VUs hammer the login endpoint using the setup credentials
export default function (data) {
  const { email, password } = data;

  const t0 = Date.now();
  const res = http.post(
    `${BASE_URL}${LOGIN}`,
    JSON.stringify({ email, password }),
    jsonHeaders(),
  );
  const dt = Date.now() - t0;
  loginTrend.add(dt);

  const ok = check(res, {
    'login 200': r => r.status === 200,
    'has access_token': r => (r.json('access_token') || '').length > 10,
  });

  errorRate.add(!ok);
  if (!ok) {
    // Keep noisy logs off unless needed. Uncomment for debugging:
     console.error(`Login failed: ${res.status} ${res.body}`);
  }

  sleep(1);
}

// Export JSON summary when run with: k6 run --summary-export=summary.json
export function handleSummary(data) {
  return {
    'k6-summary.json': JSON.stringify(data, null, 2),
  };
}
