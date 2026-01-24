import http from 'k6/http';
import { sleep, check, group, fail } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, ENDPOINTS, jsonHeaders, randomEmail } from '../config/environment.js';
import { getThresholds } from '../config/thresholds.js';

// ---- Thresholds ----
export const options = {
  vus: Number(__ENV.VUS || 10),
  duration: __ENV.DURATION || '30s',
  thresholds: getThresholds('auth'),
};

// ---- Metrics ----
const loginDuration = new Trend('auth_login_duration');
const authErrorRate = new Rate('auth_error_rate');

// ---- Setup ----
export function setup() {
  const email = randomEmail();
  const password = 'Password@123';
  let activationToken = '';

  group('Sign-up', () => {
    const payload = JSON.stringify({
      fullName: 'K6 User',
      email,
      password,
      confirmPassword: password,
    });
    const res = http.post(`${BASE_URL}${ENDPOINTS.SIGNUP}`, payload, jsonHeaders());
    check(res, { 'signup 201': (r) => r.status === 201 }) ||
      fail(`Signup failed: ${res.status} ${res.body}`);
  });

  group('Fetch activation token', () => {
    const res = http.get(`${BASE_URL}${ENDPOINTS.TEST_ACTIVATION}?email=${encodeURIComponent(email)}`);
    const token = (res.body || '').trim();
    check(res, {
      'activation token ok': (r) => r.status === 200 && token.length >= 6,
    }) || fail(`Activation fetch failed: ${res.status} ${res.body}`);
    activationToken = token;
  });

  group('Activate account', () => {
    const res = http.get(`${BASE_URL}${ENDPOINTS.ACTIVATE}?token=${encodeURIComponent(activationToken)}`);
    check(res, { 'activation ok (200)': (r) => r.status === 200 }) ||
      fail(`Activation failed: ${res.status} ${res.body}`);
  });

  group('Initial login', () => {
    const res = http.post(
      `${BASE_URL}${ENDPOINTS.LOGIN}`,
      JSON.stringify({ email, password }),
      jsonHeaders()
    );
    check(res, {
      'login ok (200)': (r) => r.status === 200,
      'has access token': (r) => (r.json('accessToken') || '').length > 10,
    }) || fail(`Login failed: ${res.status} ${res.body}`);
  });

  return { email, password };
}

// ---- Load Test ----
export default function (data) {
  const { email, password } = data;

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}${ENDPOINTS.LOGIN}`,
    JSON.stringify({ email, password }),
    jsonHeaders()
  );
  const elapsed = Date.now() - start;
  loginDuration.add(elapsed);

  const ok = check(res, {
    'login 200': (r) => r.status === 200,
    'token present': (r) => (r.json('accessToken') || '').length > 10,
  });

  authErrorRate.add(!ok);
  if (!ok && __ENV.DEBUG === 'true')
    console.error(`Auth login failed: ${res.status} ${res.body}`);

  sleep(1);
}

// ---- Summary ----
export function handleSummary(data) {
  return { 'performance/results/auth-summary.json': JSON.stringify(data, null, 2) };
}
