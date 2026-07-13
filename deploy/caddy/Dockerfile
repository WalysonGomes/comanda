# Wildcard TLS needs DNS-01 (HTTP-01 can't prove ownership of *.${APP_DOMAIN}), and the DNS
# provider modules aren't in stock Caddy — xcaddy builds them in. Swap the cloudflare module below
# if the domain's DNS lives elsewhere (github.com/caddy-dns has modules for most providers).
FROM caddy:2-builder-alpine AS build
RUN xcaddy build --with github.com/caddy-dns/cloudflare

FROM caddy:2-alpine
COPY --from=build /usr/bin/caddy /usr/bin/caddy
COPY Caddyfile /etc/caddy/Caddyfile
