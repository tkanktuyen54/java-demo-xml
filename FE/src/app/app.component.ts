import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { CourseChatComponent } from './shared/course-chat.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CourseChatComponent],
  template: `
    <a class="skip-link" href="#main">Chuyển đến nội dung</a>
    <div class="announcement">
      Một kỹ năng mới. Một cánh cửa mới.
      <span>
        Bắt đầu hành trình của bạn
        <span aria-hidden="true">↗</span>
      </span>
    </div>
    <header class="site-header">
      <div class="container nav">
        <a class="brand" routerLink="/" aria-label="Khoa, trang chủ">
          <span class="brand-symbol" aria-hidden="true">k</span>
          khoa
          <span class="brand-dot">.</span>
        </a>
        <nav aria-label="Điều hướng chính">
          <a routerLink="/" [routerLinkActiveOptions]="{ exact: true }" routerLinkActive="active">
            Khám phá khóa học
          </a>
          <a routerLink="/my-learning" routerLinkActive="active">Góc học tập</a>
        </nav>
        <div class="nav-actions">
          @if (auth.user(); as user) {
            <a class="user-link" routerLink="/my-learning">
              <span class="avatar">{{ user.name.charAt(0) }}</span>
              <span class="user-name">{{ user.name }}</span>
            </a>
            <button class="text-button logout" (click)="auth.logout()">Đăng xuất</button>
          } @else {
            <a class="login-link" routerLink="/login">Đăng nhập</a>
            <a class="button button-small" routerLink="/register">
              Bắt đầu học
              <span aria-hidden="true">↗</span>
            </a>
          }
        </div>
      </div>
    </header>
    @if (auth.notice()) {
      <div class="notice container" role="alert">
        <span>{{ auth.notice() }}</span>
        <button class="text-button" (click)="auth.notice.set('')" aria-label="Đóng thông báo">
          ×
        </button>
      </div>
    }
    <main id="main"><router-outlet /></main>
    <footer class="site-footer">
      <div class="container footer-top">
        <div>
          <a class="brand" routerLink="/">
            khoa
            <span class="brand-dot">.</span>
          </a>
          <p>Học điều mới. Mở lối riêng.</p>
        </div>
        <div class="footer-links">
          <a routerLink="/">Khám phá khóa học</a>
          <a routerLink="/my-learning">Góc học tập</a>
          <a routerLink="/register">Tham gia cùng Khoa</a>
        </div>
      </div>
      <div class="container footer-bottom">
        <span>© 2026 Khoa. Không ngừng học, không ngừng lớn.</span>
        <span>
          Được tạo nên cho những người ham học
          <span aria-hidden="true">✳</span>
        </span>
      </div>
    </footer>
    <app-course-chat />
  `,
})
export class AppComponent {
  readonly auth = inject(AuthService);
}
