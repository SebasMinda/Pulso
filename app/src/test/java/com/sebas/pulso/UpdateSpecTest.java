package com.sebas.pulso;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import org.json.JSONObject;
import org.junit.Test;

/** Comprueba versión, enlaces permitidos, tamaño y huella de la actualización. */
public class UpdateSpecTest {
  private JSONObject metadata(byte[] bytes) throws Exception {
    StringBuilder hash = new StringBuilder();
    for (byte b : MessageDigest.getInstance("SHA-256").digest(bytes))
      hash.append(String.format("%02x", b & 255));
    return new JSONObject()
        .put("versionCode", 3)
        .put("versionName", "0.3.0")
        .put("packageId", "com.sebas.pulso.app")
        .put("sha256", hash.toString())
        .put("sizeBytes", bytes.length);
  }

  @Test
  public void onlyOffersNewerVersions() throws Exception {
    UpdateSpec spec = new UpdateSpec(metadata(new byte[2048]), "com.sebas.pulso.app");
    assertTrue(spec.newerThan(2));
    assertFalse(spec.newerThan(3));
    assertFalse(spec.newerThan(4));
  }

  @Test
  public void acceptsOnlyThisRepository() {
    assertTrue(
        UpdateSpec.allowedAsset(
            "https://github.com/SebasMinda/Pulso/releases/download/v0.3.0/Pulso.apk"));
    String[] bad = {
      "http://github.com/SebasMinda/Pulso/releases/download/v1/Pulso.apk",
      "https://github.com.evil.test/SebasMinda/Pulso/releases/download/v1/Pulso.apk",
      "https://github.com/SebasMinda/Other/releases/download/v1/Pulso.apk",
      "https://github.com@evil.test/SebasMinda/Pulso/releases/download/v1/Pulso.apk",
      "https://github.com/SebasMinda/Pulso/releases/download/../other.apk",
      "https://github.com/SebasMinda/Pulso/releases/download/%2e%2e/other.apk"
    };
    for (String url : bad) assertFalse(url, UpdateSpec.allowedAsset(url));
  }

  @Test
  public void rejectsDifferentPackage() throws Exception {
    assertThrows(IOException.class, () -> new UpdateSpec(metadata(new byte[2048]), "another.app"));
  }

  @Test
  public void rejectsInvalidHashOrSize() throws Exception {
    JSONObject json = metadata(new byte[2048]).put("sha256", "bad");
    assertThrows(IOException.class, () -> new UpdateSpec(json, "com.sebas.pulso.app"));
    JSONObject tooLarge = metadata(new byte[2048]).put("sizeBytes", UpdateSpec.MAX_BYTES + 1);
    assertThrows(IOException.class, () -> new UpdateSpec(tooLarge, "com.sebas.pulso.app"));
  }

  @Test
  public void checksBothIntegrityAndCompleteness() throws Exception {
    byte[] bytes = new byte[2048];
    bytes[0] = 42;
    UpdateSpec spec = new UpdateSpec(metadata(bytes), "com.sebas.pulso.app");
    File file = Files.createTempFile("pulso-test", ".apk").toFile();
    try {
      Files.write(file.toPath(), bytes);
      spec.verify(file);
      bytes[0] = 43;
      Files.write(file.toPath(), bytes);
      assertThrows(IOException.class, () -> spec.verify(file));
      Files.write(file.toPath(), new byte[20]);
      assertThrows(IOException.class, () -> spec.verify(file));
    } finally {
      Files.deleteIfExists(file.toPath());
    }
  }
}
