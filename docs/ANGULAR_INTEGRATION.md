# Angular 22 integration

## 1. Add the API URL

Create `src/environments/environment.ts` in the Angular project:

```ts
export const environment = {
  apiUrl: 'http://localhost:8080/api/v1'
};
```

## 2. Enable HttpClient and attach JWT

In `app.config.ts` add `provideHttpClient(withInterceptors([authInterceptor]))`.

```ts
import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = localStorage.getItem('carefinder_access_token');

  if (!token) {
    return next(request);
  }

  return next(request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  }));
};
```

Do not put a database password or JWT secret in Angular. Those values belong
only in backend environment variables.

## 3. Hospital directory request

```ts
interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

searchHospitals(): void {
  const params = {
    location: this.location,
    insurance: this.insurance,
    emergency: this.emergencyOnly,
    open24x7: this.open24x7Only,
    minimumRating: this.rating4Plus ? 4 : '',
    sort: this.sortOption,
    page: 0,
    size: 50
  };

  this.http
    .get<PageResponse<Hospital>>(`${environment.apiUrl}/hospitals`, { params })
    .subscribe(response => {
      this.filteredHospitals = response.content;
    });
}
```

For GPS search add `latitude`, `longitude`, and `radiusKm` to the parameters.

## 4. Auth storage

After register, login or refresh, store only the returned tokens and public user
object. Never store the password.

```ts
localStorage.setItem('carefinder_access_token', response.accessToken);
localStorage.setItem('carefinder_refresh_token', response.refreshToken);
localStorage.setItem('carefinder_user', JSON.stringify(response.user));
```

On logout, call `/auth/logout` with the refresh token and then clear these three
keys. If an API returns `401`, call `/auth/refresh` once; if refresh fails, send
the user to `/login`.

## 5. Existing feature replacements

| Current browser feature | Backend endpoint |
|---|---|
| `carefinder_users` localStorage | `/auth/register`, `/auth/login`, `/users/me` |
| `carefinder-favorites` | `/me/favorites` |
| recently viewed localStorage | `/me/recently-viewed` |
| static `HOSPITALS` filtering | `/hospitals` search query |
| comparison array | `/hospitals/compare` |
| browser comparison PDF | `/hospitals/compare/report.pdf` |
| chatbot local rules | `/chatbot/messages` |

PWA cache may cache public GET hospital data, but protected profile, favorite,
history and auth responses should not be cached by the service worker.
