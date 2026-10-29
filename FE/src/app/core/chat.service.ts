import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { ChatEvent, readChatEvents } from './chat-stream';

export interface ChatCourse {
  slug: string;
  title: string;
  price: number;
  level: string;
  reason: string;
  available: boolean;
  enrolled: boolean;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly auth = inject(AuthService);

  async ask(
    message: string,
    courseSlug: string | null,
    signal: AbortSignal,
    receive: (event: ChatEvent) => void,
  ): Promise<void> {
    await this.auth.csrf();
    if (signal.aborted) return;
    const cookie = document.cookie.split('; ').find((item) => item.startsWith('XSRF-TOKEN='));
    const response = await fetch('/api/chat/messages', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        'X-XSRF-TOKEN': cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : '',
      },
      body: JSON.stringify({ message, courseSlug }),
      signal,
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || 'Chưa kết nối được trợ lý. Bạn thử lại nhé.');
    }
    if (!response.body || !response.headers.get('Content-Type')?.includes('text/event-stream')) {
      throw new Error('Trợ lý chưa trả lời được. Bạn thử lại nhé.');
    }
    let completed = false;
    for await (const event of readChatEvents(response.body)) {
      if (event.event === 'error') {
        throw new Error((event.data as { message: string }).message);
      }
      receive(event);
      if (event.event === 'done') {
        completed = true;
        break;
      }
    }
    if (!completed) throw new Error('Kết nối bị ngắt. Bạn hãy gửi lại câu hỏi nhé.');
  }
}
