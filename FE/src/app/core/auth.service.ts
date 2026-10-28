import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { User } from './models';
import { errorMessage } from './api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly user = signal<User | null>(null);
  readonly notice = signal('');

  async csrf(): Promise<void> {
    await firstValueFrom(this.http.get('/api/auth/csrf'));
  }

  async restore(): Promise<void> {
    try {
      await this.csrf();
      this.user.set(await firstValueFrom(this.http.get<User>('/api/auth/me')));
    } catch (error) {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        this.notice.set(errorMessage(error));
      }
      this.user.set(null);
    }
  }

  async submit(
    mode: 'login' | 'register',
    body: { email: string; password: string; name?: string },
  ): Promise<void> {
    await this.csrf();
    const user = await firstValueFrom(this.http.post<User>(`/api/auth/${mode}`, body));
    this.user.set(user);
    await this.csrf();
    this.notice.set('');
  }

  async logout(): Promise<void> {
    try {
      await this.csrf();
      await firstValueFrom(this.http.post('/api/auth/logout', {}));
      this.user.set(null);
      await this.csrf();
      await this.router.navigateByUrl('/');
    } catch (error) {
      this.notice.set(errorMessage(error));
    }
  }
}
