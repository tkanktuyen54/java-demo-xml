import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { CourseDetail, Lesson, VideoLesson } from '../../core/models';

@Component({
  selector: 'app-learning',
  imports: [RouterLink],
  templateUrl: './learning.component.html',
})
export class LearningComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly slug = this.route.snapshot.paramMap.get('slug') || '';
  readonly detail = signal<CourseDetail | null>(null);
  readonly video = signal<VideoLesson | null>(null);
  readonly completed = signal<number[]>([]);
  readonly loading = signal(true);
  readonly videoLoading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly videoError = signal('');
  readonly mediaError = signal(false);
  readonly selectedId = signal(0);
  readonly percent = computed(() =>
    this.detail()?.lessons.length
      ? Math.round((this.completed().length / this.detail()!.lessons.length) * 100)
      : 0,
  );
  readonly selected = computed(() =>
    this.detail()?.lessons.find((lesson) => lesson.id === this.selectedId()),
  );
  readonly next = computed(() => {
    const lessons = this.detail()?.lessons || [];
    return lessons[lessons.findIndex((lesson) => lesson.id === this.selectedId()) + 1];
  });
  private videoRequest = 0;

  ngOnInit() {
    void this.load();
  }
  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      const detail = await this.api.detail(this.slug);
      if (!detail.enrolled) {
        await this.router.navigate(['/courses', this.slug]);
        return;
      }
      const completed = await this.api.progress(this.slug);
      this.detail.set(detail);
      this.completed.set(completed);
      const requested = Number(this.route.snapshot.queryParamMap.get('lesson'));
      const first =
        detail.lessons.find((lesson) => lesson.id === requested) ||
        detail.lessons.find((lesson) => !completed.includes(lesson.id)) ||
        detail.lessons[0];
      if (first) {
        await this.select(first);
      }
    } catch (error) {
      this.error.set(errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  async select(lesson: Lesson): Promise<void> {
    const request = ++this.videoRequest;
    this.selectedId.set(lesson.id);
    this.video.set(null);
    this.videoError.set('');
    this.mediaError.set(false);
    this.videoLoading.set(true);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { lesson: lesson.id },
      replaceUrl: true,
    });
    try {
      const video = await this.api.video(this.slug, lesson.id);
      if (request === this.videoRequest) {
        this.video.set(video);
      }
    } catch (error) {
      if (request === this.videoRequest) {
        this.videoError.set(errorMessage(error));
      }
    } finally {
      if (request === this.videoRequest) {
        this.videoLoading.set(false);
      }
    }
  }

  async complete(): Promise<void> {
    const video = this.video();
    if (!video || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.videoError.set('');
    try {
      await this.auth.csrf();
      this.completed.set(await this.api.complete(this.slug, video.id));
    } catch (error) {
      this.videoError.set(errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  retryVideo() {
    const lesson = this.selected();
    if (lesson) {
      void this.select(lesson);
    }
  }
}
