# KLYN

KLYN is a Spring Boot backend using MySQL, Spring Security, JWT, and OpenAPI.

## Configuration

Copy the variable names from `.env.example` into your operating-system environment, IDE run configuration, or deployment secret store. Spring Boot does not automatically load a local `.env` file.

Required variables:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
```

Optional variable:

```text
JWT_EXPIRATION=3600000
```

`JWT_SECRET` must be at least 32 characters long. Never commit real credentials or a `.env` file.

## Authentication API

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/users/me       (Authorization: Bearer <JWT>)
```

Swagger UI is available at `/swagger-ui.html` when the application is running.

## Verification

```cmd
mvnw.cmd clean test
mvnw.cmd clean package
```