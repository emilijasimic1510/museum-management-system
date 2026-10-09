FROM node:22-alpine AS build
WORKDIR /src
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /src/dist/museum/browser /srv
COPY Caddyfile /etc/caddy/Caddyfile
EXPOSE 80 443
