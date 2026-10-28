import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CourseDetail } from '../../core/models';
import { CourseArtComponent } from '../../shared/course-art.component';

@Component({
  selector: 'app-checkout',
  imports: [RouterLink, CurrencyPipe, CourseArtComponent],
  template: `
    <section class="container page-space checkout-page">
      <a class="back-link" [routerLink]="['/courses', slug]">← Quay lại khóa học</a>
      @if (loading()) {
        <div class="loading-state" role="status">Đang chuẩn bị khóa học...</div>
      } @else if (detail(); as data) {
        <div class="section-heading">
          <div>
            <div class="eyebrow">SẴN SÀNG CHO BƯỚC TIẾP THEO</div>
            <h1>Một khoản đầu tư cho bạn.</h1>
            <p>Kiểm tra khóa học trước khi bắt đầu hành trình.</p>
          </div>
        </div>
        <div class="checkout-grid">
          <div>
            <div class="checkout-course">
              <app-course-art [theme]="data.course.coverKey" />
              <div>
                <span class="eyebrow">{{ data.course.category }}</span>
                <h2>{{ data.course.title }}</h2>
                <p>{{ data.course.instructor }}</p>
                <small>{{ data.course.lessonCount }} bài học · {{ data.course.hours }} giờ</small>
              </div>
            </div>
            <div class="checkout-account">
              <h3>Thông tin học viên</h3>
              <p>{{ auth.user()?.name }}</p>
              <span>{{ auth.user()?.email }}</span>
            </div>
            <div class="demo-info">
              <span aria-hidden="true">ⓘ</span>
              <div>
                <strong>Chế độ thanh toán demo</strong>
                <p>
                  Bấm xác nhận sẽ tạo đơn hàng mẫu và mở khóa học. Bạn không bị trừ tiền. Website
                  chưa kết nối cổng thanh toán thật.
                </p>
              </div>
            </div>
          </div>
          <aside class="order-summary">
            <h2>Thông tin đăng ký</h2>
            <div>
              <span>Giá khóa học</span>
              <del>
                {{ data.course.originalPrice | currency: 'VND' : 'symbol' : '1.0-0' : 'vi' }}
              </del>
            </div>
            <div>
              <span>Ưu đãi khóa học</span>
              <span class="saving">
                −{{
                  data.course.originalPrice - data.course.price
                    | currency: 'VND' : 'symbol' : '1.0-0' : 'vi'
                }}
              </span>
            </div>
            <div class="order-total">
              <strong>Tổng cộng</strong>
              <strong>{{ data.course.price | currency: 'VND' : 'symbol' : '1.0-0' : 'vi' }}</strong>
            </div>
            <button
              class="button full-width"
              [disabled]="pending() || !demoEnabled()"
              (click)="checkout()"
            >
              {{ pending() ? 'Đang đăng ký...' : 'Xác nhận đăng ký demo' }}
              <span>↗</span>
            </button>
            <p class="purchase-note">Không thu tiền · Bắt đầu học ngay sau xác nhận</p>
            @if (!demoEnabled()) {
              <p class="form-error">
                Thanh toán demo hiện đang tắt. Vui lòng liên hệ quản trị viên.
              </p>
            }
          </aside>
        </div>
      }
      @if (error()) {
        <div class="form-error" role="alert">
          {{ error() }}
          @if (!detail()) {
            <button class="text-button" (click)="load()">Thử lại</button>
          }
        </div>
      }
    </section>
  `,
})
export class CheckoutComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly slug = inject(ActivatedRoute).snapshot.paramMap.get('slug') || '';
  readonly detail = signal<CourseDetail | null>(null);
  readonly loading = signal(true);
  readonly pending = signal(false);
  readonly demoEnabled = signal(false);
  readonly error = signal('');

  ngOnInit() {
    void this.load();
  }
  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const [detail, config] = await Promise.all([this.api.detail(this.slug), this.api.config()]);
      if (detail.enrolled) {
        await this.router.navigate(['/learn', this.slug]);
        return;
      }
      this.detail.set(detail);
      this.demoEnabled.set(config.demoCheckoutEnabled);
    } catch (error) {
      this.error.set(errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
  async checkout(): Promise<void> {
    if (this.pending() || !this.demoEnabled()) {
      return;
    }
    this.pending.set(true);
    this.error.set('');
    try {
      await this.auth.csrf();
      await this.api.checkout(this.slug);
      await this.router.navigate(['/learn', this.slug]);
    } catch (error) {
      this.error.set(errorMessage(error));
    } finally {
      this.pending.set(false);
    }
  }
}
