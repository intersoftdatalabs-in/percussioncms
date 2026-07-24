package com.percussion.delivery.metadata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("UnitTest")
class DefaultH2BeansPackagingTest {
  @Test
  void metadataBeansDefaultToH2() throws Exception {
    Path beans = Path.of("src/main/java/webapp/WEB-INF/beans.xml");
    String text = Files.readString(beans, StandardCharsets.UTF_8);
    assertTrue(text.contains("org.h2.Driver"), text);
    assertTrue(text.contains("org.hibernate.dialect.H2Dialect"), text);
    assertTrue(text.contains("h2data/percmetadata"), text);
    assertFalse(text.contains("jdbc:derby:"), "defaults must not use jdbc:derby");
  }
}
