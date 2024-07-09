FROM node:20.3.0-bullseye-slim AS build

RUN apt-get update && apt-get install -y protobuf-compiler

WORKDIR /app
COPY protobuf ./protobuf
COPY web ./web
WORKDIR /app/web

RUN npm install --no-optional
RUN npm run codegen && npm run build


FROM nginx:latest

EXPOSE 80

COPY --from=build /app/web/dist /usr/share/nginx/html/
