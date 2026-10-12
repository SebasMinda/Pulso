"""Comprueba la firma pública, identidad y versión de un APK antes de publicarlo."""
import pathlib
import re
import sys

project = pathlib.Path(__file__).resolve().parent.parent
# Recibe el APK, la salida de apksigner y la salida de aapt dump badging.
apk, cert, badging = map(pathlib.Path, sys.argv[1:4])
properties = dict(line.split("=", 1) for line in (project / "version.properties").read_text().splitlines() if "=" in line)
expected = re.search(r"SHA256:\s*([0-9A-Fa-f:]+)", (project / "SIGNING_CERTIFICATE.txt").read_text()).group(1).replace(":", "").lower()
# Compara el certificado del APK con la huella pública estable del proyecto.
signers = re.findall(r"Signer #\d+ certificate SHA-256 digest:\s*([0-9a-fA-F]+)", cert.read_text())
if signers != [expected]:
    raise SystemExit("El APK no tiene la firma estable de Pulso.")
# La identidad y versión deben coincidir con version.properties.
package = re.search(r"package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'", badging.read_text())
if not package or package.groups() != ("com.sebas.pulso.app", properties["versionCode"], properties["versionName"]):
    raise SystemExit("La identidad o versión del APK no coincide con el código.")
if not apk.read_bytes().startswith(b"PK\x03\x04"):
    raise SystemExit("El archivo no es un APK estándar.")
print("Firma, identidad y versión verificadas.")
