# IMAP/SMTP Integration – Implementierungsleitlinie

## Überblick

Das Mail-System wurde um folgende Komponenten erweitert:

### Backend-Komponenten

1. **`MailService`** — Zentrale Geschäftslogik
   - `createImportedMail()` – Speichert eingehende IMAP-Mails mit Deduplizierung
   - `applyTrackingCodeIfNeeded()` – Generiert und verwaltet Ticketnummern
   - `ensureExternalSenderUser()` – Erstellt User-Records für externe Absender

2. **`MailInboxSyncService`** (@Scheduled)
   - Periodischer Abruf ungelesener IMAP-Mails (default: alle 5 Min.)
   - Parst Anhänge und Inhalte
   - Deduplication per `externalMessageId`
   - Markiert erfolgreich importierte Mails als gelesen

3. **`SMTPService`** — Mailversand
   - Echte JavaMail/Spring Mail Integration
   - Versendet an alle MailRecord-Empfänger
   - Stellt Anhänge bei, nutzt konfigurierte Von-Adresse

4. **`FileStorageService/Repository`** — Dateianlage
   - Neue Methode zum Speichern von Rohbytes (für IMAP-Anhänge)
   - Generiert eindeutige Dateinamen per UUID

### Datenbankschema (Mail-Entity)

Neue Spalten:
- `source` – Enum: `INTERN` | `EXTERN` (für die UI-Unterscheidung)
- `tracking_code` – Eindeutige Ticketnummer für Support-Tickets
- `external_message_id` – Deduplizierungsschlüssel (Message-ID oder Fingerprint)
- `external_sender_email` – Echo der echten Absenderadresse

### Konfiguration

Umgebungsvariablen in `.env`:

```env
# SMTP (Versand)
SPRING_MAIL_HOST=mailgate.thm.de
SPRING_MAIL_PORT=465
SPRING_MAIL_USERNAME=your-account@thm.de
SPRING_MAIL_PASSWORD=your-password
MAIL_FROM_ADDRESS=your-account@thm.de

# IMAP (Empfang)
MAIL_IMAP_HOST=mailgate.thm.de
MAIL_IMAP_PORT=993
MAIL_IMAP_USERNAME=your-account@thm.de
MAIL_IMAP_PASSWORD=your-password
MAIL_IMAP_FOLDER=INBOX
MAIL_IMAP_POLL_INTERVAL_MS=300000
```

Siehe `.env.example` im Root für vollständige Vorlage.

---

## Support-Workflow

### 1. Eingehende Mail (IMAP)

```
[Externe Quelle] 
  ↓
MailInboxSyncService.synchronizeInbox() (alle 5 Min)
  ↓ (prüft ungelesene Mails)
extractMessageId() → deduplication check
  ↓ (bei Neu-Mail)
parseMessage() → extrahiert Subject, Body, Anhänge
  ↓
MailService.createImportedMail()
  └─ ensureExternalSenderUser() → erstellt User für externe Adresse
  └─ speichert Anhänge via FileStorageService.saveFile(byte[])
  └─ erstellt MailRecords für alle App-Benutzer (MailType.TO)
  └─ markiert IMAP-Mail als gelesen
  ↓
[Alle App-Benutzer sehen die Mail unter "incoming"]
```

### 2. Antwort auf externe Mail (Support-Reply)

```
[App-Benutzer antwortet auf externe Mail]
  ↓
Frontend sendet POST /mails/send mit replyToIds gefüllt
  ↓
MailService.createAndSendMail()
  └─ applyTrackingCodeIfNeeded() prüft:
    • Existiert schon ein tracking_code? → verwende ihn
    • Ist es eine Reply (replyToIds nicht leer)? → generiere neuen Ticket
    • Betreff hat Ticket-Präfix? → nutze ihn, sonst prepend
  └─ speichert unter status=SENT, source=INTERN
  ↓
SMTPService.sendEmail()
  └─ Setzt From: MAIL_FROM_ADDRESS
  └─ Setzt To/CC/BCC aus MailRecords
  └─ Versendet
  ↓
[Externe Empfänger erhalten Mail mit [TICKET-XXXXXXXX] Betreff]
```

---

## Testszenarien

### Szenario 1: IMAP-Import ohne Duplikate

1. Konfiguriere `.env` mit gültigen IMAP-Credentials
2. Warte auf nächsten Poll oder triggere manuell (z. B. Restart)
3. Prüfe, dass neue Mail in DB unter `mails` mit `source=EXTERN` erscheint
4. Prüfe, dass Anhänge in `uploads/` gespeichert sind
5. Erneuter Poll: Mail sollte als gelesen markiert sein (kein Reimport)

### Szenario 2: Support-Ticketing

1. Benutzer antwortet auf externe Mail mit replyToIds gesetzt
2. Betreff wird automatisch mit `[TICKET-XXXXXXXX]` ergänzt
3. Tracking-Code wird in `mail.tracking_code` gespeichert
4. SMTP versendet mit konfigurierter Von-Adresse
5. Nachfolgende Replies nutzen denselben Ticket-Code

### Szenario 3: Fehlerbehandlung

- **SMTP-Fehler**: Mail wird mit `status=ERROR` gespeichert, nicht als SENT
- **IMAP-Fehler beim Import**: Mail-Nachricht wird geloggt, nächster Poll versucht erneut (begrenzt durch `externalMessageId`)
- **Fehlende Konfiguration**: Services sind inaktiv (no-op), wenn Host/User/Password leer sind

---

## Performance & Skalierbarkeit

- **IMAP-Polling**: Pro Mailbox, nur ungelesene Mails abrufen (Flag-basierte Deduplizierung)
- **Anhänge**: Lokal in `FILE_UPLOAD_DIR` gespeichert, nicht in DB (externe Mails oft groß)
- **Benutzer-Broadcast**: Importierte Mail erstellt N MailRecords (N = Anzahl registrierter Benutzer); bei großen Nutzerzahlen ggf. Batching verwenden
- **Scheduled Task**: Blockiert nicht den Main-Thread, ausführbar mit `@EnableScheduling`

---

## Bekannte Limitations

1. **Keine Authentifizierung für Versand-Absender**
   - Alle Support-Replies nutzen die konfigurierte `MAIL_FROM_ADDRESS`
   - Keine Filterung nach Benutzer-EMail-Adresse
   
2. **Simplifizierte HTML-Parsing**
   - Text-Mails und einfache Multipart werden korrekt geparsed
   - Komplexe MIME-Strukturen können zu Datenverlust führen

3. **Tickets nur bei Antwort**
   - Externe Mails erhalten keinen automatischen Ticket-Code
   - Nur beim Versenden einer Antwort wird der Code generiert

4. **Kein Ticket-Routing**
   - Alle Benutzer sehen alle eingehenden Tickets
   - Keine Zuordnung zu spezifischen Support-Agenten

---

## Zukünftige Verbesserungen

- [ ] OAuth2 für SMTP/IMAP (statt Plain-Text-Passwörter)
- [ ] Ticket-Zuweisung an Benutzer
- [ ] Thread-basierte Konversations-Sicht (statt einzelne Mails)
- [ ] Automatische Ticket-Nummer auch für externe Mails
- [ ] Konfigurierbare Polling-Intervalle per Mailbox
- [ ] Retry-Logik für fehlgeschlagene Versände (Message Queue)

