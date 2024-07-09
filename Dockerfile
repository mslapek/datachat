FROM rust:1.79-bookworm AS build

RUN apt-get update && apt-get install -y protobuf-compiler

RUN mkdir /src
WORKDIR /src
COPY datachat ./datachat
COPY datachat_pg ./datachat_pg
COPY protobuf ./protobuf
COPY targetdb ./targetdb
COPY Cargo.lock ./Cargo.lock
COPY Cargo.toml ./Cargo.toml

RUN cargo build --release

FROM debian:bookworm AS run

EXPOSE 8080

COPY --from=build /src/target/release/datachat /datachat
CMD ["/datachat"]
