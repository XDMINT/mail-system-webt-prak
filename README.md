# THM Web-Technologies Mail Project

Full‑stack web application for managing mails with:

- **Backend:** Kotlin + Spring Boot (Spring MVC, Spring Security, Spring Data JPA, Validation), JWT, PostgreSQL/H2
- **Frontend:** Angular + PrimeNG + TailwindCSS
- **Database:** PostgreSQL via Docker Compose

> Repo layout:
>
> - `backend/` — Spring Boot Kotlin service
> - `frontend/` — Angular application
> - `docker-compose.yml` — local Postgres

---

## Environment variables

### Backend

These variables are read via `${ENV_VAR:default}` (defaults shown in parentheses):

- `DB_URL` (default: `jdbc:h2:mem:testdb`)
- `DB_DRIVER` (default: `org.h2.Driver`)
- `DB_USER` (default: `dbuser`)
- `DB_PASSWORD` (default: `password`)
- `DB_DDL_AUTO` (default: `create-drop`)

- `APP_SECRET` (default: `default_secret_key`)
- `APP_NAME` (default: `MyApp`)
- `APP_JWT_EXPIRES` (default: `3600`) — seconds
- `APP_JWT_SECRET` (default: `jwt_secret_key`)

- `FILE_UPLOAD_DIR` (default: `uploads`)

### Mail (SMTP / IMAP)

**SMTP (Outbound)** — for sending mails from the support inbox to all registered users:

- `SPRING_MAIL_HOST` (default: `""` — empty, disables SMTP)
- `SPRING_MAIL_PORT` (default: `465`)
- `SPRING_MAIL_USERNAME` — your THM email or mail account
- `SPRING_MAIL_PASSWORD` — corresponding password
- `MAIL_FROM_ADDRESS` (default: uses `SPRING_MAIL_USERNAME`)

**IMAP (Inbound)** — for polling external mail inbox (e.g., THM support mailbox):

- `MAIL_IMAP_HOST` (default: `""` — empty, disables IMAP polling)
- `MAIL_IMAP_PORT` (default: `993`)
- `MAIL_IMAP_USERNAME` — your THM email or mail account
- `MAIL_IMAP_PASSWORD` — corresponding password
- `MAIL_IMAP_FOLDER` (default: `INBOX`)
- `MAIL_IMAP_POLL_INTERVAL_MS` (default: `300000` — 5 minutes)

### Docker Compose

Used to configure the Postgres container:

- `DB_USER`
- `DB_PASSWORD`
- `DB_NAME`

### Example `.env`

```env
# -------------------------
# Database (Docker Compose)
# -------------------------
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=mail_project

# -------------------------
# Backend datasource config
# -------------------------
# For local Postgres (matches docker-compose port mapping 5432:5432)
DB_URL=jdbc:postgresql://localhost:5432/mail_project
DB_DRIVER=org.postgresql.Driver

# JPA schema strategy (dev vs prod)
DB_DDL_AUTO=update

# -------------------------
# App / JWT
# -------------------------
APP_NAME=mail-project
APP_SECRET=change_me_super_secret
APP_JWT_EXPIRES=3600
APP_JWT_SECRET=change_me_jwt_secret

# -------------------------
# File uploads
# -------------------------
FILE_UPLOAD_DIR=uploads

# -------------------------
# SMTP Outbound Mail (Support Notifications)
# -------------------------
SPRING_MAIL_HOST=mailgate.thm.de
SPRING_MAIL_PORT=465
SPRING_MAIL_USERNAME=your-thm-email@student.thm.de
SPRING_MAIL_PASSWORD=your-thm-password
MAIL_FROM_ADDRESS=your-thm-email@student.thm.de

# -------------------------
# IMAP Inbound Mail (External Mailbox Polling)
# -------------------------
MAIL_IMAP_HOST=mailgate.thm.de
MAIL_IMAP_PORT=993
MAIL_IMAP_USERNAME=your-thm-email@student.thm.de
MAIL_IMAP_PASSWORD=your-thm-password
MAIL_IMAP_FOLDER=INBOX
MAIL_IMAP_POLL_INTERVAL_MS=300000
```

---

## Support Features

### External Mail Import (IMAP)

The backend polls the configured IMAP inbox at regular intervals (default: 5 minutes) and:

1. **Fetches unread messages** from the configured mailbox
2. **Downloads attachments** and stores them locally
3. **Deduplicates** using Message-ID (avoids re-importing the same mail multiple times)
4. **Marks as read** only after successful import
5. **Distributes to all users** — every registered user sees the incoming support mail

**Configuration:** Set `MAIL_IMAP_*` variables to enable.

### Ticket Tracking

When responding to an incoming (external) email:

1. A **unique ticket number** is auto-generated: `[TICKET-XXXXXXXX]`
2. Prepended to the reply's **subject** if not already present
3. Stored in the `tracking_code` field for reference

**Purpose:** Track conversations across multiple replies and support thread correlations.

### Shared Support Inbox

- All incoming external mails are visible to **all registered users**
- Replies use the configured **MAIL_FROM_ADDRESS** as the sender
- No individual user inboxes needed for external correspondence

---

## Seed users

> **Note:** These accounts are intended for development/testing only.

| # | First name | Last name | Email | Password |
|---:|-----------|----------|-------|----------|
| 1 | Ameline | Allanson | `aallanson@example.com` | `123456` |
| 2 | Sanson | Vardey | `svardey1@example.com` | `123456` |
| 3 | Jami | Poe | `jpoe@example.uk` | `123456` |
| 4 | Trent | Ianno | `tianno3@example.com` | `123456` |
| 5 | Alikee | Raisbeck | `araisbeck4@example.com` | `123456` |

---

## Quick start (local development)

### 1) Start the database (PostgreSQL)

1. Create a `.env` file in the repository root (same folder as `docker-compose.yml`) with the variables above.
2. Start Postgres:

```bash
docker compose up -d
```

PostgreSQL will be exposed on `localhost:5432`.

---

### 2) Run the backend

```bash
cd backend
./gradlew bootRun
```

---

### 3) Run the frontend

```bash
cd frontend
npm install
npm run start
```

---

## Build

### Backend

```bash
cd backend
./gradlew build
```

### Frontend

```bash
cd frontend
npm install
npm run build
```
