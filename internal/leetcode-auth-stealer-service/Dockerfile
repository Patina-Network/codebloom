FROM ubuntu:25.10

COPY --from=ghcr.io/astral-sh/uv:0.11.15 /uv /bin/uv

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    UV_PROJECT_ENVIRONMENT=/opt/venv \
    HOME=/home/auth \
    XDG_CACHE_HOME=/home/auth/.cache

# regular mode
# hadolint ignore=DL3008
# RUN apt-get update \
#     && apt-get install -y --no-install-recommends ca-certificates tini \
#     && useradd --create-home --uid 10001 auth \
#     && rm -rf /var/lib/apt/lists/*
# display mode
# hadolint ignore=DL3008
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
    ca-certificates \
    tini \
    xvfb \
    x11vnc \
    x11-utils \
    && useradd --create-home --uid 10001 auth \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pyproject.toml uv.lock ./

RUN uv sync --frozen --no-dev --no-install-project \
    && /opt/venv/bin/playwright install-deps firefox \
    && mkdir -p /home/auth/.cache \
    && chown -R 10001:10001 /home/auth

COPY src ./src

RUN uv sync --frozen --no-dev --no-editable \
    && /opt/venv/bin/python -m camoufox fetch \
    && chown -R 10001:10001 /home/auth

USER 10001

EXPOSE 50051 8080
ENV GRPC_VERBOSITY=ERROR

# regular mode
# ENTRYPOINT ["tini", "--"]
# CMD ["/opt/venv/bin/python", "src/server.py"]

# display mode
ENV DISPLAY=:99
COPY --chmod=0755 display-entrypoint.sh /app/display-entrypoint.sh
EXPOSE 5900
ENTRYPOINT ["tini", "--"]
CMD ["/app/display-entrypoint.sh"]
