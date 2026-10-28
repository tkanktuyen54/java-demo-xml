import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  return inject(AuthService).user()
    ? true
    : inject(Router).createUrlTree(['/login'], { queryParams: { next: state.url } });
};
