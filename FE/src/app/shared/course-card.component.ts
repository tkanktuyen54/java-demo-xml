import { Component, input } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Course } from '../core/models';
import { CourseArtComponent } from './course-art.component';

@Component({
  selector: 'app-course-card',
  imports: [RouterLink, CurrencyPipe, DecimalPipe, CourseArtComponent],
  template: `
    <article class="course-card">
      <a
        class="course-cover-link"
        [routerLink]="['/courses', course().slug]"
        [attr.aria-label]="'Xem khóa học ' + course().title"
      >
        <app-course-art [theme]="course().coverKey" />
        @if (course().badge) {
          <span class="course-badge">{{ course().badge }}</span>
        }
      </a>
      <div class="course-card-body">
        <div class="course-eyebrow">
          <span>{{ course().category }}</span>
          <span class="level">
            <i></i>
            {{ course().level }}
          </span>
        </div>
        <h3>
          <a [routerLink]="['/courses', course().slug]">{{ course().title }}</a>
        </h3>
        <p class="instructor">
          <span class="instructor-avatar">{{ course().instructor.charAt(0) }}</span>
          {{ course().instructor }}
        </p>
        <div class="course-meta">
          <span>
            <span class="star">★</span>
            <strong>{{ course().rating | number: '1.1-1' }}</strong>
            <span class="muted">({{ course().studentCount | number }} học viên)</span>
          </span>
          <span>{{ course().lessonCount }} bài · {{ course().hours }} giờ</span>
        </div>
        <div class="course-card-bottom">
          <div class="prices">
            <strong>{{ course().price | currency: 'VND' : 'symbol' : '1.0-0' : 'vi' }}</strong>
            <del>{{ course().originalPrice | currency: 'VND' : 'symbol' : '1.0-0' : 'vi' }}</del>
          </div>
          <a
            class="circle-link"
            [routerLink]="['/courses', course().slug]"
            [attr.aria-label]="'Khám phá ' + course().title"
          >
            ↗
          </a>
        </div>
      </div>
    </article>
  `,
})
export class CourseCardComponent {
  readonly course = input.required<Course>();
}
