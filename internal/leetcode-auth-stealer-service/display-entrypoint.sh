#!/bin/sh
set -e

# hacky af, but its what we use to allow display attaching inside of the k8s pods

export DISPLAY=:99

Xvfb "$DISPLAY" -screen 0 1280x720x24 &
xvfb_pid=$!

# wait up to 30 seconds for the display to accept connections
attempt=0
until xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; do
    if ! kill -0 "$xvfb_pid" 2>/dev/null; then
        echo "Xvfb exited before the display became ready" >&2
        exit 1
    fi

    attempt=$((attempt + 1))
    if [ "$attempt" -ge 30 ]; then
        echo "Timed out waiting for Xvfb on $DISPLAY" >&2
        exit 1
    fi

    sleep 1
done

x11vnc \
    -display "$DISPLAY" \
    -forever \
    -shared \
    -nopw \
    -listen 0.0.0.0 \
    -rfbport 5900 \
    -bg

exec /opt/venv/bin/python src/server.py
