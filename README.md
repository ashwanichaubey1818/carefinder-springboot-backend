## Angular Frontend Integration

The Angular 22 frontend is fully integrated with this Spring Boot backend.

## Live deployment

- Frontend: https://carefinder-angular-frontend.ashwanichaubey1818.workers.dev
- Backend API: https://carefinder-springboot-backend.onrender.com
- Health check: https://carefinder-springboot-backend.onrender.com/actuator/health
- Swagger UI: https://carefinder-springboot-backend.onrender.com/swagger-ui.html
- Production database: TiDB Cloud (MySQL compatible)
- Transactional email: Brevo API

The production backend runs on Render with the `prod` Spring profile.
Secrets such as database credentials, JWT signing key and Brevo API key
are supplied through Render environment variables and are not committed
to GitHub.

### Frontend repository

[CareFinder Angular Frontend](https://github.com/ashwanichaubey1818/carefinder-angular-frontend)

### Local development URLs

- Angular frontend: `http://localhost:4200`
- Spring Boot backend: `http://localhost:8080`
- REST API base URL: `http://localhost:8080/api/v1`

### Integrated features

- Registration and login
- JWT access and refresh tokens
- Automatic token refresh
- User profile management
- Password reset through Brevo email
- Hospital search and filtering
- Favorites
- Recently viewed hospitals
- Hospital comparison
- Chatbot messages and history
- Admin and hospital staff authorization

### Additional documentation

- [Angular Integration](docs/ANGULAR_INTEGRATION.md)
- [API Endpoints](docs/API_ENDPOINTS.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Feature Mapping](docs/FEATURE_MAPPING.md)
  [![Backend CI](https://github.com/ashwanichaubey1818/carefinder-springboot-backend/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/ashwanichaubey1818/carefinder-springboot-backend/actions/workflows/backend-ci.yml)
