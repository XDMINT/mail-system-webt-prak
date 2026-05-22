FROM caddy:2-alpine
LABEL authors="hellenmyglas"

COPY build/install/browser/ /srv/

COPY Caddyfile /etc/caddy/Caddyfile

EXPOSE 80

CMD ["caddy", "run", "--config", "/etc/caddy/Caddyfile", "--adapter", "caddyfile"]

