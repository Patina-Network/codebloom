"""Internal-only gRPC transport. Inject an AuthProvider to swap login implementations."""

import asyncio
import signal

import grpc
import structlog
from aiohttp import web
from grpc_reflection.v1alpha import reflection

from config import Config
from generated import (
    LeetcodeAuthStealerService_pb2,
    LeetcodeAuthStealerService_pb2_grpc,
)
from grpcserve.interceptor import RequestIdInterceptor
from grpcserve.service.auth import AuthService
from httpserve.router import AppState, register_routes
from provider.camou import CamouProvider

logger = structlog.get_logger()

shared_processors = [
    structlog.contextvars.merge_contextvars,
    structlog.processors.TimeStamper(fmt="iso"),
    structlog.processors.add_log_level,
]


async def serve(c: Config) -> None:
    address = f"{c.grpc_host}:{c.grpc_port}"

    server = grpc.aio.server(
        interceptors=[RequestIdInterceptor()],
        # more than 1 will crash the pod due to multiple browsers
        maximum_concurrent_rpcs=1
    )
    invis_provider = CamouProvider(c)

    LeetcodeAuthStealerService_pb2_grpc.add_LeetcodeAuthStealerServiceServicer_to_server(
        AuthService(c, invis_provider), server
    )

    if c.grpc_reflection:
        svc_names = {
            LeetcodeAuthStealerService_pb2.DESCRIPTOR.services_by_name[
                "LeetcodeAuthStealerService"
            ].full_name,
            reflection.SERVICE_NAME,
        }
        reflection.enable_server_reflection(svc_names, server)

    if server.add_insecure_port(address) == 0:
        raise RuntimeError(f"Unable to bind gRPC server to {address}")

    app_state = AppState(c, ready=False)
    app = register_routes(web.Application(), app_state)
    runner = web.AppRunner(app)

    loop = asyncio.get_running_loop()
    shutdown = asyncio.Event()
    signals = (signal.SIGTERM, signal.SIGINT)
    registered_signals: list[signal.Signals] = []
    grpc_started = False
    try:
        for sig in signals:
            loop.add_signal_handler(sig, shutdown.set)
            registered_signals.append(sig)

        await runner.setup()
        site = web.TCPSite(runner, c.http_host, c.http_port)
        await site.start()

        await server.start()
        grpc_started = True
        app_state.ready = True
        logger.info(
            "Auth service started at gRPC: %s:%s - HTTP: %s:%s",
            c.grpc_host,
            c.grpc_port,
            c.http_host,
            c.http_port,
        )
        await shutdown.wait()
    finally:
        try:
            if grpc_started:
                await server.stop(grace=5)
                await server.wait_for_termination()
        finally:
            try:
                await runner.cleanup()
            finally:
                for sig in registered_signals:
                    loop.remove_signal_handler(sig)


def main() -> None:
    c = Config.from_env()

    if c.environment == "production" or c.environment == "staging":
        processors = shared_processors + [
            structlog.processors.dict_tracebacks,
            structlog.processors.JSONRenderer(),
        ]
    else:
        processors = shared_processors + [
            structlog.dev.ConsoleRenderer(colors=True),
        ]

    structlog.configure(
        processors=processors,
        logger_factory=structlog.PrintLoggerFactory(),
        cache_logger_on_first_use=True,
    )

    c.log()

    try:
        asyncio.run(serve(c))
    except Exception:
        logger.exception("service stopped unexpectedly")
        raise SystemExit(1) from None


if __name__ == "__main__":
    main()
