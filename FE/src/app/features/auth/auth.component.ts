import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/api.service';

@Component({
  selector: 'app-auth',
  imports: [FormsModule, RouterLink],
  templateUrl: './auth.component.html',
})
export class AuthComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly register = this.route.snapshot.routeConfig?.path === 'register';
  readonly pending = signal(false);
  readonly error = signal('');
  readonly showPassword = signal(false);
  name = '';
  email = '';
  password = '';
  readonly next = this.safeNext(this.route.snapshot.queryParamMap.get('next'));

  ngOnInit() {
    if (this.auth.user()) {
      void this.router.navigateByUrl(this.next);
    }
  }

  private safeNext(value: string | null) {
    return value && /^\/(checkout|learn|courses)\/[a-z0-9-]+$/.test(value) ? value : '/my-learning';
  }

  async submit(form: NgForm): Promise<void> {
    if (this.pending()) {
      return;
    }
    if (form.invalid) {
      this.error.set('Vui lòng kiểm tra họ tên, email và mật khẩu trước khi tiếp tục.');
      return;
    }
    this.pending.set(true);
    this.error.set('');
    try {
      await this.auth.submit(this.register ? 'register' : 'login', {
        name: this.name.trim(),
        email: this.email.trim(),
        password: this.password,
      });
      await this.router.navigateByUrl(this.next);
    } catch (error) {
      this.error.set(errorMessage(error));
    } finally {
      this.pending.set(false);
    }
  }
}
