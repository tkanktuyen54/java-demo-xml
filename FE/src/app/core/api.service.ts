import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Course, CourseDetail, Enrollment, VideoLesson } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  courses() {
    return firstValueFrom(this.http.get<Course[]>('/api/courses'));
  }
  detail(slug: string) {
    return firstValueFrom(this.http.get<CourseDetail>(`/api/courses/${encodeURIComponent(slug)}`));
  }
  preview(slug: string) {
    return firstValueFrom(
      this.http.get<VideoLesson>(`/api/courses/${encodeURIComponent(slug)}/preview`),
    );
  }
  config() {
    return firstValueFrom(this.http.get<{ demoCheckoutEnabled: boolean }>('/api/config'));
  }
  enrollments() {
    return firstValueFrom(this.http.get<Enrollment[]>('/api/enrollments'));
  }
  checkout(slug: string) {
    return firstValueFrom(
      this.http.post<Enrollment>(`/api/enrollments/${encodeURIComponent(slug)}/demo-checkout`, {}),
    );
  }
  video(slug: string, id: number) {
    return firstValueFrom(
      this.http.get<VideoLesson>(`/api/learning/${encodeURIComponent(slug)}/lessons/${id}`),
    );
  }
  progress(slug: string) {
    return firstValueFrom(
      this.http.get<number[]>(`/api/learning/${encodeURIComponent(slug)}/progress`),
    );
  }
  complete(slug: string, id: number) {
    return firstValueFrom(
      this.http.post<number[]>(
        `/api/learning/${encodeURIComponent(slug)}/lessons/${id}/complete`,
        {},
      ),
    );
  }
}

export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0 || error.status === 502 || error.status === 504) {
      return 'Chưa kết nối được máy chủ. Vui lòng kiểm tra backend và thử lại.';
    }
    const message: unknown = error.error?.message;
    if (typeof message === 'string') {
      return message;
    }
    if (error.status === 401) {
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
    }
    if (error.status === 404) {
      return 'Không tìm thấy nội dung bạn yêu cầu.';
    }
    if (error.status === 403) {
      return 'Bạn chưa có quyền truy cập hoặc phiên bảo mật đã hết hạn. Vui lòng tải lại trang.';
    }
  }
  return 'Có lỗi xảy ra. Vui lòng thử lại sau.';
}
