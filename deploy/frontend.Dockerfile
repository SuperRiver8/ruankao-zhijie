FROM node:22-alpine AS build
WORKDIR /app
COPY frontend/customer/package*.json ./customer/
COPY frontend/admin/package*.json ./admin/
RUN npm --prefix customer ci --no-audit --no-fund && npm --prefix admin ci --no-audit --no-fund
COPY frontend/ .
RUN npm run build
FROM nginx:alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/customer/dist /usr/share/nginx/mobile
COPY --from=build /app/admin/dist /usr/share/nginx/admin
EXPOSE 80 81
