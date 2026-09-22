import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { App } from './app/app';
import { routes } from './app/routes';
import { authInterceptor } from './app/core/auth';

bootstrapApplication(App, {providers: [provideHttpClient(withInterceptors([authInterceptor])),
  provideRouter(routes, withInMemoryScrolling({scrollPositionRestoration:'top'}))]}).catch(console.error);
