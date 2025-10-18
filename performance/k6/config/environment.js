import http from 'k6/http';
import { check } from 'k6';

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
