import http from 'k6/http';
import { sleep, check } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { BASE_URL, jsonHeaders, provisionOwnerAccount } from '../config/environment.js';
import { getThresholds } from '../config/thresholds.js';

// ---- Thresholds ----
export const options = {
  vus: 10,
  duration: '30s',
  thresholds: getThresholds('users'),
};

// ---- Metrics ----
const userFetchDuration = new Trend('user_fetch_duration');
const userErrorRate = new Rate('user_error_rate');

// ---- Setup ----
export function setup() {
  if (__ENV.ACCESS_TOKEN) {
    return { token: __ENV.ACCESS_TOKEN };
  }

  const ownerSession = provisionOwnerAccount();
  return { token: ownerSession.accessToken };
}

// ---- Default ----
export default function (data) {
  const start = Date.now();
  const res = http.get(`${BASE_URL}/users/`, {
    headers: { Authorization: `Bearer ${data.token}` },
  });
  const elapsed = Date.now() - start;
  userFetchDuration.add(elapsed);

  const ok = check(res, {
    'fetch users 200': (r) => r.status === 200,
  });

  userErrorRate.add(!ok);
  if (!ok && __ENV.DEBUG === 'true') console.error(`User fetch failed: ${res.status} ${res.body}`);

  sleep(1);
}

// ---- Summary ----
export function handleSummary(data) {
  return { 'performance/k6/results/user-summary.json': JSON.stringify(data, null, 2) };
}
