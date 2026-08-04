/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.security.SecureStringUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestSecureStringUtils {

  private Path tempDir;

  private static final String[] resourcePaths =
      new String[] {
        "/Sites/",
        "/Assets/",
        "/rx_resources",
        "/sys_resources",
        "/cm/",
        "/web_resources",
        "/tmx/",
        "/services/",
        "/sessioncheck",
        "/webservices/",
        "/designwebservices/",
        "/content/",
        "/rest",
        "/v8",
        "/assembler/",
        "/contentlist",
        "/sitelist",
        "/login",
        "/logout",
        "/rxwebdav",
        "/ui/actionpage/panel",
        "/user/apps",
        "/publisher/",
        "/linkback/",
        "/servlet/",
        "/assembly/aa",
        "/contentui/aa",
        "/adf/",
        "/uploadAssetFile",
        "/textToImage/",
        "/Designer",
        "/Rhythmyx/"
      };

  @BeforeEach
  public void setup() throws Exception {
    tempDir = Files.createTempDirectory("perc-security-utils-tests");
    System.setProperty("rxdeploydir", tempDir.toAbsolutePath().toString());
  }

  @Test
  public void testRandomPassword() {
    String pwd = SecureStringUtils.generateRandomPassword();

    assertTrue(pwd != null);
  }

  @Test
  public void testWildStrings() {

    String testPath =
        SecureStringUtils.cleanWildPath(
            resourcePaths,
            "/Rhythmyx/cm%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5cwindows%5cwin.ini",
            "unit.test");
    assertNull(testPath);

    testPath = SecureStringUtils.cleanWildPath(resourcePaths, "/login", "unit.test");
    assertEquals("/login", testPath);

    testPath =
        SecureStringUtils.cleanWildPath(
            resourcePaths, "/cm/../sys_resources/css/test.css", "unit.test");
    assertEquals("/cm/../sys_resources/css/test.css", testPath);

    testPath =
        SecureStringUtils.cleanWildPath(
            resourcePaths, "/cm/../../sys_resources/css/test.css", "unit.test");
    assertNull(testPath);

    testPath = SecureStringUtils.cleanWildPath(resourcePaths, "favicon.ico", "unit.test");
    assertEquals("favicon.ico", testPath);

    testPath =
        SecureStringUtils.cleanWildPath(
            resourcePaths,
            "%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5c%2e%2e%5cwindows%5cwin.ini",
            "unit.test");
    assertNull(testPath);
  }

  @Test
  @Disabled
  public void testPwdDecryption() {
    String encPwd = "Qhmee/8dNs2SL0+499RqMv/1hoNxdAgdnyIOewLB7xrt5A==";
    // Add .key and .legacyKey in this dir to get decrypted pwd.
    String secureDir = "c:/test/secure/";
    try {
      String pwd = PSEncryptor.decryptString(secureDir, encPwd);
      assertTrue("mypass".equals(pwd));
      System.out.println(pwd);
    } catch (PSEncryptionException e) {
      assertNull(e);
    }
  }

  @Test
  public void testIsHTML() {
    assertFalse(SecureStringUtils.isHTML("x"));
    assertFalse(SecureStringUtils.isHTML(null));
    assertFalse(SecureStringUtils.isHTML(""));
    assertTrue(SecureStringUtils.isHTML("<div></div>"));
    assertTrue(SecureStringUtils.isHTML("<a href='#'>x</a>"));
    assertTrue(SecureStringUtils.isHTML("a&b"));
    assertTrue(SecureStringUtils.isHTML("\"quoted\""));
  }

  @Test
  public void testIsXML() {
    assertFalse(SecureStringUtils.isXML("x"));
    assertFalse(SecureStringUtils.isXML(null));
    assertFalse(SecureStringUtils.isXML(""));
    assertTrue(SecureStringUtils.isXML("<div></div>"));
    assertTrue(SecureStringUtils.isXML("<a href='#'>x</a>"));
    assertTrue(SecureStringUtils.isXML("a&b"));
  }

  @Test
  public void testSanitizeStringForLDAP() {
    assertNull(SecureStringUtils.sanitizeStringForLDAP(null, true));
    assertEquals("plain", SecureStringUtils.sanitizeStringForLDAP("plain", true));
    assertEquals("a\\2ab", SecureStringUtils.sanitizeStringForLDAP("a*b", true));
    assertEquals("a*b", SecureStringUtils.sanitizeStringForLDAP("a*b", false));
    assertEquals("a\\28b\\29c", SecureStringUtils.sanitizeStringForLDAP("a(b)c", true));
    assertEquals("a\\5cb", SecureStringUtils.sanitizeStringForLDAP("a\\b", true));
    assertEquals("a\\2fb", SecureStringUtils.sanitizeStringForLDAP("a/b", true));
    assertEquals("a\\00b", SecureStringUtils.sanitizeStringForLDAP("a\0b", true));
    // Non-ASCII → UTF-8 hex escapes (café = c3 a9 for é)
    assertEquals("caf\\c3\\a9", SecureStringUtils.sanitizeStringForLDAP("café", true));
  }

  @Test
  public void testSanitizeForJson() {
    assertNull(SecureStringUtils.sanitizeForJson(null));
    assertEquals("plain", SecureStringUtils.sanitizeForJson("plain"));
    // OWASP JS encoder escapes quotes, angle brackets, and backslashes
    String encoded = SecureStringUtils.sanitizeForJson("a'b\"c</script>\\");
    assertNotNull(encoded);
    // Each quote type must be escaped independently (do not AND the checks)
    assertFalse(encoded.contains("'"), "single quote must be escaped in JS/JSON encoding");
    assertFalse(encoded.contains("\""), "double quote must be escaped in JS/JSON encoding");
    assertTrue(encoded.contains("\\x27") || encoded.contains("\\'"));
  }

  @Test
  public void testUrlEncode() {
    assertNull(SecureStringUtils.urlEncode(null));
    assertEquals("plain", SecureStringUtils.urlEncode("plain"));
    // Space and reserved URI component chars are percent-encoded
    assertEquals("a%20b%2Fc%3Fd%3De%26f", SecureStringUtils.urlEncode("a b/c?d=e&f"));
    assertFalse(SecureStringUtils.urlEncode("<script>").contains("<"));
  }

  @Test
  public void testSanitizeStringForHTML() {
    assertNull(SecureStringUtils.sanitizeStringForHTML(null));
    assertEquals("hello", SecureStringUtils.sanitizeStringForHTML("hello"));
    String encoded = SecureStringUtils.sanitizeStringForHTML("<script>alert(1)</script>");
    assertFalse(encoded.contains("<script>"));
    assertTrue(encoded.contains("&lt;") || encoded.contains("&#"));
  }

  @Test
  public void testCrazyPreviewUrl() {
    try {
      String testUrl =
          "http://ts-trees4.percussion.local:9992/Sites/www.bw.edu/student-life/organizations/?percmobilepreview=true&clear=function(){this.length=0}&first=function(){return%20this[0]}&last=function(){return%20this[this.length-1]}&flatten=function(){for(var%20t=this.length,e=[],n=0;n%3Ct;n++)this[n]instanceof%20Array?e=e.concat(this[n]):e.push(this[n]);return%20e}&each=function(t){var%20e=this.length;if(%22function%22!=typeof%20t)throw%22Array.each%20requires%20first%20argument%20to%20be%20a%20function%22;for(var%20n=arguments[1],i=0;i%3Ce;i++)i%20in%20this&&t.call(n,this[i],i,this);return%20null}&include=function(t){return%20this.length,this.indexOf(t)%3E=0}";
      URI uri = new URI(SecureStringUtils.stripUrlParams(testUrl));
      assertNotNull(uri);
      assertEquals("ts-trees4.percussion.local", uri.getHost());
      assertEquals(9992, uri.getPort());
      assertEquals("http", uri.getScheme());
      assertEquals("/Sites/www.bw.edu/student-life/organizations/", uri.getPath());

    } catch (URISyntaxException e) {
      assertNull(e);
    }
  }

  @Test
  public void testStripNoQueryParams() {
    assertEquals(
        "http://somesite.edu/noparams",
        SecureStringUtils.stripUrlParams("http://somesite.edu/noparams"));
    assertEquals(
        "http://somesite.edu/noparams",
        SecureStringUtils.stripUrlParams("http://somesite.edu/noparams#test"));
  }

  @Test
  public void testSanitizeStringForSQLStatementDerby() {
    assertEquals(
        "http://somesite.edu/noparams",
        SecureStringUtils.sanitizeStringForSQLStatement(
            "http://somesite.edu/noparams", SecureStringUtils.DatabaseType.DERBY));
  }

  @Test
  public void testSanitizeStringForSQLStatementMySql() {
    assertEquals(
        "http://somesite.edu/noparams",
        SecureStringUtils.sanitizeStringForSQLStatement(
            "http://somesite.edu/noparams", SecureStringUtils.DatabaseType.MYSQL));
  }

  @Test
  public void testSanitizeStringForSQLStatementSqlServer() {
    assertEquals(
        "http://somesite.edu/noparams",
        SecureStringUtils.sanitizeStringForSQLStatement(
            "http://somesite.edu/noparams", SecureStringUtils.DatabaseType.MSSQL));
  }

  @Test
  public void testContainsXSSChars() {

    // naked
    assertTrue(
        SecureStringUtils.containsXSSChars("/Sites/mysite/<script>alert('test');</script>/file"));

    // url encoded
    assertTrue(
        SecureStringUtils.containsXSSChars(
            "%2FSites%2Fmysite%2F%3Cscript%3Ealert(%27test%27)%3B%3C%2Fscript%3E%2Ffile"));

    // html encoded
    assertTrue(
        SecureStringUtils.containsXSSChars(
            "/Sites/mysite/&lt;script&gt;alert(&#39;test&#39;);&lt;/script&gt;/file"));

    // xml numeric
    assertTrue(
        SecureStringUtils.containsXSSChars(
            "/Sites/mysite/&#60;script&#62;alert('test');&#60;/script&#62;/file"));
  }

  @Test
  public void testSanitizeFileName() {
    StringBuilder t =
        new StringBuilder(SecureStringUtils.sanitizeFileName("this has   some spaces.png"));
    assertEquals("this-has-some-spaces.png", t.toString());
    t = new StringBuilder(SecureStringUtils.sanitizeFileName("this-has-dashes.png"));
    assertEquals("this-has-dashes.png", t.toString());
    t = new StringBuilder(SecureStringUtils.sanitizeFileName("Latin_alphabet_Ħħ.png"));
    assertEquals("Latin_alphabet_Ħħ.png", t.toString());
    for (int i = 0; i < 500; i++) {
      t.append(i);
    }
    assertTrue(t.length() > SecureStringUtils.MAX_FILENAME_LEN);
    t = new StringBuilder(SecureStringUtils.sanitizeFileName(t.toString()));
    assertTrue(t.length() <= SecureStringUtils.MAX_FILENAME_LEN);
  }

  @Test
  public void testValidString() {
    assertFalse(SecureStringUtils.isValidString("<script>alert(111);</script>"));
    assertTrue(SecureStringUtils.isValidString("somestring"));
    assertTrue(
        SecureStringUtils.isValidString(
            "se-inicia-la-postulación-al-fondo-concursable-para-apoyar-tu-practica-en-el-extranjero"));
  }

  @Test
  public void testUnescapeXML() {

    String test =
        "&amp;lt;?xml version=&amp;quot;1.0&amp;quot; encoding=&amp;quot;UTF-8&amp;quot;"
            + " standalone=&amp;quot;yes&amp;quot;?&amp;gt;\n"
            + "&amp;lt;RegionBranches&amp;gt;\n"
            + "    &amp;lt;regionWidgetAssociations&amp;gt;\n"
            + "        &amp;lt;regionWidget&amp;gt;\n"
            + "            &amp;lt;id&amp;gt;content&amp;lt;/id&amp;gt;\n"
            + "            &amp;lt;regionId&amp;gt;content&amp;lt;/regionId&amp;gt;\n"
            + "            &amp;lt;widgetItems&amp;gt;\n"
            + "                &amp;lt;widgetItem&amp;gt;\n"
            + "                    &amp;lt;cssProperties/&amp;gt;\n"
            + "                   "
            + " &amp;lt;definitionId&amp;gt;percBlogPost&amp;lt;/definitionId&amp;gt;\n"
            + "                    &amp;lt;id&amp;gt;830717652&amp;lt;/id&amp;gt;\n"
            + "                    &amp;lt;properties/&amp;gt;\n"
            + "                &amp;lt;/widgetItem&amp;gt;\n"
            + "            &amp;lt;/widgetItems&amp;gt;\n"
            + "        &amp;lt;/regionWidget&amp;gt;\n"
            + "        &amp;lt;regionWidget&amp;gt;\n"
            + "            &amp;lt;id&amp;gt;header&amp;lt;/id&amp;gt;\n"
            + "            &amp;lt;regionId&amp;gt;header&amp;lt;/regionId&amp;gt;\n"
            + "            &amp;lt;widgetItems&amp;gt;\n"
            + "                &amp;lt;widgetItem&amp;gt;\n"
            + "                    &amp;lt;cssProperties/&amp;gt;\n"
            + "                   "
            + " &amp;lt;definitionId&amp;gt;percTitle&amp;lt;/definitionId&amp;gt;\n"
            + "                    &amp;lt;id&amp;gt;533037133&amp;lt;/id&amp;gt;\n"
            + "                    &amp;lt;properties/&amp;gt;\n"
            + "                &amp;lt;/widgetItem&amp;gt;\n"
            + "            &amp;lt;/widgetItems&amp;gt;\n"
            + "        &amp;lt;/regionWidget&amp;gt;\n"
            + "    &amp;lt;/regionWidgetAssociations&amp;gt;\n"
            + "    &amp;lt;regions&amp;gt;\n"
            + "        &amp;lt;region&amp;gt;\n"
            + "            &amp;lt;regionId&amp;gt;header&amp;lt;/regionId&amp;gt;\n"
            + "            &amp;lt;attributes/&amp;gt;\n"
            + "            &amp;lt;children&amp;gt;\n"
            + "                &amp;lt;code&amp;gt;\n"
            + "                   "
            + " &amp;lt;templateCode&amp;gt;#region(&amp;quot;header&amp;quot;,&amp;quot;&amp;quot;,&amp;quot;&amp;quot;,&amp;quot;&amp;quot;,&amp;quot;&amp;quot;)&amp;lt;/templateCode&amp;gt;\n"
            + "                &amp;lt;/code&amp;gt;\n"
            + "            &amp;lt;/children&amp;gt;\n"
            + "        &amp;lt;/region&amp;gt;\n"
            + "        &amp;lt;region&amp;gt;\n"
            + "            &amp;lt;regionId&amp;gt;content&amp;lt;/regionId&amp;gt;\n"
            + "            &amp;lt;attributes/&amp;gt;\n"
            + "            &amp;lt;children&amp;gt;\n"
            + "                &amp;lt;code&amp;gt;\n"
            + "                   "
            + " &amp;lt;templateCode&amp;gt;#region(&amp;quot;content&amp;quot;,&amp;quot;&amp;quot;,&amp;quot;&amp;quot;,&amp;quot;&amp;quot;,&amp;quot;&amp;quot;)&amp;lt;/templateCode&amp;gt;\n"
            + "                &amp;lt;/code&amp;gt;\n"
            + "            &amp;lt;/children&amp;gt;\n"
            + "        &amp;lt;/region&amp;gt;\n"
            + "    &amp;lt;/regions&amp;gt;\n"
            + "&amp;lt;/RegionBranches&amp;gt;\n";
    String t2 = test.replaceAll("&amp;", "&");
    System.out.println(StringEscapeUtils.UNESCAPE_XML.translate(t2));
    assertFalse(t2.contains("&amp;lt;"));
  }
}
