# Integrationsprüfung – IMAP/SMTP Mail-System

Folgende Checkliste zeigt alle Änderungen und deren Verifikation:

## ✅ Datenbankmodell

- [ ] **Mail-Entity** erweitert um:
  - `source: MailSource` – INTERN | EXTERN
  - `tracking_code: String?` – Eindeutige Ticketnummer
  - `external_message_id: String?` – Deduplizierungsschlüssel (unique)
  - `external_sender_email: String?` – Echo der externen Absenderadresse

**Verifizierung:** DB-Schema nach `docker compose up`:
```sql
SELECT column_name, data_type FROM information_schema.columns 
WHERE table_name='mails' AND column_name IN ('source', 'tracking_code', 'external_message_id', 'external_sender_email');
```

---

## ✅ Backend-Persistenzschicht

### Repository-Erweiterungen

- [ ] `MailRepository` – neue Methoden:
  - `findByExternalMessageId(externalMessageId: String): Mail?`
  - `existsByExternalMessageId(externalMessageId: String): Boolean`

**Datei:** `backend/src/main/kotlin/de/thm/mni/backend/mail/MailRepository.kt`

### Service-Logik

- [ ] `MailService.createImportedMail()` – Speichert externe Mails mit:
  - Deduplizierung per `externalMessageId`
  - Anlage von Anhängen via `FileStorageService.saveFile(byte[])`
  - Broadcast MailRecords an alle Benutzer (MailType.TO)

- [ ] `MailService.getMailByExternalMessageId()` – Lookup für Deduplizierung

- [ ] `MailService.applyTrackingCodeIfNeeded()` – Generiert Ticketnummern:
  - Format: `[TICKET-XXXXXXXX]`
  - Prepend zu Subject bei Replies (replyToIds nicht leer)
  - Ersetzt existierende Codes nicht

**Datei:** `backend/src/main/kotlin/de/thm/mni/backend/mail/MailService.kt`

### Dateispeicherung

- [ ] `FileStorageRepository.saveFile(fileName, contentType, bytes)` – Neue Methode zum Speichern von Rohdaten (für IMAP-Anhänge ohne `MultipartFile`)

- [ ] `FileStorageService` – entsprechende Service-Methode

**Dateien:**
- `backend/src/main/kotlin/de/thm/mni/backend/storage/FileStorageRepository.kt`
- `backend/src/main/kotlin/de/thm/mni/backend/storage/FileStorageService.kt`

---

## ✅ Mail-Transport (SMTP & IMAP)

### SMTP-Versand

- [ ] `SMTPService` – echte Implementierung:
  - Nutzt `JavaMailSender` für SMTP-Versand
  - Setzt `From:` auf `MAIL_FROM_ADDRESS`
  - Liest To/CC/BCC aus MailRecords (per Enum-Typ)
  - Hängt lokale Anhänge an (via `FileStorageService.load()`)
  - Fehlerbehandlung: Exception → return false

**Datei:** `backend/src/main/kotlin/de/thm/mni/backend/smtp/SMTPService.kt`

### IMAP-Import

- [ ] `MailInboxSyncService` – geplanter Service:
  - `@Scheduled(fixedDelayString = "${mail.imap.poll-interval-ms:300000}")`
  - Pollt ungelesene IMAP-Mails
  - Parst Text, HTML, Multipart mit Anhängen
  - Dedupliziert per Message-ID oder Fingerprint
  - Markiert erfolgreiche Importe als gelesen
  - Fehlerbehandlung: wirft Exceptions nicht (Logger.warn)

**Datei:** `backend/src/main/kotlin/de/thm/mni/backend/mail/imap/MailInboxSyncService.kt`

---

## ✅ API & Datenmodelle

### DTO-Erweiterungen

- [ ] `MailDTO` – neue Felder:
  - `trackingCode: String?`
  - `externalSenderEmail: String?`

- [ ] `MailMapper` – liest `mail.source` und `mail.tracking_code` direkt aus der Entity

**Dateien:**
- `backend/src/main/kotlin/de/thm/mni/backend/mail/dto/MailDTO.kt`
- `backend/src/main/kotlin/de/thm/mni/backend/mail/MailMapper.kt`

### Frontend-Typen

- [ ] `Mail` in `frontend/src/types/mails.ts` – neue Optional-Felder:
  - `trackingCode?: string`
  - `externalSenderEmail?: string`

**Datei:** `frontend/src/types/mails.ts`

---

## ✅ Konfiguration

### Umgebungsvariablen

- [ ] `application.properties` – neue Optionen:
  - SMTP: `spring.mail.host`, `port`, `username`, `password`, `mail.smtp.auth/ssl.enable`
  - SMTP: `mail.from-address`
  - IMAP: `mail.imap.host`, `port`, `username`, `password`, `folder`, `poll-interval-ms`

**Datei:** `backend/src/main/resources/application.properties`

### Build-Abhängigkeiten

- [ ] `build.gradle.kts` – ergänzt:
  - `implementation("org.springframework.boot:spring-boot-starter-mail:4.0.6")`

**Datei:** `backend/build.gradle.kts`

### Application Bootstrap

- [ ] `BackendApplication.kt` – aktiviert Scheduling:
  - `@EnableScheduling`

**Datei:** `backend/src/main/kotlin/de/thm/mni/backend/BackendApplication.kt`

---

## ✅ Dokumentation

- [ ] `.env.example` – vollständige Konfigurationsvorlage mit SMTP/IMAP-Variablen

- [ ] `README.md` – erweitert um:
  - SMTP/IMAP-Variablendokumentation
  - Abschnitt "Support Features"
  - Beschreibung des External Mail Import & Ticket Tracking

- [ ] `IMAP_SMTP_IMPLEMENTATION.md` – detaillierte technische Dokumentation:
  - Komponenten-Übersicht
  - Support-Workflow
  - Testszenarien
  - Performance-Überlegungen

---

## 🧪 Verifikationsschritte

### 1. Lokale Laufzeitprüfung

```bash
# Root-Verzeichnis
cp .env.example .env
# Bearbeite .env mit echten SMTP/IMAP-Credentials für THM

docker compose up -d

cd backend
./gradlew bootRun
# Log sollte KEIN ERROR beim Scheduling enthalten (falls IMAP aktiviert)
```

### 2. SMTP-Test (optional)

Wenn `SPRING_MAIL_HOST` konfiguriert:
```bash
curl -X POST http://localhost:8080/api/mails/send \
  -H "Content-Type: application/json" \
  -d '{"subject":"Test","content":"Body","toIds":["user-id"],"ccIds":[],"bccIds":[],"replyToIds":[]}'
# Sollte Mail versenden
```

### 3. IMAP-Test (nach 5 Min. oder Restart)

Wenn `MAIL_IMAP_HOST` konfiguriert:
- Warte auf nächsten Poll (default: 5 Min)
- Oder DB-Logs prüfen: `SELECT * FROM mails WHERE source='EXTERN' LIMIT 1;`
- Prüfe MailRecords: `SELECT COUNT(*) FROM mail_records WHERE mail_id = <id>;`

### 4. Ticketing-Test

```bash
# 1. Externe Mail als Basis anlegen (manuell via DB oder IMAP-Import)
# 2. User-Antwort mit replyToIds auf externe Mail senden

curl -X POST http://localhost:8080/api/mails/send \
  -H "Content-Type: application/json" \
  -d '{"subject":"Re: Support Anfrage","content":"Antwort","toIds":["external-id"],"ccIds":[],"bccIds":[],"replyToIds":["external-id"]}'

# 3. Überprüfe: Mail.trackingCode sollte `[TICKET-XXXXXXXX]` enthalten
# 4. SMTP sollte versiondet sein (falls konfiguriert)
```

### 5. API-Response-Prüfung

```bash
curl http://localhost:8080/api/mails/<id> -H "Authorization: Bearer <token>"
# Response sollte enthalten:
# - trackingCode
# - externalSenderEmail
# - source (EXTERN bei importierten Mails)
```

---

## 🐛 Bekannte Edge Cases

1. **Keine IMAP/SMTP konfiguriert** → Services sind inaktiv (no-op), kein Fehler
2. **IMAP-Verbindung fehlgeschlagen** → geloggt, nächster Poll versucht erneut
3. **SMTP-Fehler beim Versenden** → Mail mit `status=ERROR`, nicht neu versucht
4. **Leere Empfängerliste** → SMTP versendet nicht (early return)
5. **Ticket-Code in falscher Mail** → wird nicht automatisch repariert (manuell via DB)

---

## ✨ Nächste Schritte (optional)

- [ ] OAuth2 für SMTP/IMAP (statt Klartext-Passwörter)
- [ ] Ticket-Routing (Zuordnung zu Support-Agenten)
- [ ] Thread-Sicht (Konversationen gruppieren)
- [ ] Message-Queue für Versand-Retry
- [ ] Webhook-Integration für externe Systeme

