import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService, errorMessage } from '../../core/api.service';
import { CourseDetail, VideoLesson } from '../../core/models';
import { CourseArtComponent } from '../../shared/course-art.component';

@Component({
  selector: 'app-course',
  imports: [CurrencyPipe, DecimalPipe, RouterLink, CourseArtComponent],
  templateUrl: './course.component.html',
})
export class CourseComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  readonly detail = signal<CourseDetail | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly preview = signal<VideoLesson | null>(null);
  readonly previewLoading = signal(false);
  readonly previewError = signal('');
  readonly mediaError = signal(false);
  private request = 0;

  ngOnInit() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      void this.load();
    });
  }

  async load(): Promise<void> {
    const request = ++this.request;
    this.loading.set(true);
    this.error.set('');
    this.preview.set(null);
    this.previewError.set('');
    this.previewLoading.set(false);
    try {
      const detail = await this.api.detail(this.route.snapshot.paramMap.get('slug') || '');
      if (request === this.request) {
        this.detail.set(detail);
      }
    } catch (error) {
      if (request === this.request) {
        this.error.set(errorMessage(error));
      }
    } finally {
      if (request === this.request) {
        this.loading.set(false);
      }
    }
  }

  async watchPreview(): Promise<void> {
    const detail = this.detail();
    if (!detail || this.previewLoading()) {
      return;
    }
    const request = this.request;
    this.previewLoading.set(true);
    this.previewError.set('');
    this.mediaError.set(false);
    try {
      const video = await this.api.preview(detail.course.slug);
      if (request === this.request) {
        this.preview.set(video);
      }
    } catch (error) {
      if (request === this.request) {
        this.previewError.set(errorMessage(error));
      }
    } finally {
      if (request === this.request) {
        this.previewLoading.set(false);
      }
    }
  }
}
