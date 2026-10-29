import { Component, ElementRef, OnDestroy, inject, signal, viewChild } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ChatCourse, ChatService } from '../core/chat.service';
import { AuthService } from '../core/auth.service';

interface Message {
  id: number;
  role: 'user' | 'assistant';
  text: string;
  courses: ChatCourse[];
}

@Component({
  selector: 'app-course-chat',
  imports: [FormsModule, RouterLink, CurrencyPipe],
  templateUrl: './course-chat.component.html',
  styleUrl: './course-chat.component.css',
})
export class CourseChatComponent implements OnDestroy {
  readonly auth = inject(AuthService);
  private readonly chat = inject(ChatService);
  private readonly router = inject(Router);
  private readonly input = viewChild<ElementRef<HTMLTextAreaElement>>('questionInput');
  private readonly log = viewChild<ElementRef<HTMLDivElement>>('messageLog');
  private readonly launcher = viewChild<ElementRef<HTMLButtonElement>>('launcher');
  private controller?: AbortController;
  private nextId = 1;
  readonly open = signal(false);
  readonly pending = signal(false);
  readonly status = signal('');
  readonly error = signal('');
  readonly messages = signal<Message[]>([]);
  readonly prompts = [
    'Liệt kê các danh mục khóa học trên Khoa',
    'Mình mới bắt đầu học lập trình',
    'Tìm khóa học dưới 500 nghìn',
  ];
  question = '';

  onEnter(event: Event): void {
    const key = event as KeyboardEvent;
    if (!key.shiftKey && !key.isComposing) {
      key.preventDefault();
      void this.send();
    }
  }

  toggle(): void {
    this.open.update((value) => !value);
    if (this.open()) {
      setTimeout(() => this.input()?.nativeElement.focus());
      this.scroll();
    } else {
      setTimeout(() => this.launcher()?.nativeElement.focus());
    }
  }

  async send(text = this.question): Promise<void> {
    text = text.trim();
    if (!text || text.length > 2000 || this.pending()) return;
    this.question = '';
    this.error.set('');
    this.pending.set(true);
    this.status.set('Đang tìm khóa học phù hợp…');
    const replyId = this.nextId++;
    this.messages.update((items) => [
      ...items.slice(-30),
      { id: this.nextId++, role: 'user', text, courses: [] },
      { id: replyId, role: 'assistant', text: '', courses: [] },
    ]);
    this.controller = new AbortController();
    this.scroll();
    const slug =
      /^\/(?:courses|learn|checkout)\/([a-z0-9-]+)(?:[?#]|$)/.exec(this.router.url)?.[1] ?? null;
    try {
      await this.chat.ask(text, slug, this.controller.signal, (event) => {
        if (event.event === 'status') this.status.set((event.data as { message: string }).message);
        if (event.event === 'answer') {
          const answer = event.data as { text: string };
          this.update(replyId, { text: answer.text });
        }
        if (event.event === 'courses')
          this.update(replyId, { courses: event.data as ChatCourse[] });
        this.scroll();
      });
    } catch (error) {
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        this.error.set(error instanceof Error ? error.message : 'Chưa kết nối được trợ lý.');
        this.question = text;
      }
    } finally {
      this.messages.update((items) =>
        items.filter((item) => item.id !== replyId || item.text || item.courses.length),
      );
      this.pending.set(false);
      this.status.set('');
      this.scroll();
    }
  }

  private update(id: number, changes: Partial<Message>): void {
    this.messages.update((items) =>
      items.map((item) => (item.id === id ? { ...item, ...changes } : item)),
    );
  }

  private scroll(): void {
    requestAnimationFrame(() => {
      const element = this.log()?.nativeElement;
      if (element) element.scrollTop = element.scrollHeight;
    });
  }

  ngOnDestroy(): void {
    this.controller?.abort();
  }
}
