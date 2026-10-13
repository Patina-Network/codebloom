from dataclasses import dataclass

from aiohttp import web

from config import Config
from httpserve.health import _register_routes as register_health_routes
from httpserve.metrics import _register_routes as register_metrics_routes
from httpserve.version import _register_routes as register_version_routes


@dataclass
class AppState:
    cfg: Config
    ready: bool


def register_routes(app: web.Application, state: AppState) -> web.Application:
    app["app_state"] = state

    register_metrics_routes(app)
    register_health_routes(app)
    register_version_routes(app)

    return app
