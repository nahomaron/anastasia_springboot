import { Trend, Rate, Counter } from 'k6/metrics';

// ---- General-purpose metrics ----
export const httpDuration = new Trend('http_req_duration', true);
export const httpFailures = new Rate('http_req_failed');
export const requestCount = new Counter('http_reqs');

// ---- Authentication metrics ----
export const authLoginDuration = new Trend('auth_login_duration', true);
export const authErrorRate = new Rate('auth_error_rate');

// ---- Member endpoint metrics ----
export const membersListDuration = new Trend('members_list_duration', true);
export const membersErrorRate = new Rate('members_error_rate');

// ---- User endpoint metrics ----
export const userFetchDuration = new Trend('user_fetch_duration', true);
export const userErrorRate = new Rate('user_error_rate');

// ---- Utility to register dynamic metrics ----
export function createCustomTrend(name, desc) {
  console.log(`📊 Creating custom trend: ${name} (${desc || 'no description'})`);
  return new Trend(name, true);
}
