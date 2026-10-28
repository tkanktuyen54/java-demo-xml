import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <section class="container empty-state page-space">
      <span class="empty-icon">404</span>
      <h1>Trang này chưa có trong lộ trình.</h1>
      <p>Cùng quay lại khám phá một điều mới nhé.</p>
      <a class="button" routerLink="/">
        Về trang chủ
        <span>↗</span>
      </a>
    </section>
  `,
})
export class NotFoundComponent {}
