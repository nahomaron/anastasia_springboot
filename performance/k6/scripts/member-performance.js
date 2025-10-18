import http from 'k6/http';
import { sleep, check, fail } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, jsonHeaders } from '../config/environment.js';
import { getThresholds } from '../config/thresholds.js';

// ---- Thresholds ----
export const options = {
  vus: 10,
  duration: '30s',
  thresholds: getThresholds('members'),
};

// ---- Metrics ----
const membersListDuration = new Trend('members_list_duration');
const membersErrorRate = new Rate('members_error_rate');

// ---- Setup ----
export function setup() {
  const res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: __ENV.OWNER_EMAIL || 'loadtest_owner@mail.com',
    password: __ENV.OWNER_PASSWORD || 'Password@123',
  }), jsonHeaders());

  check(res, {
    'owner login ok': (r) => r.status === 200,
    'token present': (r) => (r.json('access_token') || '').length > 10,
  }) || fail(`Login failed: ${res.status} ${res.body}`);

  return { token: res.json('access_token') };
}

// ---- Load Test ----
export default function (data) {
  const start = Date.now();
  const res = http.get(`${BASE_URL}/registrar/members`, {
    headers: { Authorization: `Bearer ${data.token}` },
  });
  const elapsed = Date.now() - start;
  membersListDuration.add(elapsed);

  const ok = check(res, {
    'list members 200': (r) => r.status === 200,
  });

  membersErrorRate.add(!ok);
  if (!ok && __ENV.DEBUG === 'true') console.error(`Member list failed: ${res.status} ${res.body}`);

  sleep(1);
}

// ---- Summary ----
export function handleSummary(data) {
  return { 'performance/k6/results/member-summary.json': JSON.stringify(data, null, 2) };
}
