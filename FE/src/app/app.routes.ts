import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/catalog/catalog.component').then((m) => m.CatalogComponent),
    title: 'Khoa. — Học điều mới, mở lối riêng',
  },
  {
    path: 'courses/:slug',
    loadComponent: () =>
      import('./features/course/course.component').then((m) => m.CourseComponent),
    title: 'Khám phá khóa học · Khoa.',
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/auth.component').then((m) => m.AuthComponent),
    title: 'Đăng nhập · Khoa.',
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/auth.component').then((m) => m.AuthComponent),
    title: 'Tạo tài khoản · Khoa.',
  },
  {
    path: 'my-learning',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/my-learning/my-learning.component').then((m) => m.MyLearningComponent),
    title: 'Góc học tập · Khoa.',
  },
  {
    path: 'checkout/:slug',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/checkout/checkout.component').then((m) => m.CheckoutComponent),
    title: 'Đăng ký khóa học · Khoa.',
  },
  {
    path: 'learn/:slug',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/learning/learning.component').then((m) => m.LearningComponent),
    title: 'Không gian học tập · Khoa.',
  },
  {
    path: '**',
    loadComponent: () => import('./shared/not-found.component').then((m) => m.NotFoundComponent),
    title: 'Không tìm thấy trang · Khoa.',
  },
];
