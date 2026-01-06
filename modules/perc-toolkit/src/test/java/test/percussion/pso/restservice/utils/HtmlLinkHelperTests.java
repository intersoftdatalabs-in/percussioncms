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
package test.percussion.pso.restservice.utils;

<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.*;

=======
>>>>>>> development-8.1.x
import com.percussion.pso.restservice.utils.HtmlLinkHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
<<<<<<< HEAD
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
=======
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;
>>>>>>> development-8.1.x
import org.xml.sax.SAXException;

public class HtmlLinkHelperTests {

  @Test
  public void testConvertToAbsoluteLink1() throws URISyntaxException, MalformedURLException {

    String test = HtmlLinkHelper.convertToAbsoluteLink("http://www.somedomain.com/", "image1.jpg");
<<<<<<< HEAD
    Assertions.assertEquals("http://www.somedomain.com/image1.jpg", test);
=======
    Assert.assertEquals("http://www.somedomain.com/image1.jpg", test);
>>>>>>> development-8.1.x
  }

  @Test
  public void testDotRelative() throws MalformedURLException, URISyntaxException {

    String test =
        HtmlLinkHelper.convertToAbsoluteLink("http://www.somedomain.com/", "./image1.jpg");
<<<<<<< HEAD
    Assertions.assertEquals("http://www.somedomain.com/image1.jpg", test);
=======
    Assert.assertEquals("http://www.somedomain.com/image1.jpg", test);
>>>>>>> development-8.1.x
  }

  @Test
  public void testRootAbsoluteFile() throws MalformedURLException, URISyntaxException {

    String test = HtmlLinkHelper.convertToAbsoluteLink("http://www.somedomain.com/", "/image1.jpg");
<<<<<<< HEAD
    Assertions.assertEquals("http://www.somedomain.com/image1.jpg", test);
=======
    Assert.assertEquals("http://www.somedomain.com/image1.jpg", test);
>>>>>>> development-8.1.x
  }

  @Test
  public void testRelativeDir() throws MalformedURLException, URISyntaxException {

    String test = HtmlLinkHelper.convertToAbsoluteLink("http://www.somedomain.com/", "images");
<<<<<<< HEAD
    Assertions.assertEquals("http://www.somedomain.com/images", test);
=======
    Assert.assertEquals("http://www.somedomain.com/images", test);
>>>>>>> development-8.1.x
  }

  @Test
  public void testRelativeDirTrailingSlash() throws MalformedURLException, URISyntaxException {

    String test = HtmlLinkHelper.convertToAbsoluteLink("http://www.somedomain.com/", "images/");
<<<<<<< HEAD
    Assertions.assertEquals("http://www.somedomain.com/images/", test);
=======
    Assert.assertEquals("http://www.somedomain.com/images/", test);
>>>>>>> development-8.1.x
  }

  @Test
  public void testRelativeDirTrailingSlashFile() throws MalformedURLException, URISyntaxException {

    String test =
        HtmlLinkHelper.convertToAbsoluteLink("http://www.somedomain.com/", "images/file1.docx");
<<<<<<< HEAD
    Assertions.assertEquals("http://www.somedomain.com/images/file1.docx", test);
=======
    Assert.assertEquals("http://www.somedomain.com/images/file1.docx", test);
>>>>>>> development-8.1.x
  }

  @Test
  public void testRelativeDirTrailingSlashFileBookmark()
      throws MalformedURLException, URISyntaxException {

    String test =
        HtmlLinkHelper.convertToAbsoluteLink(
            "http://www.somedomain.com/", "images/file1.html#234.78");
<<<<<<< HEAD
    Assertions.assertEquals("http://www.somedomain.com/images/file1.html#234.78", test);
=======
    Assert.assertEquals("http://www.somedomain.com/images/file1.html#234.78", test);
>>>>>>> development-8.1.x
  }

  @Test
  public void testGetBaseLink() throws URISyntaxException, MalformedURLException {

    String test =
        HtmlLinkHelper.getBaseLink("http://www.somewhere.gov/news/about.html?month=03&year=2011");

<<<<<<< HEAD
    Assertions.assertEquals("http://www.somewhere.gov/", test);
=======
    Assert.assertEquals("http://www.somewhere.gov/", test);
>>>>>>> development-8.1.x
  }

  @Test
  public void testBaseWithPort() throws MalformedURLException, URISyntaxException {
    String test =
        HtmlLinkHelper.getBaseLink(
            "http://www.somewhere.gov:8793/news/about.html?month=03&year=2011");

<<<<<<< HEAD
    Assertions.assertEquals("http://www.somewhere.gov:8793/", test);
  }

  @Test
  @Disabled("Test is failing") // TODO: Fix me
  public void testFixLinksRandom()
      throws IOException,
          TransformerException,
          ParserConfigurationException,
          SAXException,
=======
    Assert.assertEquals("http://www.somewhere.gov:8793/", test);
  }

  @Test
  @Ignore("Test is failing") // TODO: Fix me
  public void testFixLinksRandom()
      throws IOException, TransformerException, ParserConfigurationException, SAXException,
>>>>>>> development-8.1.x
          URISyntaxException {

    InputStream is = this.getClass().getResourceAsStream("randomcar.html");

    Reader reader = new BufferedReader(new InputStreamReader(is));
    StringBuilder builder = new StringBuilder();
    char[] buffer = new char[8192];
    int read;
    while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
      builder.append(buffer, 0, read);
    }

    String n =
        HtmlLinkHelper.convertLinksToAbsolute(
            "https://www.percussion.com/randomcar.html", builder.toString());

<<<<<<< HEAD
    Assertions.assertEquals(builder.toString(), n);
=======
    Assert.assertEquals(builder.toString(), n);
>>>>>>> development-8.1.x
  }
}
