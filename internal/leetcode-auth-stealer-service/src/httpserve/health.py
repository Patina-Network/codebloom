"""Liveness and readiness HTTP endpoints."""

from typing import TYPE_CHECKING

from aiohttp import web

if TYPE_CHECKING:
    from httpserve.router import AppState


async def _livez(request: web.Request) -> web.Response:
    state: AppState = request.app["app_state"]

    return web.Response(
        text="OK" if state.live else "mr stark i dont feel so good",
        status=200 if state.live else 503,
    )


async def _readyz(request: web.Request) -> web.Response:
    state: AppState = request.app["app_state"]

    return web.Response(
        text="OK" if state.ready else "Not Ready", status=200 if state.ready else 503
    )


def _register_routes(app: web.Application) -> None:
    app.router.add_get("/livez", _livez)
    app.router.add_get("/readyz", _readyz)
