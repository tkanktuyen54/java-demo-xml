import assert from 'node:assert/strict';
import { test } from 'node:test';
import { readChatEvents } from './chat-stream.ts';

test('SSE handles UTF-8 and CRLF split at every byte', async () => {
  const bytes = new TextEncoder().encode(
    'event:answer\r\ndata:{"text":"Khóa học tiếng Việt"}\r\n\r\nevent:done\r\ndata:{}\r\n\r\n',
  );
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      for (const byte of bytes) controller.enqueue(new Uint8Array([byte]));
      controller.close();
    },
  });
  const events = [];
  for await (const event of readChatEvents(body)) events.push(event);
  assert.deepEqual(events, [
    { event: 'answer', data: { text: 'Khóa học tiếng Việt' } },
    { event: 'done', data: {} },
  ]);
});

test('SSE ignores comments and supports multiline data in one chunk', async () => {
  const response = new Response(
    ':heartbeat\n\nevent:answer\ndata:{\ndata: "text": "Xin chào"\ndata:}\n\n',
  );
  const events = [];
  for await (const event of readChatEvents(response.body!)) events.push(event);
  assert.deepEqual(events, [{ event: 'answer', data: { text: 'Xin chào' } }]);
});

test('Malformed JSON closes the stream instead of leaving its reader locked', async () => {
  let cancelled = false;
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      controller.enqueue(new TextEncoder().encode('event:answer\ndata:invalid\n\n'));
    },
    cancel() {
      cancelled = true;
    },
  });
  await assert.rejects(async () => {
    for await (const event of readChatEvents(body)) void event;
  }, SyntaxError);
  assert.equal(cancelled, true);
  assert.equal(body.locked, false);
});
