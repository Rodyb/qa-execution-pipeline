FROM node:20-alpine AS base
WORKDIR /app
COPY package*.json ./
RUN npm ci

# Build both the frontend (Vite) and backend bundle
FROM base AS build
WORKDIR /app
COPY . .
ENV NODE_ENV=production
RUN npm run build

# Backend runtime image
FROM node:20-alpine AS backend
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm ci --omit=dev
COPY --from=build /app/dist ./dist
COPY --from=build /app/client/public ./client/public
EXPOSE 5000
CMD ["node", "dist/index.cjs"]

# Frontend runtime image (static assets served by nginx)
FROM nginx:1.27-alpine AS frontend
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/public /usr/share/nginx/html

