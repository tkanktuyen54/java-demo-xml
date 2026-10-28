FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
ENV BACKEND_URL=http://host.docker.internal:8081
ENV NGINX_ENVSUBST_FILTER=^BACKEND_URL$
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/khoa/browser /usr/share/nginx/html
EXPOSE 80
