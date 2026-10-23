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
    # controlls /livez
    # app may set this to False to trigger an unrecoverable state
    # look at httpserve.health for how liveness works in k8s
    live: bool = True

    # use to mark unalive without having to import/pass AppState
    def mark_unalive(self) -> None:
        self.live = False


def register_routes(app: web.Application, state: AppState) -> web.Application:
    app["app_state"] = state

    register_metrics_routes(app)
    register_health_routes(app)
    register_version_routes(app)

    return app
