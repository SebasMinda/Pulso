"""Crea update.json con versión, tamaño y SHA-256 del APK para el actualizador de Pulso."""
import hashlib
import json
import pathlib
import sys

project = pathlib.Path(__file__).resolve().parent.parent
properties = dict(
    line.split("=", 1) for line in (project / "version.properties").read_text().splitlines()
    if line.strip() and not line.lstrip().startswith("#")
)
# Uso: python tools/make_update.py ruta/Pulso.apk ruta/update.json
apk = pathlib.Path(sys.argv[1])
size = apk.stat().st_size
if not 1024 <= size <= 100 * 1024 * 1024:
    raise SystemExit("APK size is invalid")
# Describe el APK real y toma un resumen de cambios para mostrarlo en la app.
metadata = {
    "versionCode": int(properties["versionCode"]),
    "versionName": properties["versionName"],
    "packageId": "com.sebas.pulso.app",
    "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
    "sizeBytes": size,
    "changes": (project / "CHANGELOG.md").read_text()[:600],
}
pathlib.Path(sys.argv[2]).write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n")
