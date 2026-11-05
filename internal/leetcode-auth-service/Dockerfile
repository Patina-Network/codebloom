# build context is internal/
# `docker build -f internal/leetcode-auth-service/Dockerfile internal` (from the repo root)
FROM mirror.gcr.io/library/rust:1-alpine3.24 AS build

WORKDIR /app/leetcode-auth-service

RUN apk add --no-cache 'musl-dev=~1.2.6' 'protobuf-dev=~31.1'

COPY leetcode-auth-stealer-service/proto ../leetcode-auth-stealer-service/proto
COPY leetcode-auth-service/Cargo.toml leetcode-auth-service/Cargo.lock leetcode-auth-service/build.rs ./
COPY leetcode-auth-service/proto ./proto
COPY leetcode-auth-service/src ./src

RUN cargo build --release --locked

FROM mirror.gcr.io/library/alpine:3.24.3 AS server-runtime

RUN addgroup -g 10001 -S grpc-server \
    && adduser -u 10001 -S -G grpc-server -h /app grpc-server

WORKDIR /app

COPY --from=build /app/leetcode-auth-service/target/release/leetcode-auth-service ./

ENV ENVIRONMENT=production

USER 10001:10001

EXPOSE 8082 50052

ENTRYPOINT ["./leetcode-auth-service"]
