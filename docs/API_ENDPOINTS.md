# CareFinder API endpoints

Base URL: `http://localhost:8080/api/v1`

Protected routes use `Authorization: Bearer <accessToken>`.

| Feature | Method | Endpoint | Access |
|---|---|---|---|
| Register | POST | `/auth/register` | Public |
| Login | POST | `/auth/login` | Public |
| Refresh access token | POST | `/auth/refresh` | Public |
| Logout | POST | `/auth/logout` | Public |
| Start password reset | POST | `/auth/forgot-password` | Public |
| Finish password reset | POST | `/auth/reset-password` | Public |
| Search and filter hospitals | GET | `/hospitals` | Public |
| Hospital profile | GET | `/hospitals/{id}` | Public; records history when bearer token is sent |
| Compare 2–3 hospitals | GET | `/hospitals/compare?ids=1,2,3` | Public |
| Comparison PDF | GET | `/hospitals/compare/report.pdf?ids=1,2,3` | Public |
| Insurance providers | GET | `/insurers` | Public |
| Directory statistics | GET | `/meta/stats` | Public |
| Chatbot answer and cards | POST | `/chatbot/messages` | Public; saves history when bearer token is sent |
| My profile | GET/PUT | `/users/me` | User |
| Change password | PATCH | `/users/me/password` | User |
| Favorites | GET/DELETE | `/me/favorites` | User |
| Save/remove favorite | POST/DELETE | `/me/favorites/{hospitalId}` | User |
| Recently viewed | GET/DELETE | `/me/recently-viewed` | User |
| Record/remove recent | POST/DELETE | `/me/recently-viewed/{hospitalId}` | User |
| Chat history | GET/DELETE | `/me/chat-history` | User |
| Manage hospitals | POST/PUT/DELETE | `/admin/hospitals/**` | Admin |
| Update assigned hospital | PUT | `/staff/hospitals/{id}` | Hospital staff/Admin |
| Manage insurers | GET/POST/PUT/DELETE | `/admin/insurers/**` | Admin |
| Manage users | GET/PATCH | `/admin/users/**` | Admin |
| Analytics | GET | `/admin/analytics/summary` | Admin |
| Audit log | GET | `/admin/audit-logs` | Admin |

## Hospital search parameters

`query`, `location`, `insurance`, `emergency`, `open24x7`, `minimumRating`,
`latitude`, `longitude`, `radiusKm`, `sort`, `page`, `size`.

`sort` supports `recommended`, `distance`, `rating`, and `name`. GPS radius is
limited to 500 km and page size to 50.
