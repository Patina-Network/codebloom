import uuid

import grpc
import structlog
from structlog.contextvars import (
    bind_contextvars,
    clear_contextvars,
)

logger = structlog.get_logger()


class RequestIdInterceptor(grpc.aio.ServerInterceptor):
    async def intercept_service(
        self,
        continuation,
        handler_call_details,
    ):
        handler = await continuation(handler_call_details)

        if handler is None or handler.unary_unary is None:
            return handler

        original_handler = handler.unary_unary
        method = handler_call_details.method

        async def wrapped_handler(request, context):
            clear_contextvars()

            metadata = dict(context.invocation_metadata())
            request_id = metadata.get("x-request-id") or str(uuid.uuid4())

            bind_contextvars(
                request_id=request_id,
                grpc_method=method,
            )

            try:
                logger.info("grpc request started")

                response = await original_handler(request, context)

                logger.info("grpc request completed")
                return response

            except Exception:
                logger.exception("grpc request failed")
                raise

            finally:
                clear_contextvars()

        return grpc.unary_unary_rpc_method_handler(
            wrapped_handler,
            request_deserializer=handler.request_deserializer,
            response_serializer=handler.response_serializer,
        )
