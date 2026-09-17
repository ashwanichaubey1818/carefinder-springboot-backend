# CareFinder Backend

Spring Boot REST backend for the CareFinder Angular hospital-network project.
It preserves the Angular hospital-card fields and moves authentication,
profiles, favorites, recently viewed history, comparison and administration
from browser storage into a secure MySQL API.

## Included features

- 100 hospital seed profiles matching the latest Angular directory model
- Search by name/city/state, insurer, emergency, 24x7, rating and GPS radius
- Recommended, distance, rating and name sorting with pagination
- Hospital detail, 2–3 hospital comparison and downloadable PDF report
- Register, login, short-lived HS256 access token and rotating refresh token
- BCrypt password hashing, forgot/reset password and token invalidation
- User profile, password change, favorites, recent profiles and chat history
- Free database-backed Hindi/English hospital assistant; no paid AI API required
- `USER`, `HOSPITAL_STAFF` and `ADMIN` role authorization
- Hospital staff object-level access to only their assigned hospital
- Admin hospital, insurer and user management
- Analytics, audit log, request validation, safe errors, CORS and rate limits
- Flyway migration, Actuator health, Swagger/OpenAPI, Docker and H2 tests

## Requirements

- Java 21
- Maven 3.6.3 or newer
- MySQL 8.x, or Docker Desktop

The project uses Spring Boot 3.5.16. Java 21 is selected to match the planned
CareFinder development environment.

## Fastest start with Docker

1. Copy `.env.example` to `.env`.
2. Replace every password and `JWT_SECRET` in `.env`.
3. Run:

```powershell
docker compose up --build
```

The API starts at `http://localhost:8080` and MySQL at `localhost:3306`.

## Manual Windows start

### 1. Create the database

Run this in MySQL Workbench:

```sql
CREATE DATABASE carefinder
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Flyway creates all tables automatically on the first backend start.

### 2. Set environment variables

In the same PowerShell window that will run Maven:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/carefinder?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
$env:JWT_SECRET="put-a-random-secret-of-at-least-32-characters-here"
$env:CORS_ORIGINS="http://localhost:4200"
$env:ADMIN_EMAIL="admin@carefinder.local"
$env:ADMIN_PASSWORD="Use-A-New-Strong-Password-123!"
$env:STAFF_EMAIL="staff@carefinder.local"
$env:STAFF_PASSWORD="Use-A-New-Staff-Password-123!"
```

### 3. Run and test

```powershell
mvn clean test
mvn spring-boot:run
```

Open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API health: `http://localhost:8080/actuator/health`
- Hospital data: `http://localhost:8080/api/v1/hospitals`

The first start seeds exactly 100 hospital profiles and 8 insurers.

## Development accounts

When `BOOTSTRAP_ADMIN=true`, these accounts are created only if they do not
already exist:

| Role | Email | Password source |
|---|---|---|
| Admin | `ADMIN_EMAIL` | `ADMIN_PASSWORD` |
| Hospital staff | `STAFF_EMAIL` | `STAFF_PASSWORD` |

Hospital staff is assigned to hospital ID `1`. Change the staff password before
using this outside local development. Set `BOOTSTRAP_ADMIN=false` after creating
real administrator accounts.

## Login example

```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@carefinder.local",
  "password": "Use-A-New-Strong-Password-123!"
}
```

Copy `accessToken` from the response and send it on protected requests:

```http
Authorization: Bearer YOUR_ACCESS_TOKEN
```

Access tokens last 15 minutes. Refresh tokens last 14 days and are rotated on
every refresh. The database stores only a SHA-256 hash of refresh and reset
tokens.

## Password reset in development

The default `dev` profile returns `developmentResetToken` so the feature can be
tested without configuring email. The `prod` profile always hides it. A real
deployment should email the reset link through an SMTP/provider adapter.

## Angular connection

Keep Angular on `http://localhost:4200` and the backend on
`http://localhost:8080`. Follow [docs/ANGULAR_INTEGRATION.md](docs/ANGULAR_INTEGRATION.md)
to replace the current localStorage/static-data services gradually.

The complete endpoint table is in [docs/API_ENDPOINTS.md](docs/API_ENDPOINTS.md).
The module, security and ownership design is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
The complete frontend mapping is in [docs/FEATURE_MAPPING.md](docs/FEATURE_MAPPING.md).

## Main search example

```text
GET /api/v1/hospitals?location=Delhi&insurance=Star%20Health&emergency=true&open24x7=true&minimumRating=4&sort=rating&page=0&size=12
```

GPS example:

```text
GET /api/v1/hospitals?latitude=28.6139&longitude=77.2090&radiusKm=20&sort=distance
```

## Project structure

```text
src/main/java/com/carefinder/backend
├── auth       JWT sessions, refresh and reset tokens
├── user       profile and admin user management
├── hospital   directory, filters, compare and PDF
├── insurance  network provider management
├── personal   favorites and recently viewed
├── chat       free hospital assistant and history
├── admin      statistics
├── audit      administration audit trail
├── security   authorization, CORS and rate limiting
├── common     pagination and safe API errors
└── config     settings, Swagger and seed data
```

## Production checklist

- Use HTTPS through a reverse proxy/load balancer.
- Set unique database, admin and JWT secrets; never commit `.env`.
- Set `SPRING_PROFILES_ACTIVE=prod` and `EXPOSE_DEV_RESET_TOKEN=false`.
- Restrict `CORS_ORIGINS` to the deployed Angular origin.
- Replace the in-memory single-instance rate limiter with a gateway/Redis rule
  when running multiple API instances.
- Connect a real email provider for password reset delivery.
- Back up MySQL and monitor `/actuator/health` and application logs.
- Confirm every hospital, service and cashless insurance record before exposing
  it as verified real-world information.

## Reference versions

Spring Boot 3.5.x is paired with springdoc-openapi 2.8.x. PDF generation uses
Apache PDFBox 3.0.8. The application itself requires no paid API.
