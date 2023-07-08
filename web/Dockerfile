FROM node:20.3.0-bullseye-slim AS build

WORKDIR /app
COPY . .
RUN npm install --no-optional
RUN npm run build


FROM nginx:latest

EXPOSE 80

COPY --from=build /app/dist /usr/share/nginx/html/
