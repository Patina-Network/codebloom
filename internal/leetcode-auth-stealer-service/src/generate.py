import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent

proto_dir = root / "proto"
python_output = root / "src" / "generated"

repo = root.parent.parent
java_output = repo / "lib" / "leetcode-auth-stealer-service" / "src" / "main" / "java"

python_output.mkdir(parents=True, exist_ok=True)
java_output.mkdir(parents=True, exist_ok=True)

subprocess.run(
    [
        sys.executable,
        "-m",
        "grpc_tools.protoc",
        f"-Igenerated={root / 'proto'}",
        f"--python_out={root / 'src'}",
        f"--grpc_python_out={root / 'src'}",
        f"--pyi_out={root / 'src'}",
        "generated/LeetcodeAuthStealerService.proto",
    ],
    check=True,
)

subprocess.run(
    [
        "protoc",
        f"-I{proto_dir}",
        f"--java_out={java_output}",
        f"--grpc-java_out={java_output}",
        str(proto_dir / "LeetcodeAuthStealerService.proto"),
    ],
    check=True,
)
