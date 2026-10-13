"""HTTP version endpoint."""

from typing import TYPE_CHECKING, TypedDict

from aiohttp import web

if TYPE_CHECKING:
    from httpserve.router import AppState


class VersionResponse(TypedDict):
    version: str


async def _version(request: web.Request) -> web.Response:
    state: AppState = request.app["app_state"]

    body: VersionResponse = {"version": state.cfg.version}

    return web.json_response(body)


def _register_routes(app: web.Application) -> None:

    app.router.add_get("/version", _version)
