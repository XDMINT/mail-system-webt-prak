# Verifikation – Support-Mail, SMTP und IMAP

## Automatisierte Prüfungen

- [x] Neue IMAP-Mail ohne Präfix erhält beim Import keinen Trackingcode.
- [x] Vorhandene Ticketpräfixe werden beim Import erkannt und wiederverwendet.
- [x] Deduplizierung erfolgt zusätzlich über die externe `Message-ID`.
- [x] Ein Reply-Entwurf wird mit der ursprünglichen Mail verknüpft.
- [x] Der Reply-Entwurf adressiert den externen Absender als `TO`.
- [x] Die erste Antwort erzeugt genau einen zufälligen Trackingcode.
- [x] Weitere Antworten verwenden denselben Code.
- [x] SMTP verwendet `MAIL_FROM_ADDRESS` und die gespeicherten `TO`-/`CC`-/`BCC`-Empfänger.
- [x] Eine leere Empfängerliste gilt als Versandfehler.
- [x] Fehlgeschlagener SMTP-Versand persistiert `ERROR`.
- [x] Erfolgreich importierte IMAP-Nachrichten werden als `SEEN` markiert.
- [x] Fehlgeschlagene Importe bleiben ungelesen.
- [x] Frontend- und Backendtests sowie Produktionsbuild laufen über Gradle.

## Manueller Demoablauf ohne THM-Zugangsdaten

1. Stack mit `./gradlew composeUp` beziehungsweise `.\gradlew.bat composeUp` starten.
2. Mit einem der dokumentierten Keycloak-Demobenutzer anmelden.
3. Die externe Beispielmail „Question about my semester registration“ öffnen.
4. „Reply“ wählen.
5. Prüfen, dass Empfänger `erika.external@example.org` ist und der Betreff ein sichtbares `[TICKET-XXXXXXXX]` enthält.
6. Entwurf speichern. Ein realer Versand ist ohne konfigurierte SMTP-Zugangsdaten erwartungsgemäß nicht möglich und muss als Fehler angezeigt werden.

## Manueller THM-End-to-End-Test

1. Gültige SMTP-/IMAP-Werte ausschließlich in `.env` eintragen.
2. Eine neue, ungelesene Mail an die konfigurierte THM-Adresse senden.
3. Nach dem Poll prüfen, dass genau diese Mail allen internen Benutzern angezeigt wird und Attachments aus SeaweedFS abrufbar sind.
4. Prüfen, dass die Quellnachricht erst nach erfolgreichem Import als gelesen markiert wurde.
5. Reply-Entwurf erzeugen, Ticketnummer kontrollieren und versenden.
6. Beim externen Empfänger prüfen: `From` ist die gemeinsame THM-Adresse, `To` der externe Absender und der Betreff enthält das Ticketpräfix.
7. Eine externe Antwort mit demselben Präfix senden und prüfen, dass der Trackingcode wiedererkannt wird.

## Noch extern erforderlich

Der echte Transporttest benötigt gültige THM-SMTP-/IMAP-Zugangsdaten. Diese werden nicht committed und nicht in Logs oder Dokumentation ausgegeben.
