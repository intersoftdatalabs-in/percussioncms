package com.percussion.pubserver;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Regression for GH-847 / v8.1.7 PR #859: createServer sets publishServer DTS property. */
class PSPubServerDaoDtsDefaultTest {
  @Test
  void createServerInitializesPublishServerProperty() throws Exception {
    Path root = resolve();
    Path java =
        root.resolve(
            "system/services/src/com/percussion/services/pubserver/impl/PSPubServerDao.java");
    String src = Files.readString(java, StandardCharsets.UTF_8);
    int create = src.indexOf("public PSPubServer createServer");
    int ret = src.indexOf("return pubServer;", create);
    String body = src.substring(create, ret);
    assertTrue(body.contains("PUBLISH_SERVER_PROPERTY"), "must set PUBLISH_SERVER_PROPERTY");
    assertTrue(body.contains("\"NONE\""), "must fallback to NONE");
    assertTrue(body.contains("getAdminUrls"), "must consult delivery admin URLs");
  }

  private static Path resolve() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("system"))) return up;
    if (Files.isDirectory(cwd.resolve("system"))) return cwd;
    fail("root");
    return cwd;
  }
}
