import { bootstrapApplication } from '@angular/platform-browser';
import { inject, provideAppInitializer, provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { AuthService } from './app/core/auth.service';
import { registerLocaleData } from '@angular/common';
import localeVi from '@angular/common/locales/vi';

registerLocaleData(localeVi);

bootstrapApplication(AppComponent, {
  providers: [
    provideZonelessChangeDetection(),
    provideHttpClient(),
    provideRouter(
      routes,
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' }),
    ),
    provideAppInitializer(() => inject(AuthService).restore()),
  ],
}).catch((error) => console.error('Không thể khởi động ứng dụng', error));
