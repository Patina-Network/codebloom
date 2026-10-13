import os
from dataclasses import dataclass
from typing import Literal

import structlog
from pydantic import TypeAdapter

type Environment = Literal["production", "staging", "local"]


@dataclass(frozen=True)
class Config:
    grpc_host: str
    grpc_port: int
    grpc_reflection: bool
    http_host: str
    http_port: int
    version: str
    environment: Environment
    auth_timeout_seconds: float
    grpc_shutdown_grace_seconds: float
    max_challenge_try_clicks: int
    disable_virtual: bool
    log_level: str

    @classmethod
    def from_env(cls) -> Config:
        return cls(
            grpc_port=int(os.environ.get("GRPC_PORT", "50051")),
            grpc_host=os.environ.get("GRPC_HOST", "0.0.0.0"),
            http_host=os.environ.get("HTTP_HOST", "0.0.0.0"),
            http_port=int(os.environ.get("HTTP_PORT", "8081")),
            version=os.environ.get("VERSION", "unknown"),
            disable_virtual=bool(os.environ.get("DISABLE_VIRTUAL", "false")),
            grpc_reflection=bool(os.environ.get("GRPC_REFLECTION", "false")),
            environment=TypeAdapter(Environment).validate_python(
                os.environ.get("ENVIRONMENT", "local")
            ),
            auth_timeout_seconds=float(os.environ.get("AUTH_TIMEOUT_SECONDS", "120")),
            max_challenge_try_clicks=int(
                os.environ.get("AUTH_MAX_CHALLENGE_TRY_CLICKS", "20")
            ),
            grpc_shutdown_grace_seconds=float(
                os.environ.get("GRPC_SHUTDOWN_GRACE_SECONDS", "5")
            ),
            log_level=os.environ.get("LOG_LEVEL", "INFO"),
        )

    def log(self) -> None:
        logger: structlog.stdlib.BoundLogger = structlog.get_logger()

        logger.info(
            "config values:",
            grpc_host=self.grpc_host,
            grpc_port=self.grpc_port,
            http_host=self.http_host,
            http_port=self.http_port,
            version=self.version,
            environment=self.environment,
            auth_timeout_seconds=self.auth_timeout_seconds,
            grpc_shutdown_grace_seconds=self.grpc_shutdown_grace_seconds,
            log_level=self.log_level,
        )
