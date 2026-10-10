"""Create the bounded metadata consumed by Pulso's updater."""
import hashlib
import json
import pathlib
import sys

project = pathlib.Path(__file__).resolve().parent.parent
properties = dict(
    line.split("=", 1) for line in (project / "version.properties").read_text().splitlines()
    if line.strip() and not line.lstrip().startswith("#")
)
apk = pathlib.Path(sys.argv[1])
size = apk.stat().st_size
if not 1024 <= size <= 100 * 1024 * 1024:
    raise SystemExit("APK size is invalid")
metadata = {
    "versionCode": int(properties["versionCode"]),
    "versionName": properties["versionName"],
    "packageId": "com.sebas.pulso.app",
    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
    "sizeBytes": size,
    "changes": (project / "CHANGELOG.md").read_text()[:600],
}
pathlib.Path(sys.argv[2]).write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n")
