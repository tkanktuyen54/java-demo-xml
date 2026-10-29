// Exercises the real Java -> Python advisor using anonymous session and CSRF cookies.
import assert from 'node:assert/strict';

const base = process.argv[2] || 'http://127.0.0.1:8080';
const cookies = new Map();

function remember(response) {
  for (const value of response.headers.getSetCookie()) {
    const [pair] = value.split(';');
    const separator = pair.indexOf('=');
    cookies.set(pair.slice(0, separator), pair.slice(separator + 1));
  }
}

async function ask(message, courseSlug = null, isolated = false) {
  if (isolated) cookies.clear();
  const csrf = await fetch(new URL('/api/auth/csrf', base), {
    headers: { Cookie: [...cookies].map(([key, value]) => `${key}=${value}`).join('; ') },
  });
  assert.equal(csrf.status, 200);
  remember(csrf);
  const response = await fetch(new URL('/api/chat/messages', base), {
    method: 'POST',
    headers: {
      Cookie: [...cookies].map(([key, value]) => `${key}=${value}`).join('; '),
      'X-XSRF-TOKEN': decodeURIComponent(cookies.get('XSRF-TOKEN') || ''),
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify({ message, courseSlug }),
  });
  remember(response);
  const text = await response.text();
  assert.equal(response.status, 200, text);
  const events = new Map();
  for (const block of text.split(/\r?\n\r?\n/)) {
    const event = /^event:(.*)$/m.exec(block)?.[1]?.trim();
    const data = block.split(/\r?\n/).filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart()).join('\n');
    if (event && data) events.set(event, JSON.parse(data));
  }
  assert(!events.has('error'), JSON.stringify(events.get('error')));
  assert(events.has('done'), text);
  assert(events.has('answer'), text);
  return events;
}

let events = await ask('Mình mới bắt đầu học lập trình, ngân sách dưới 500 nghìn');
assert.deepEqual(events.get('courses').map((course) => course.slug), ['python-co-ban']);
assert.equal(Number(events.get('courses')[0].price), 399000);
assert.equal(events.get('courses')[0].enrolled, false);

events = await ask('Còn Figma thì sao');
assert.equal(events.get('courses')[0].slug, 'ui-ux-figma');
assert(events.get('courses').every((course) => Number(course.price) < 500000));

events = await ask('Mở khóa học này', 'java-spring-boot', true);
assert.equal(events.get('courses')[0].slug, 'java-spring-boot');
assert.equal(events.get('courses')[0].enrolled, false);
events = await ask('Có hoàn tiền không');
assert.equal(events.get('courses').length, 0);

const unauthorized = await fetch(new URL('/api/learning/java-spring-boot/lessons/10', base));
assert.equal(unauthorized.status, 401);
const missingCsrf = await fetch(new URL('/api/chat/messages', base), {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ message: 'Python' }),
});
assert.equal(missingCsrf.status, 403);
console.log('PASS: real Java/Python SSE, anonymous CSRF, budget/level filters, follow-up topic, course action and unchanged learning permissions.');
