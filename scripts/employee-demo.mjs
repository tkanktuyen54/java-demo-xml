// Creates one persisted demo employee through real login, CSRF and independent approvals.
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const base = process.argv[2] || 'http://127.0.0.1:8080';
const compose = fileURLToPath(new URL('../BE/compose.yaml', import.meta.url));
const run = randomUUID().slice(0, 8);
const creation = '/api/employee-creation-requests';
const update = '/api/employee-update-requests';
let requests = 0;

class Client {
  cookies = new Map();

  async request(path, { method = 'GET', body, expected = 200 } = {}) {
    const headers = {};
    if (this.cookies.size) {
      headers.Cookie = [...this.cookies].map(([key, value]) => `${key}=${value}`).join('; ');
    }
    if (method !== 'GET' && this.cookies.has('XSRF-TOKEN')) {
      headers['X-XSRF-TOKEN'] = decodeURIComponent(this.cookies.get('XSRF-TOKEN'));
    }
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const response = await fetch(new URL(path, base), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(20000),
    });
    requests += 1;
    for (const value of response.headers.getSetCookie()) {
      const pair = value.split(';')[0];
      const index = pair.indexOf('=');
      const key = pair.slice(0, index);
      const content = pair.slice(index + 1);
      if (/max-age=0/i.test(value) || !content) this.cookies.delete(key);
      else this.cookies.set(key, content);
    }
    const text = await response.text();
    assert.equal(response.status, expected, `${method} ${path}: ${text}`);
    return text ? JSON.parse(text) : null;
  }

  post(path, body, expected = 200) {
    return this.request(path, { method: 'POST', body, expected });
  }
}

async function account(role) {
  const client = new Client();
  const credentials = {
    email: `${role.toLowerCase()}-demo-${run}@example.test`,
    password: `Demo-${randomUUID()}`,
  };
  await client.request('/api/auth/csrf');
  const registered = await client.post(
    '/api/auth/register',
    {
      name: role === 'HR' ? 'HR lập phiếu demo' : 'Người duyệt demo',
      ...credentials,
    },
    201,
  );
  assert(Number.isSafeInteger(registered.id));
  // Grant a role only to the new account created by this run, then log in again.
  execFileSync(
    'docker',
    [
      'compose',
      '-f',
      compose,
      'exec',
      '-T',
      'mysql',
      'sh',
      '-c',
      'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -h 127.0.0.1 -u "$MYSQL_USER" "$MYSQL_DATABASE"',
    ],
    {
      input: `INSERT INTO account_role(user_id, role) SELECT id, '${role}' FROM tbl_user WHERE id = ${registered.id} AND email = '${credentials.email}';`,
      stdio: ['pipe', 'pipe', 'pipe'],
    },
  );
  await client.request('/api/auth/csrf');
  await client.post('/api/auth/login', credentials);
  await client.request('/api/auth/csrf');
  return { client, credentials: { ...credentials, role, id: registered.id } };
}

const maker = await account('HR');
const reviewer = await account('HR_APPROVER');
const target = {
  fullName: 'Nguyễn Văn An (demo)',
  avatar: null,
  personalInformation: { dateOfBirth: '1995-05-20', gender: 'Male', nationality: 'Việt Nam' },
  employment: { hireDate: '2026-11-01' },
  assignment: {
    department: 'Nhân sự',
    position: 'Chuyên viên nhân sự',
    workingType: 'FullTime',
    effectiveFrom: '2026-11-01',
  },
  contacts: [
    {
      type: 'Work',
      email: `an-demo-${run}@example.test`,
      phoneNumber: '0900000001',
      isPrimary: true,
    },
  ],
  addresses: [{ type: 'Permanent', fullAddress: 'Địa chỉ demo, Hà Nội', isPrimary: true }],
  emergencyContacts: [],
  note: `Hồ sơ mẫu lưu thật trong MySQL; lần chạy ${run}`,
};

const draft = await maker.client.post(creation, { target }, 201);
assert.equal(draft.state, 'Draft');
await reviewer.client.request(`${creation}/${draft.id}`, { expected: 404 });
const pending = await maker.client.post(`${creation}/${draft.id}/submit`, {
  version: draft.version,
  reason: 'Tuyển mới nhân viên demo',
});
assert.equal(pending.state, 'PendingApproval');
assert.equal(
  (
    await maker.client.request(
      `/api/employees?${new URLSearchParams({ search: target.contacts[0].email })}`,
    )
  ).totalCount,
  0,
);
await maker.client.post(
  `${creation}/${draft.id}/approve`,
  {
    version: pending.version,
    revisionId: pending.latestRevisionId,
  },
  403,
);
const approved = await reviewer.client.post(`${creation}/${draft.id}/approve`, {
  version: pending.version,
  revisionId: pending.latestRevisionId,
  reason: 'Đồng ý tuyển mới demo',
});
assert.equal(approved.state, 'Approved');
const original = await maker.client.request(`/api/employees/${approved.subjectId}`);
assert.equal(original.version, 1);
assert.equal(original.profile.contacts[0].phoneNumber, '0900000001');

const edit = await maker.client.post(update, { subjectId: original.id }, 201);
assert.deepEqual(edit.baseline, original.profile);
const updatedTarget = structuredClone(edit.target);
updatedTarget.contacts[0].phoneNumber = '0900000002';
updatedTarget.note = `Đã duyệt cập nhật số điện thoại; lần chạy ${run}`;
const saved = await maker.client.request(`${update}/${edit.id}/draft`, {
  method: 'PUT',
  body: { version: edit.version, target: updatedTarget },
});
const submitted = await maker.client.post(`${update}/${edit.id}/submit`, {
  version: saved.version,
  reason: 'Cập nhật số điện thoại liên hệ',
});
assert.equal(submitted.state, 'PendingApproval');
assert.equal(
  (await maker.client.request(`/api/employees/${original.id}`)).profile.contacts[0].phoneNumber,
  '0900000001',
);
const updateApproved = await reviewer.client.post(`${update}/${edit.id}/approve`, {
  version: submitted.version,
  revisionId: submitted.latestRevisionId,
  reason: 'Đã kiểm tra số điện thoại demo',
});
assert.equal(updateApproved.state, 'Approved');
const employee = await maker.client.request(`/api/employees/${original.id}`);
assert.equal(employee.id, original.id);
assert.equal(employee.employeeCode, original.employeeCode);
assert.equal(employee.version, 2);
assert.equal(employee.profile.contacts[0].phoneNumber, '0900000002');
const creationHistory = await maker.client.request(`${creation}/${draft.id}/activity-history`);
const updateHistory = await maker.client.request(`${update}/${edit.id}/activity-history`);
assert.equal(creationHistory.totalCount, 3);
assert.equal(updateHistory.totalCount, 4);
assert.equal(creationHistory.items[0].action, 'Approved');
assert.equal(updateHistory.items[0].action, 'Approved');
assert.equal(updateHistory.items[0].actorId, reviewer.credentials.id);
assert.equal((await maker.client.request(`${update}/${edit.id}/revisions`)).length, 1);
await maker.client.post('/api/auth/logout', {}, 204);
await reviewer.client.post('/api/auth/logout', {}, 204);
const result = {
  status: 'PASS',
  base,
  run,
  requests,
  completedAt: new Date().toISOString(),
  accounts: [maker.credentials, reviewer.credentials],
  employee,
  creationRequest: approved,
  updateRequest: updateApproved,
  creationHistory,
  updateHistory,
};
const resultPath = new URL('../BE/target/employee-demo-result.json', import.meta.url);
mkdirSync(new URL('../BE/target/', import.meta.url), { recursive: true });
writeFileSync(resultPath, JSON.stringify(result, null, 2), { mode: 0o600 });
console.log(
  `PASS: creation -> independent approval -> update -> independent approval (${requests} HTTP requests).`,
);
console.log(
  `Employee: ${employee.employeeCode}; UUID: ${employee.id}; version: ${employee.version}`,
);
console.log(`Requests: creation ${draft.id}, update ${edit.id}. Data retained in MySQL.`);
console.log(`Result and demo login credentials: ${fileURLToPath(resultPath)}`);
