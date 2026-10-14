# Frontend do SBI (Next.js). BACKEND_ORIGIN vale no build (rewrites) e na execucao (layouts do servidor).
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
ARG BACKEND_ORIGIN=http://api:8080
ENV BACKEND_ORIGIN=${BACKEND_ORIGIN} NEXT_TELEMETRY_DISABLED=1
RUN npm run build && npm prune --omit=dev

FROM node:24-alpine
WORKDIR /app
ARG BACKEND_ORIGIN=http://api:8080
ENV NODE_ENV=production BACKEND_ORIGIN=${BACKEND_ORIGIN} NEXT_TELEMETRY_DISABLED=1
COPY --from=build --chown=node:node /app/package.json /app/next.config.mjs ./
COPY --from=build --chown=node:node /app/node_modules ./node_modules
COPY --from=build --chown=node:node /app/.next ./.next
USER node
EXPOSE 3000
CMD ["node", "node_modules/next/dist/bin/next", "start", "--hostname", "0.0.0.0", "--port", "3000"]
