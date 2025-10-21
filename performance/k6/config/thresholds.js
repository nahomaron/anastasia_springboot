/**
 * Global K6 Threshold Configuration
 * ------------------------------------
 * Defines performance SLAs (Service Level Agreements) for each endpoint group.
 * These thresholds are automatically enforced by K6 during CI runs —
 * if any condition fails (e.g., p95 latency too high, failure rate > 1%),
 * the job exits with a non-zero code and your pipeline will fail.
 *
 * You can customize each group independently (auth, members, users, etc.).
 */

export const thresholds = {
  // Authentication & Login APIs
  auth: {
    http_req_failed: ['rate<0.01'],       // <1% requests should fail
    auth_login_duration: ['p(95)<800'],   // 95% of logins < 800 ms
  },

  // Member Management APIs
  members: {
    http_req_failed: ['rate<0.01'],          // reliability target
    members_list_duration: ['p(95)<700'],    // p95 < 700 ms for member listing
    members_error_rate: ['rate<0.01'],       // <1% endpoint-specific errors
  },

  // User Management APIs
  users: {
    http_req_failed: ['rate<0.01'],
    user_dashboard_duration: ['p(95)<600'],      // tighter SLA for smaller payloads
  },

  // Default global thresholds (applied when not overridden)
  default: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
  },
};

/**
 * Helper utility to safely retrieve thresholds by key.
 * Falls back to the global default if a category isn’t defined.
 */
export function getThresholds(category) {
  return thresholds[category] || thresholds.default;
}
