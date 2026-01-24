import http from 'k6/http';
import { check, sleep, fail } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export const ENDPOINTS = {
  SIGNUP: '/auth/sign-up',
  ACTIVATE: '/auth/activate-account',
  TEST_ACTIVATION: '/auth/test/activation-token',
  LOGIN: '/auth/login',
};

export function jsonHeaders() {
  return { headers: { 'Content-Type': 'application/json' } };
}

export function randomEmail() {
  return `k6_user_${Math.floor(Math.random() * 1e9)}@mail.com`;
}

export function healthCheck() {
  const res = http.get(`${BASE_URL.replace('/api/v1', '')}/actuator/health`);
  check(res, { 'health is UP': (r) => r.status === 200 });
}

const DEFAULT_OWNER_PASSWORD = __ENV.OWNER_PASSWORD || 'Password@123';
const OWNER_NAME = __ENV.OWNER_NAME || 'K6 Load Owner';
const OWNER_TENANT_TYPE = __ENV.OWNER_TENANT_TYPE || 'CHURCH';
const OWNER_SUBSCRIPTION_PLAN = __ENV.OWNER_SUBSCRIPTION_PLAN || 'BASIC';

function randomDigits(length) {
  const min = Math.pow(10, length - 1);
  return Math.floor(Math.random() * 9 * min + min).toString();
}

function generatePhoneNumber() {
  // Ensures we respect E.164-like format (+1 followed by 9 digits by default)
  if (__ENV.OWNER_PHONE) {
    return __ENV.OWNER_PHONE;
  }
  return `+1555${randomDigits(7)}`;
}

function pollForTestArtifact(pathWithQuery, entityDescription, maxAttempts = 10, waitSeconds = 0.5) {
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const res = http.get(`${BASE_URL}${pathWithQuery}`);
    if (res.status === 200 && res.body && res.body.trim().length > 0) {
      return res.body.trim();
    }
    sleep(waitSeconds);
  }
  fail(`Failed to fetch ${entityDescription} after ${maxAttempts} attempts`);
}

export function provisionOwnerAccount() {
  const existingEmail = __ENV.OWNER_EMAIL;
  const password = DEFAULT_OWNER_PASSWORD;

  if (existingEmail) {
    const loginRes = http.post(
      `${BASE_URL}${ENDPOINTS.LOGIN}`,
      JSON.stringify({ email: existingEmail, password }),
      jsonHeaders()
    );
    check(loginRes, {
      'owner login (env) 200': (r) => r.status === 200,
      'owner login (env) token issued': (r) => (r.json('accessToken') || '').length > 10,
    }) || fail(`Owner login failed for ${existingEmail}: ${loginRes.status} ${loginRes.body}`);

    return {
      email: existingEmail,
      password,
      accessToken: loginRes.json('accessToken'),
      refreshToken: loginRes.json('refreshToken'),
    };
  }

  const email = `owner_${Math.floor(Math.random() * 1e12)}@mail.com`;
  const phone = generatePhoneNumber();

  const subscriptionPayload = JSON.stringify({
    tenantType: OWNER_TENANT_TYPE,
    subscriptionPlan: OWNER_SUBSCRIPTION_PLAN,
    ownerName: OWNER_NAME,
    email,
    phoneNumber: phone,
    password,
    confirmPassword: password,
  });

  const subscriptionRes = http.post(
    `${BASE_URL}/tenant/subscription`,
    subscriptionPayload,
    jsonHeaders()
  );
  check(subscriptionRes, { 'tenant subscription 201': (r) => r.status === 201 }) ||
    fail(`Tenant subscription failed: ${subscriptionRes.status} ${subscriptionRes.body}`);

  const otp = pollForTestArtifact(
    `/tenant/test/otp?phone=${encodeURIComponent(phone)}`,
    `OTP for ${phone}`
  );

  const verificationRes = http.post(
    `${BASE_URL}/tenant/verify-phone`,
    JSON.stringify({ phone, otp }),
    jsonHeaders()
  );
  check(verificationRes, { 'phone verification 200': (r) => r.status === 200 }) ||
    fail(`Phone verification failed: ${verificationRes.status} ${verificationRes.body}`);

  const activationToken = pollForTestArtifact(
    `/auth/test/activation-token?email=${encodeURIComponent(email)}`,
    `activation token for ${email}`
  );

  const activationRes = http.get(
    `${BASE_URL}${ENDPOINTS.ACTIVATE}?token=${encodeURIComponent(activationToken)}`
  );
  check(activationRes, { 'account activation 200': (r) => r.status === 200 }) ||
    fail(`Account activation failed: ${activationRes.status} ${activationRes.body}`);

  const loginRes = http.post(
    `${BASE_URL}${ENDPOINTS.LOGIN}`,
    JSON.stringify({ email, password }),
    jsonHeaders()
  );
  check(loginRes, {
    'owner login 200': (r) => r.status === 200,
    'owner token present': (r) => (r.json('accessToken') || '').length > 10,
  }) || fail(`Owner login failed: ${loginRes.status} ${loginRes.body}`);

  return {
    email,
    phone,
    password,
    accessToken: loginRes.json('accessToken'),
    refreshToken: loginRes.json('refreshToken'),
  };
}
