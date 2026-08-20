# Support-Reply, SMTP und IMAP

Dieses Dokument beschreibt den implementierten, bewusst kleinen Support-Mail-Flow. Maßgeblich sind die Aufgabenstellung und die Mail-Communication-Folie aus „Infrastructure as Code“.

## Rollen und Adressen

- Der über Keycloak angemeldete Benutzer ist der interne Bearbeiter und Autor einer Antwort.
- Der Empfänger einer Antwort ist automatisch der externe Absender der gewählten eingehenden Mail.
- Der sichtbare SMTP-Absender ist immer die gemeinsame THM-Adresse aus `MAIL_FROM_ADDRESS`.
- Die SMTP-Anmeldung verwendet `SPRING_MAIL_USERNAME` und `SPRING_MAIL_PASSWORD`.
- Es wird kein separates `Reply-To` gesetzt. Antworten externer Empfänger gehen dadurch wieder an die gemeinsame `From`-Adresse.

## Support- und Ticketablauf

1. Eine externe Mail wird über IMAP importiert und allen internen Benutzern als eingehende Mail zugeordnet.
2. Eine neue Mail ohne Ticketpräfix behält zunächst ihren ursprünglichen Betreff und hat keinen Trackingcode.
3. `POST /api/mails/{mailId}/reply` prüft Zugriff und externe Herkunft und erzeugt einen Antwortentwurf.
4. Der Entwurf erhält eine zufällige Nummer im Format `TICKET-XXXXXXXX`, sofern die eingehende Mail noch keine Nummer besitzt.
5. Der Betreff wird als `[TICKET-XXXXXXXX] Re: <ursprünglicher Betreff>` aufgebaut und im Formular angezeigt.
6. Weitere Antworten auf dieselbe Mail verwenden den bestehenden Trackingcode.
7. Der Versand lädt vorhandene Attachments aus SeaweedFS und setzt den Status nur bei erfolgreichem SMTP-Versand auf `SENT`. Andernfalls wird `ERROR` persistiert und HTTP 502 zurückgegeben.

## IMAP-Ablauf

Der Scheduler ist deaktiviert, solange Host, Benutzer oder Passwort fehlen. Bei aktiver Konfiguration:

1. wird der konfigurierte Ordner mit Schreibzugriff geöffnet;
2. werden ausschließlich Nachrichten ohne `SEEN`-Flag gesucht;
3. werden Absender, Betreff, Text und Attachments gelesen;
4. werden Attachments in SeaweedFS und die Mail in PostgreSQL gespeichert;
5. wird die Nachricht erst danach als `SEEN` markiert.

Schlägt die Verarbeitung fehl, bleibt die Nachricht ungelesen und wird beim nächsten Poll erneut versucht. Bereits gespeicherte `Message-ID`s werden nicht erneut importiert; eine solche IMAP-Nachricht kann sicher als gelesen markiert werden.

## Attachment-Konsistenz

Neu gespeicherte S3-Objekte werden bei einem Rollback der zugehörigen Datenbanktransaktion kompensierend gelöscht. Beim Ersetzen oder Löschen einer Mail werden nicht mehr benötigte Objekte erst nach erfolgreichem Datenbank-Commit entfernt. Eine perfekte verteilte Transaktion zwischen PostgreSQL und S3 wird bewusst nicht eingeführt.

## Konfiguration

```env
SPRING_MAIL_HOST=mailgate.thm.de
SPRING_MAIL_PORT=465
SPRING_MAIL_USERNAME=your-account@thm.de
SPRING_MAIL_PASSWORD=your-password
MAIL_FROM_ADDRESS=your-account@thm.de

MAIL_IMAP_HOST=mailgate.thm.de
MAIL_IMAP_PORT=993
MAIL_IMAP_USERNAME=your-account@thm.de
MAIL_IMAP_PASSWORD=your-password
MAIL_IMAP_FOLDER=INBOX
MAIL_IMAP_POLL_INTERVAL_MS=300000
```

Echte Zugangsdaten gehören ausschließlich in die nicht getrackte `.env`.

## Bewusste Grenzen

- kein separates Ticketmodell, Routing, SLA oder Bearbeiter-Locking;
- kein UID-/UIDVALIDITY-Synchronisationssystem;
- vereinfachte Verarbeitung üblicher Text-, HTML- und Multipart-Mails;
- keine Versand-Queue und kein automatisches Retry für `ERROR`-Mails.
