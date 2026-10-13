# LeetCode auth stealer service

Python gRPC service wrapping the new auth stealer logic that is done via direct Leetcode.com auth page
using Camoufox. The service has no external dependencies at all, it's essentially a direct
input/output operation.

## Run

From this directory:

```sh
# if you dont have uv
brew install uv

uv sync
uv run src/server.py # runs gRPC server on :50051
```
