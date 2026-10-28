import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Enrollment } from '../../core/models';
import { CourseArtComponent } from '../../shared/course-art.component';

@Component({
  selector: 'app-my-learning',
  imports: [RouterLink, CourseArtComponent],
  template: `
    <section class="container page-space my-learning">
      <div class="learning-welcome">
        <div>
          <div class="eyebrow">GÓC HỌC TẬP CỦA BẠN</div>
          <h1>
            Chào {{ auth.user()?.name }}
            <span class="welcome-star" aria-hidden="true">✳</span>
          </h1>
          <p>Mỗi bài học hôm nay là một bước tiến cho ngày mai.</p>
        </div>
        <a class="button button-outline" routerLink="/">
          Khám phá thêm
          <span>↗</span>
        </a>
      </div>
      <div class="learning-stats">
        <div>
          <strong>{{ items().length.toString().padStart(2, '0') }}</strong>
          <span>Khóa học của bạn</span>
        </div>
        <div>
          <strong>{{ completedCount().toString().padStart(2, '0') }}</strong>
          <span>Khóa học hoàn thành</span>
        </div>
        <div>
          <strong>{{ lessonsDone().toString().padStart(2, '0') }}</strong>
          <span>Bài học đã hoàn thành</span>
        </div>
      </div>
      <h2>Tiếp tục hành trình</h2>
      @if (loading()) {
        <div class="loading-state" role="status">Đang tải góc học tập...</div>
      } @else if (error()) {
        <div class="empty-state" role="alert">
          <p>{{ error() }}</p>
          <button class="button" (click)="load()">Thử lại</button>
        </div>
      } @else {
        <div class="course-grid">
          @for (item of items(); track item.orderId) {
            <article class="course-card enrolled-card">
              <a [routerLink]="['/learn', item.slug]" [attr.aria-label]="'Học ' + item.title">
                <app-course-art [theme]="item.coverKey" />
              </a>
              <div class="course-card-body">
                <span class="eyebrow">
                  {{
                    item.completedLessons === item.lessonCount
                      ? 'ĐÃ HOÀN THÀNH'
                      : 'ĐANG TRÊN HÀNH TRÌNH'
                  }}
                </span>
                <h3>
                  <a [routerLink]="['/learn', item.slug]">{{ item.title }}</a>
                </h3>
                <p class="instructor">{{ item.instructor }}</p>
                <div class="progress-label">
                  <span>{{ item.completedLessons }}/{{ item.lessonCount }} bài hoàn thành</span>
                  <strong>{{ percent(item) }}%</strong>
                </div>
                <progress
                  [value]="item.completedLessons"
                  [max]="item.lessonCount"
                  [attr.aria-label]="'Tiến độ ' + item.title"
                ></progress>
                <a class="button button-outline full-width" [routerLink]="['/learn', item.slug]">
                  {{
                    item.completedLessons === item.lessonCount ? 'Ôn tập khóa học' : 'Tiếp tục học'
                  }}
                  <span>↗</span>
                </a>
              </div>
            </article>
          } @empty {
            <div class="empty-state no-results">
              <span class="empty-icon">✳</span>
              <h3>Hành trình của bạn đang chờ được viết.</h3>
              <p>Chọn khóa học đầu tiên và bắt đầu từ một điều bạn tò mò.</p>
              <a class="button" routerLink="/">
                Tìm khóa học của bạn
                <span>↗</span>
              </a>
            </div>
          }
        </div>
      }
    </section>
  `,
})
export class MyLearningComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  readonly items = signal<Enrollment[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly completedCount = computed(
    () =>
      this.items().filter(
        (item) => item.lessonCount > 0 && item.completedLessons === item.lessonCount,
      ).length,
  );
  readonly lessonsDone = computed(() =>
    this.items().reduce((sum, item) => sum + item.completedLessons, 0),
  );
  percent(item: Enrollment) {
    return item.lessonCount ? Math.round((item.completedLessons / item.lessonCount) * 100) : 0;
  }
  ngOnInit() {
    void this.load();
  }
  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.items.set(await this.api.enrollments());
    } catch (error) {
      this.error.set(errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
}
