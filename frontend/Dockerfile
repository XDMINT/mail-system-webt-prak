FROM node:24.12.0-alpine AS builder

WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm install --global npm@11.6.2 \
    && npm ci

COPY angular.json tsconfig.json tsconfig.app.json tsconfig.spec.json .postcssrc.json ./
COPY public/ public/
COPY src/ src/

RUN npm run build

FROM caddy:2.10.2-alpine
LABEL authors="hellenmyglas"

COPY --from=builder /workspace/build/install/browser/ /srv/

COPY Caddyfile /etc/caddy/Caddyfile

RUN setcap -r /usr/bin/caddy \
    && addgroup -S -g 1000 frontend \
    && adduser -S -D -H -u 1000 -G frontend frontend

USER frontend

EXPOSE 8080

CMD ["caddy", "run", "--config", "/etc/caddy/Caddyfile", "--adapter", "caddyfile"]

