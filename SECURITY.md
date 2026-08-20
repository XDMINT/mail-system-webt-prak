# Security infrastructure

This document records the deliberately small security setup used for the university assignment. It follows the supplied ModSecurity, certificate, container-hardening and Snort examples and adapts only ports, routes and permissions required by this application.

## Public request path

```text
Browser
  -> localhost:80/443
  -> ips (Snort 3, inline NFQUEUE)
  -> proxy (ModSecurity + OWASP CRS, TLS termination)
     -> frontend:8080
     -> backend:8080
     -> keycloak:8080
```

Only `ips` publishes host ports. HTTP port 80 is forwarded through Snort to the WAF and redirected there to HTTPS. HTTPS port 443 is forwarded through Snort to WAF port 8443. TLS terminates at the WAF; the isolated container hops use HTTP as allowed by the course setup.

The certificate and key in `proxy/cert/` are copied unchanged from the lecture template and mounted through Compose secrets. They are self-signed development material for `localhost`, `proxy` and `127.0.0.1`, not a production PKI. Browsers therefore require a one-time local trust exception.

## Network separation

| Network | Members | Purpose |
| --- | --- | --- |
| `ingress` | `ips`, `proxy` | Only public ingress path |
| `web` | `proxy`, `frontend` | Static UI delivery |
| `application` | `proxy`, `backend`, `keycloak` | API and identity routing |
| `database` | `backend`, `database` | PostgreSQL access only |
| `storage` | `backend`, `seaweedfs` | S3-compatible attachment access only |

The backend intentionally joins three private networks because it is the sole application component that needs both persistence systems. PostgreSQL and SeaweedFS do not share a network with the public ingress components.

## WAF behavior and tuning

The proxy uses the lecture image `owasp/modsecurity-crs:4.25-nginx-lts` with OWASP CRS enabled, blocking paranoia level 2, detection paranoia level 3, JSON audit output and the assignment's request-size limits. Audit parts are limited to transaction metadata and rule messages (`AHZ`); complete request/response headers and bodies are omitted so bearer tokens, Keycloak cookies and mail content are not written to the audit log. The only WAF-disabled endpoint is its local `/healthz` response, which contains no application functionality.

The following narrow target exclusions are required for valid framework payloads:

- Keycloak validates `redirect_uri` and `post_logout_redirect_uri` against the configured client. CRS rule 934190 nevertheless classifies local HTTPS redirect values as SSRF, so only those two parameters are excluded from that rule under `/auth/`.
- Keycloak cryptographically protects `KC_RESTART`. Its random ciphertext can resemble SQL punctuation, so only that cookie is excluded from rules 942420 and 942440 under `/auth/`.
- Mail creation and updates use a multipart part named `data` containing JSON plus arbitrary attachment bytes. Binary bytes are excluded only from the printable-byte rule 920272, and only `ARGS:data` is excluded from the listed SQL punctuation rules under `/api/mails`. All other URI, header, filename, parameter and CRS checks remain active. The backend still performs typed JSON deserialization, Bean Validation, recipient checks, parameterized persistence, attachment-size enforcement and filename/media-type normalization.

These exclusions were derived from reproducible audit-log rule matches during the real PKCE and multipart flows. A SQL-injection request against `/api/users` remains blocked with HTTP 403.

The proxy uses a bounded `/tmp` `tmpfs` as `MODSEC_UPLOAD_DIR`. Mounting `/tmp` hides the image's pre-created `/tmp/modsecurity/upload` directory; selecting the existing writable tmpfs directly prevents failed multipart parsing without adding persistent storage.

## Container hardening and exceptions

| Service | Applied hardening | Verified exception |
| --- | --- | --- |
| `frontend` | UID/GID 1000, read-only root, all capabilities dropped, `no-new-privileges`, bounded `/tmp`, `/config`, `/data` tmpfs | Caddy's low-port file capability is removed and it listens internally on 8080 |
| `backend` | Read-only root, all capabilities dropped, `no-new-privileges`, bounded `/tmp` tmpfs | None |
| `seaweedfs` | UID/GID 1000, read-only root, all capabilities dropped, `no-new-privileges`, bounded `/tmp` tmpfs | Named volume remains writable at `/data` for attachments |
| `database` | Read-only root, all capabilities dropped by default, `no-new-privileges`, bounded runtime tmpfs | Writable named data volume plus `CHOWN`, `DAC_OVERRIDE`, `FOWNER`, `SETGID`, `SETUID` are required by the official PostgreSQL entrypoint/runtime |
| `keycloak` | All capabilities dropped, `no-new-privileges`, bounded `/tmp` tmpfs | Root filesystem is writable because the assignment-approved `start-dev` path writes below `/opt/keycloak/lib` during startup |
| `proxy` | All capabilities dropped, `no-new-privileges`, bounded `/tmp` tmpfs, unprivileged image user 101 | Root filesystem remains writable because the upstream image renders Nginx/CRS configuration at startup |
| `ips` | All capabilities dropped by default, `no-new-privileges`, bounded `/tmp` tmpfs | Root plus `DAC_OVERRIDE`, `NET_ADMIN`, `NET_RAW`, `SETGID`, `SETUID`, IP forwarding and writable generated Snort configuration are required for iptables/NFQUEUE inline operation |

No privileged container or Docker socket mount is used. The Snort exceptions are intentionally limited to the supplied inline demonstrator and must not be copied to ordinary application containers.

## Snort demonstrator

The implementation retains the lecture's Snort 3 build, community rules and NFQUEUE approach. Its entrypoint was generalized from one `FORWARD_PORT` to `FORWARD_PORTS="8080 8443"`, because this project must carry both the HTTP redirect and HTTPS application path. It installs DNAT and NFQUEUE rules for both ports and starts Snort in inline mode.

This is a course NIDS/NIPS demonstrator, not a tuned production rule deployment. Application-aware HTTPS attack blocking is performed by ModSecurity after TLS termination; Snort demonstrates inspection/enforcement on the lower network layer as required by the exercise.

## Verification

Start the complete stack through the required one-command task:

```powershell
.\gradlew.bat composeUp
```

The completed public-path verification covered:

- HTTP-to-HTTPS redirect;
- application, Swagger UI and JSON/YAML OpenAPI access through the public path;
- HTTP 401 for an anonymous API request;
- HTTP 403 for a SQL-injection probe;
- Keycloak Authorization Code Flow with PKCE, token exchange and logout;
- authenticated API access;
- real multipart PNG upload, authenticated inline preview and cleanup of the created draft.

Container configuration is additionally checked with `docker compose config`, `docker compose ps`, `docker inspect` and the Snort startup/iptables state. Real SMTP and IMAP transport still requires local THM credentials in the ignored `.env` and is independent of the public web-security path.
