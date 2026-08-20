# THM Web-Technologies Mail Project

Full-stack web application for managing mails with:

- **Backend:** Kotlin + Spring Boot, Spring Security, Spring Data JPA, Validation, JWT
- **Frontend:** Angular + PrimeNG + TailwindCSS
- **Database:** PostgreSQL via Docker Compose
- **Attachment storage:** SeaweedFS with S3-compatible API

## Repository Layout

- `backend/` - Spring Boot Kotlin service
- `frontend/` - Angular application
- `docker-compose.yml` - local application stack
- `seaweedfs-s3.json` - local SeaweedFS S3 credentials/configuration

## Local Setup

Create a `.env` file in the repository root, next to `docker-compose.yml`.

Required local values:

```env
# Database
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=mail_project
DB_DDL_AUTO=update

# App / JWT
APP_NAME=mail-project
APP_SECRET=change_me_super_secret
APP_JWT_EXPIRES=3600
APP_JWT_SECRET=change_me_jwt_secret

# SeaweedFS / S3 storage
STORAGE_S3_ENDPOINT=http://seaweedfs:8333
STORAGE_S3_BUCKET=mail-attachments
STORAGE_S3_REGION=us-east-1
STORAGE_S3_ACCESS_KEY=mail-system
STORAGE_S3_SECRET_KEY=mail-system-secret

# SMTP outbound mail
SPRING_MAIL_HOST=mailgate.thm.de
SPRING_MAIL_PORT=465
SPRING_MAIL_USERNAME=your-thm-email@student.thm.de
SPRING_MAIL_PASSWORD=your-thm-password
MAIL_FROM_ADDRESS=your-thm-email@student.thm.de
MAIL_REPLY_TO_ADDRESS=your-thm-email@student.thm.de

# IMAP inbound mail
MAIL_IMAP_HOST=mailgate.thm.de
MAIL_IMAP_PORT=993
MAIL_IMAP_USERNAME=your-thm-email@student.thm.de
MAIL_IMAP_PASSWORD=your-thm-password
MAIL_IMAP_FOLDER=INBOX
MAIL_IMAP_POLL_INTERVAL_MS=5000
MAIL_IMAP_RECENT_WINDOW_SIZE=50
```

Do not commit `.env`. It is ignored by Git.

## Start

Use Docker Compose as the primary local setup:

```bash
docker compose up -d --build
```

Open the application at:

```text
http://localhost
```

The backend and database are not exposed directly to the host. Caddy in the frontend container proxies `/api/*` to the backend.

## SeaweedFS

Attachments are stored in SeaweedFS through its S3-compatible API.

In Docker Compose:

- SeaweedFS runs as service `seaweedfs`
- the backend talks to `http://seaweedfs:8333`
- the default bucket is `mail-attachments`
- credentials are defined in `seaweedfs-s3.json` and mirrored through `.env`

Attachment metadata remains in PostgreSQL. The binary file content is stored in SeaweedFS under keys like:

```text
attachments/<uuid>.<extension>
```

Downloads go through the backend endpoint:

```text
GET /api/attachments/{attachmentId}
```

The endpoint checks whether the authenticated user is the sender or has a mail record for the mail before returning the file.

## Mail Import

### IMAP

The backend polls the configured IMAP inbox.

Behavior:

1. The first successful sync imports all messages from the configured folder.
2. Later syncs inspect the most recent messages from the configured folder.
3. The IMAP folder is opened read-only, so read/unread state is not changed by the import.
4. Messages are deduplicated by `Message-ID`.
5. Imported external mails are assigned to all internal app users.
6. External senders are stored as contacts with `externalContact=true`.
7. Attachments are uploaded to SeaweedFS during import.

`MAIL_IMAP_POLL_INTERVAL_MS` controls how often the backend checks the mailbox. `MAIL_IMAP_RECENT_WINDOW_SIZE` controls how many recent messages are checked after the initial import. The backend deduplicates by `Message-ID`, so already imported messages are skipped. This keeps replies importable even if they were already opened on another device.

If `MAIL_IMAP_HOST`, `MAIL_IMAP_USERNAME`, or `MAIL_IMAP_PASSWORD` is empty, IMAP polling is disabled.

### SMTP

SMTP is used for outbound mails. Configure `SPRING_MAIL_*`, `MAIL_FROM_ADDRESS`, and optionally `MAIL_REPLY_TO_ADDRESS` in `.env`.

`MAIL_REPLY_TO_ADDRESS` must be a complete email address and should point to the mailbox that IMAP imports, for example `jfdr91@mailserv.fh-giessen.de`. If it is omitted, the backend does not set a `Reply-To` header and replies go to the configured `From` address.

## Ticket Tracking

Imported external mails receive a tracking code in the subject:

```text
[TICKET-XXXXXXXX]
```

Existing ticket prefixes are reused. This keeps replies grouped by the same tracking code.

## Shared Support Inbox

Imported external mails are visible to all internal app users. External contacts are kept out of the visible recipient lists in the mail detail view.

## Reset Local Data

To reset PostgreSQL and SeaweedFS and trigger a fresh initial IMAP import:

```bash
docker compose down -v
docker compose up -d --build
```

This deletes local database and attachment storage volumes.

## Seed Users

Development users from `backend/src/main/resources/data.json`:

| # | First name | Last name | Email | Password |
|---:|------------|-----------|-------|----------|
| 1 | Ameline | Allanson | `aallanson@example.com` | `123456` |
| 2 | Sanson | Vardey | `svardey1@example.com` | `123456` |
| 3 | Jami | Poe | `jpoe@example.uk` | `123456` |
| 4 | Trent | Ianno | `tianno3@example.com` | `123456` |
| 5 | Alikee | Raisbeck | `araisbeck4@example.com` | `123456` |

## Build And Checks

Backend:

```bash
./gradlew.bat :backend:test
```

Frontend:

```bash
cd frontend
npm run build
```

The frontend currently emits a known Angular bundle budget warning.

## OpenAPI / Swagger

Die API-Dokumentation ist im Backend ohne Login erreichbar:

- `GET /v3/api-docs`
- `GET /v3/api-docs.yaml`
- `GET /swagger-ui/index.html`

Die aus SpringDoc generierte OpenAPI-Datei liegt zusaetzlich als [`swagger.yml`](swagger.yml) und, passend zur PDF/IntelliJ-Konvention, als [`swagger.yaml`](swagger.yaml) im Repository.
