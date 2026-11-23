/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.i18n;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Delivery-panel PUBLISH_DELIVERY_* i18n registration for issue #7105 (residual from #7076).
 *
 * <p>{@code #7076} keyed the ~200 delivery-panel chrome strings through {@code message(MSG.
 * PUBLISH_DELIVERY_*)} in {@code WebUI/src/main/ts/i18n/message.ts} but registered only a
 * handful of canonical {@code CmsUi.tmx} units, leaving ~203 of the 211 keys with no catalog
 * entry. English rendering was unaffected (TypeScript {@code @}-suffix fallback), but every
 * non-English session silently rendered fallback English instead of a catalog lookup. This test
 * pins the catalog side: every {@code PUBLISH_DELIVERY_*} fallback text has exactly one {@code
 * <tu>} with an {@code en-us} segment matching the TypeScript text (same mechanics as {@code
 * PSExplorerScheduleConfirmKeysTmxTest}). Units are {@code en-us}-only by precedent
 * (#6894/#7056/#7076); other locales resolve through the {@code regional -&gt; base -&gt;
 * en-us} chain, so no bulk translation back-fill is in scope for this issue.
 *
 * <p>Note: {@code Unknown file driver: {0}} is also registered by open PR #7149 (issue #7106);
 * both PRs add the identical {@code en-us}-only unit at disjoint anchors, so whichever lands
 * second drops its copy on rebase.
 */
public class PSPublishDeliveryPanelKeysTmxTest {

  /** Expected {@code perc.ui.publish.modern@...} tuid suffix (English) for every delivery key. */
  private static final List<String> EXPECTED_ENGLISH = List.of(
      "Delivery type name already exists",
      "Delivery Type",
      "Database connection",
      "Database host",
      "Database name",
      "MSSQL owner",
      "Database password",
      "Database port",
      "Oracle schema",
      "Oracle SID",
      "Database user id",
      "FTP folder",
      "FTP host",
      "FTP password",
      "FTP port",
      "FTP user id",
      "FTPS folder",
      "FTPS host",
      "FTPS port",
      "FTPS user id",
      "SFTP folder",
      "SFTP host",
      "SFTP passphrase",
      "SFTP port",
      "SFTP private key",
      "SFTP username",
      "Amazon S3 access key",
      "Amazon S3 bucket",
      "Amazon S3 region",
      "Amazon S3 secret key",
      "Local folder",
      "Connection test",
      "Mark delivery server as default",
      "Rename delivery server",
      "* Host",
      "* Port",
      "* User ID",
      "* New password",
      "* Database",
      "* Owner",
      "* Schema",
      "* SID",
      "* Name",
      "* Bucket",
      "* Region",
      "* New access key",
      "* New secret key",
      "* New passphrase",
      "* New private key",
      "Database server",
      "Server IP / host",
      "SID / service",
      "Host",
      "Port",
      "User ID",
      "Name",
      "Type",
      "Default",
      "Database",
      "Database / SID",
      "Region",
      "Bucket",
      "Owner",
      "Schema",
      "SID",
      "Password",
      "Passphrase",
      "Private key",
      "Access key",
      "Secret key",
      "Cancel",
      "Set connection",
      "Save connection",
      "Set host",
      "Save host",
      "Set database name",
      "Save database name",
      "Set MSSQL owner",
      "Save MSSQL owner",
      "Set password",
      "Save password",
      "Set port",
      "Save port",
      "Set Oracle schema",
      "Save Oracle schema",
      "Set Oracle SID",
      "Save Oracle SID",
      "Set user id",
      "Save user id",
      "Set folder",
      "Save folder",
      "Set username",
      "Save username",
      "Set SFTP private key",
      "Save SFTP private key",
      "Set SFTP passphrase",
      "Save SFTP passphrase",
      "Set S3 access key",
      "Save S3 access key",
      "Set S3 region",
      "Save S3 region",
      "Set S3 bucket",
      "Save S3 bucket",
      "Set S3 secret key",
      "Save S3 secret key",
      "Rename",
      "Save name",
      "Mark as default",
      "Save default",
      "Test connection",
      "Leave blank to clear the folder.",
      "Mark this server as the site default? Other servers lose the default badge only after a successful save.",
      "Database connection can be set only on a database delivery server.",
      "Yes",
      "No",
      "Select",
      "Unknown file driver: {0}",
      "Connection test failed. Check the server settings and try again.",
      "Connection test succeeded.",
      "Database host can be set only on a database delivery server.",
      "Database host is required",
      "Database host must be 255 characters or fewer",
      "Database name can be set only on a MySQL or MSSQL delivery server.",
      "Database name is required",
      "Database name must be 255 characters or fewer",
      "Database password can be set only on a database delivery server.",
      "Stored password is never shown.",
      "Database password is required",
      "Database password must be 255 characters or fewer",
      "Database port can be set only on a database delivery server.",
      "Database port is required",
      "Database port must be 255 characters or fewer",
      "Database user id can be set only on a database delivery server.",
      "Database user id is required",
      "Database user id must be 255 characters or fewer",
      "This server is already the default.",
      "Default flag must be true to mark a delivery server as default.",
      "Publish server name already exists",
      "Invalid character in server name. Characters allowed are: a-z, 0-9, ., _ and -.",
      "Name is a required field.",
      "Server name cannot have more than 50 characters.",
      "FTPS folder can be set only on an FTPS delivery server.",
      "FTPS folder must be 255 characters or fewer",
      "FTPS host can be set only on an FTPS delivery server.",
      "FTPS host is required",
      "FTPS host must be 255 characters or fewer",
      "FTPS port can be set only on an FTPS delivery server.",
      "FTPS port is required",
      "FTPS port must be 255 characters or fewer",
      "FTPS user id can be set only on an FTPS delivery server.",
      "FTPS user id is required",
      "FTPS user id must be 255 characters or fewer",
      "FTP folder can be set only on an FTP delivery server.",
      "FTP folder must be 255 characters or fewer",
      "FTP host can be set only on an FTP delivery server.",
      "FTP host is required",
      "FTP host must be 255 characters or fewer",
      "FTP password can be set only on an FTP delivery server.",
      "FTP password is required",
      "FTP password must be 255 characters or fewer",
      "FTP port can be set only on an FTP delivery server.",
      "FTP port is required",
      "FTP port must be 255 characters or fewer",
      "FTP user id can be set only on an FTP delivery server.",
      "FTP user id is required",
      "FTP user id must be 255 characters or fewer",
      "Local folder can be set only on a Local delivery server.",
      "Local folder must be 255 characters or fewer",
      "MSSQL owner can be set only on an MSSQL delivery server.",
      "MSSQL owner is required",
      "MSSQL owner must be 255 characters or fewer",
      "Oracle schema can be set only on an Oracle delivery server.",
      "Oracle schema is required",
      "Oracle schema must be 255 characters or fewer",
      "Oracle SID can be set only on an Oracle delivery server.",
      "Oracle SID is required",
      "Oracle SID must be 255 characters or fewer",
      "S3 access key can be set only on an Amazon S3 delivery server.",
      "Stored access key is never shown.",
      "S3 access key is required",
      "S3 access key must be 255 characters or fewer",
      "S3 bucket can be set only on an Amazon S3 delivery server.",
      "S3 bucket is required",
      "S3 bucket must be 255 characters or fewer",
      "S3 region can be set only on an Amazon S3 delivery server.",
      "S3 region is required",
      "S3 region must be 255 characters or fewer",
      "S3 secret key can be set only on an Amazon S3 delivery server.",
      "Stored secret key is never shown.",
      "S3 secret key is required",
      "S3 secret key must be 255 characters or fewer",
      "SFTP folder can be set only on an SFTP delivery server.",
      "SFTP folder must be 255 characters or fewer",
      "SFTP host can be set only on an SFTP delivery server.",
      "SFTP host is required",
      "SFTP host must be 255 characters or fewer",
      "SFTP passphrase can be set only on an SFTP delivery server.",
      "Stored passphrase is never shown.",
      "SFTP passphrase is required",
      "SFTP passphrase must be 255 characters or fewer",
      "SFTP port can be set only on an SFTP delivery server.",
      "SFTP port is required",
      "SFTP port must be 255 characters or fewer",
      "SFTP private key can be set only on an SFTP delivery server.",
      "Stored private key is never shown.",
      "SFTP private key is required",
      "SFTP private key must be 255 characters or fewer",
      "SFTP username can be set only on an SFTP delivery server.",
      "SFTP username is required",
      "SFTP username must be 255 characters or fewer");

  @Test
  public void deliveryPanelKeys_areRegisteredWithEnglish() throws Exception {
    assertKeysRegistered(loadEnSegsByTuid(), EXPECTED_ENGLISH);
  }

  private static Map<String, List<String>> loadEnSegsByTuid() throws Exception {
    Path cmsUi = resolveCmsUiTmx();
    assertNotNull(cmsUi, "CmsUi.tmx not found");

    Map<String, List<String>> enSegsByTuid = new LinkedHashMap<>();
    Document doc = parse(cmsUi);
    NodeList tus = doc.getElementsByTagName("tu");
    for (int i = 0; i < tus.getLength(); i++) {
      Element tu = (Element) tus.item(i);
      String tuid = tu.getAttribute("tuid");
      if (!tuid.startsWith("perc.ui.publish.modern@")) {
        continue;
      }
      NodeList tuvs = tu.getElementsByTagName("tuv");
      for (int j = 0; j < tuvs.getLength(); j++) {
        Element tuv = (Element) tuvs.item(j);
        if (!"en-us".equals(normalizeLang(readXmlLang(tuv)))) {
          continue;
        }
        NodeList segs = tuv.getElementsByTagName("seg");
        if (segs.getLength() > 0) {
          enSegsByTuid
              .computeIfAbsent(tuid, k -> new ArrayList<>())
              .add(segs.item(0).getTextContent());
        }
      }
    }
    return enSegsByTuid;
  }

  private static void assertKeysRegistered(
      Map<String, List<String>> enSegsByTuid, List<String> expected) {
    Set<String> missing = new LinkedHashSet<>();
    List<String> mismatched = new ArrayList<>();
    List<String> duplicated = new ArrayList<>();
    for (String en : expected) {
      String tuid = "perc.ui.publish.modern@" + en;
      List<String> segs = enSegsByTuid.get(tuid);
      if (segs == null || segs.isEmpty()) {
        missing.add(tuid);
      } else {
        if (segs.size() > 1) {
          duplicated.add(tuid);
        }
        if (!en.equals(segs.get(0))) {
          mismatched.add(tuid + " -> " + segs.get(0));
        }
      }
    }
    assertTrue(missing.isEmpty(), "CmsUi.tmx missing delivery-panel keys: " + missing);
    assertTrue(
        duplicated.isEmpty(), "CmsUi.tmx has duplicate delivery-panel keys: " + duplicated);
    assertTrue(
        mismatched.isEmpty(), "CmsUi.tmx en-us seg differs from TS fallback: " + mismatched);
  }

  /**
   * Resolve {@code CmsUi.tmx} portably: prefer the test classpath resource (Maven surefire), then
   * fall back to common source-tree locations relative to the process working directory.
   */
  private static Path resolveCmsUiTmx() throws URISyntaxException {
    URL url = PSPublishDeliveryPanelKeysTmxTest.class.getResource("/i18n/CmsUi.tmx");
    if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
      Path p = Paths.get(url.toURI());
      if (Files.isRegularFile(p)) {
        return p;
      }
    }
    Path[] candidates =
        new Path[] {
          Paths.get("src", "main", "resources", "i18n", "CmsUi.tmx"),
          Paths.get("modules", "perc-i18n", "src", "main", "resources", "i18n", "CmsUi.tmx"),
        };
    for (Path c : candidates) {
      Path abs = c.toAbsolutePath().normalize();
      if (Files.isRegularFile(abs)) {
        return abs;
      }
    }
    return null;
  }

  private static String readXmlLang(Element tuv) {
    String lang = tuv.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang");
    if (lang == null || lang.isBlank()) {
      lang = tuv.getAttribute("xml:lang");
    }
    if (lang == null || lang.isBlank()) {
      lang = tuv.getAttribute("lang");
    }
    return lang != null ? lang : "";
  }

  private static String normalizeLang(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    return raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private static Document parse(Path f) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);
    factory.setIgnoringElementContentWhitespace(true);
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    var is = new InputSource(f.toUri().toASCIIString());
    is.setEncoding("UTF-8");
    return factory.newDocumentBuilder().parse(is);
  }
}
