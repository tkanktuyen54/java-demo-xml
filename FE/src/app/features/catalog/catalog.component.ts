import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api.service';
import { Course } from '../../core/models';
import { CourseCardComponent } from '../../shared/course-card.component';

@Component({
  selector: 'app-catalog',
  imports: [FormsModule, RouterLink, CourseCardComponent],
  templateUrl: './catalog.component.html',
})
export class CatalogComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly courses = signal<Course[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly query = signal('');
  readonly category = signal('Tất cả');
  readonly sort = signal('recommended');
  readonly categories = ['Tất cả', 'Lập trình', 'Thiết kế', 'Marketing'];
  readonly filtered = computed(() => {
    const q = this.query().trim().toLocaleLowerCase('vi');
    const items = this.courses().filter(
      (course) =>
        (this.category() === 'Tất cả' || course.category === this.category()) &&
        (!q ||
          `${course.title} ${course.instructor} ${course.category}`
            .toLocaleLowerCase('vi')
            .includes(q)),
    );
    switch (this.sort()) {
      case 'price-asc':
        return items.sort((a, b) => a.price - b.price);
      case 'price-desc':
        return items.sort((a, b) => b.price - a.price);
      case 'rating':
        return items.sort((a, b) => b.rating - a.rating);
      default:
        return items;
    }
  });

  ngOnInit() {
    void this.load();
  }
  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      this.courses.set(await this.api.courses());
    } catch (error) {
      this.error.set(errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }
  reset() {
    this.query.set('');
    this.category.set('Tất cả');
    this.sort.set('recommended');
  }
}
