# THM Web-Technologies Mail Project

University project for a shared mail-support application. The current stack consists of:

- Kotlin, Spring Boot, Spring Security and Spring Data JPA
- Angular, PrimeNG and Tailwind CSS
- Keycloak with OpenID Connect Authorization Code Flow and PKCE
- PostgreSQL and SeaweedFS (S3-compatible attachment storage)
- Docker Compose and Caddy as the local web entry point

The repository is a Gradle monorepo. The detailed implementation plan and the distinction between completed and pending assignment items are documented in `PROJEKTPLAN.md`. C4 documentation is intentionally handled separately.

## Prerequisites

- Docker Desktop or Docker Engine with Docker Compose
- JDK 25 (the backend's configured Gradle toolchain)

Node.js, npm, Gradle, PostgreSQL and Keycloak do not have to be installed globally. The Gradle build downloads the configured Node/npm versions and Docker Compose runs the infrastructure.

## Configure

The application has local development defaults and starts without a `.env` file. To configure SMTP/IMAP or override defaults:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Never commit `.env` or real THM credentials.

## Build, verify and start with one command

Windows:

```powershell
.\gradlew.bat composeUp
```

Linux/macOS:

```bash
./gradlew composeUp
```

This one command:

1. installs frontend dependencies reproducibly with `npm ci`;
2. runs the backend and frontend tests;
3. creates the Angular production build;
4. builds the backend and frontend container images;
5. starts PostgreSQL, SeaweedFS, Keycloak, backend and frontend with Docker Compose.

Open the application at <http://localhost>. Stop the stack with:

```powershell
.\gradlew.bat composeDown
```

## Keycloak login

Authentication is performed exclusively by the `mail-system` Keycloak realm. The frontend is a public OIDC client named `mail-system-frontend` and uses Authorization Code Flow with PKCE (`S256`). The backend accepts signed access tokens from this realm as a stateless OAuth2 resource server.

The imported development users all use the password `demo-password`:

| Name | Login |
| --- | --- |
| Ameline Allanson | `aallanson@example.com` |
| Sanson Vardey | `svardey1@example.com` |
| Jami Poe | `jpoe@example.uk` |
| Trent Ianno | `tianno3@example.com` |
| Alikee Raisbeck | `araisbeck4@example.com` |

The users are initialized both in Keycloak and as passwordless local business profiles with matching stable OIDC subjects. Local login, local registration, application passwords and application-issued JWTs are intentionally not present.

For this local university demonstrator Keycloak runs with `start-dev` behind Caddy over HTTP. TLS termination at the self-signed WAF is a separate assignment phase.

## Local development

The full Compose stack should be running so that PostgreSQL, SeaweedFS, Keycloak and the backend are available. To use Angular's development server:

```bash
cd frontend
npm ci
npm start
```

The development proxy forwards `/api`, `/auth` and the OpenAPI routes to the Compose entry point. Open <http://localhost:4200>.

Useful verification commands:

```powershell
.\gradlew.bat :backend:test :frontend:test :frontend:build
docker compose config
docker compose ps
```

The Angular production build currently reports its pre-existing initial-bundle budget warning; the build still succeeds.

## OpenAPI

The backend generates its OpenAPI 3.1 description from the running code and annotations. Every operation has a stable operation ID, a summary, a description, success and error responses, and a functional tag. Request/response DTOs, required properties and the common JSON error body are described as schemas. The global authentication requirement uses the Keycloak OpenID Connect discovery URL.

- Swagger UI: <http://localhost/swagger-ui/index.html>
- JSON: <http://localhost/v3/api-docs>
- YAML: <http://localhost/v3/api-docs.yaml>

The OpenAPI endpoints are public; application API endpoints under `/api/**` require a valid Keycloak access token.

## Support mail, SMTP and IMAP

The seeded external support request can be opened and answered by every registered Keycloak user. Creating a reply draft assigns or reuses a random `TICKET-XXXXXXXX` number, prefixes the reply subject and automatically addresses the external sender. The logged-in user remains the internal author; SMTP always uses the shared THM address configured as `MAIL_FROM_ADDRESS` for the visible `From` header.

The IMAP poller searches only messages without the `SEEN` flag. It stores the message and its attachments in PostgreSQL and SeaweedFS and marks the source message as `SEEN` only after the complete import succeeds. The external `Message-ID` provides an additional deduplication guard. Failed imports remain unseen and are retried.

HTTP uploads and IMAP attachments share configurable file and total-size limits. File names and media types are normalized before storage, internal S3 object keys are not exposed through the REST DTO, and authorized downloads use `Content-Disposition: attachment` with `nosniff`. JPEG, PNG, GIF and WebP files can additionally be requested through the authenticated inline-preview mode; active formats such as HTML and SVG remain downloads.

SMTP and IMAP are optional for the local demo and are disabled when their hosts or credentials are empty. Configure real THM credentials only in the local `.env`; all supported values are listed in `.env.example`.

## Repository layout

- `backend/` — Spring Boot application and tests
- `frontend/` — Angular application, tests and self-contained container build
- `keycloak/` — reproducible development realm, OIDC client and demo users
- `docker-compose.yml` — complete local application stack
- `.env.example` — non-secret configuration reference
- `PROJEKTPLAN.md` — verified decisions, status and remaining assignment phases

The local `.codex/` and `folien/` directories are deliberately left untouched and are not added to `.gitignore`.
