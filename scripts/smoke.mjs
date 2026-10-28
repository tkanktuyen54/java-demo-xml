// Runs the real cookie/CSRF flow against a running backend and creates one local test learner.
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';

const base = process.argv[2] || 'http://127.0.0.1:8080';
const cookies = new Map();
const email = `smoke-${randomUUID()}@example.com`;
const password = `Test-${randomUUID()}`;

async function request(path, { method = 'GET', body, expected = 200, authenticated = true } = {}) {
  const headers = {};
  if (authenticated && cookies.size) { headers.Cookie = [...cookies].map(([key, value]) => `${key}=${value}`).join('; '); }
  if (method === 'POST' && cookies.has('XSRF-TOKEN')) { headers['X-XSRF-TOKEN'] = decodeURIComponent(cookies.get('XSRF-TOKEN')); }
  if (body !== undefined) { headers['Content-Type'] = 'application/json'; }
  const response = await fetch(new URL(path, base), { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  for (const value of response.headers.getSetCookie()) {
    const [pair] = value.split(';');
    const position = pair.indexOf('=');
    const key = pair.slice(0, position);
    const content = pair.slice(position + 1);
    if (/max-age=0/i.test(value) || !content) { cookies.delete(key); } else { cookies.set(key, content); }
  }
  const text = await response.text();
  assert.equal(response.status, expected, `${method} ${path}: ${text}`);
  return text ? JSON.parse(text) : null;
}

const catalogue = await request('/api/courses');
assert.equal(catalogue.length, 6);
assert(!JSON.stringify(catalogue).includes('videoUrl'));
await request('/api/auth/csrf');
await request('/api/auth/register', { method: 'POST', body: { name: 'Học viên kiểm thử', email, password }, expected: 201 });
await request('/api/auth/csrf');
assert.equal((await request('/api/auth/me')).email, email);
await request('/api/learning/angular-thuc-chien/lessons/2', { expected: 403 });
await request('/api/enrollments/angular-thuc-chien/demo-checkout', { method: 'POST', body: {} });
await request('/api/enrollments/angular-thuc-chien/demo-checkout', { method: 'POST', body: {} });
assert.equal((await request('/api/enrollments')).length, 1);
const video = await request('/api/learning/angular-thuc-chien/lessons/2');
assert.match(video.videoUrl, /^\/api\/media\/lessons\/\d+$/);
const media = await fetch(new URL(video.videoUrl, base), { headers: {
  Cookie: [...cookies].map(([key, value]) => `${key}=${value}`).join('; '), Range: 'bytes=0-127'
} });
assert.equal(media.status, 206);
assert.equal(media.headers.get('content-type'), 'video/mp4');
assert.equal((await media.arrayBuffer()).byteLength, 128);
await request('/api/learning/angular-thuc-chien/lessons/9', { expected: 404 });
await request('/api/learning/angular-thuc-chien/lessons/2/complete', { method: 'POST', body: {} });
assert.equal((await request('/api/enrollments'))[0].completedLessons, 1);
await request('/api/auth/logout', { method: 'POST', body: {}, expected: 204 });
await request('/api/auth/me', { expected: 401 });
await request('/api/auth/csrf');
await request('/api/auth/login', { method: 'POST', body: { email, password } });
await request('/api/auth/csrf');
assert.equal((await request('/api/enrollments'))[0].completedLessons, 1);
await request('/api/auth/logout', { method: 'POST', body: {}, expected: 204 });
console.log('PASS: catalogue, real CSRF cookies, registration, login/logout, protected MP4 streaming with byte ranges, idempotent checkout and persisted progress.');
