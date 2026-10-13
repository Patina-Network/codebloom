"""Expose default Python/runtime and Linux process metrics plus service counters."""

from aiohttp import web

from prom import render_metrics


async def _metrics(request: web.Request) -> web.Response:
    body, content_type = render_metrics()
    return web.Response(
        body=body,
        headers={"Content-Type": content_type},
    )


def _register_routes(app: web.Application) -> None:
    app.router.add_get("/metrics", _metrics)
