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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Guards for issue #7131: {@code zh-tw} segments shipped as Simplified
 * Chinese, and "Take Down" rendered literally instead of the product sense
 * ("remove from the live site").
 *
 * <p>The {@code zh-tw} catalog was repaired by converting every
 * byte-identical {@code zh-cn} / {@code zh-tw} pair with OpenCC
 * {@code s2twp} (see {@code scripts/repair_zh_tw_takedown.py}). This test
 * fails when a {@code zh-tw} segment still equals its {@code zh-cn} source
 * <em>and</em> contains a Simplified-only character, which is exactly what
 * a machine back-fill reintroduces (the engine copies Simplified script
 * into both tags).
 *
 * <p>{@link #SIMPLIFIED_ONLY} is a curated allowlist derived from the
 * pre-fix catalog: every character OpenCC {@code s2twp} rewrites at the
 * single-character level, minus every character that still appears in the
 * repaired {@code zh-tw} corpus (so context-dependent survivals such as
 * {@code 台} in {@code 台灣}, and script-neutral characters, can never
 * false-positive). Measured coverage on the pre-fix catalog: 2,387 of the
 * 2,913 converted pairs contain at least one listed character; the
 * remainder differ only at the phrase level (e.g. {@code 查看} to
 * {@code 檢視}) where the individual characters are valid in both scripts.
 * Phrase-only regressions are therefore not caught here — they remain
 * covered by review of the repair script's report.
 *
 * <p>The Take Down guard pins the exact bad literals removed from
 * {@code perc.ui.finder.view@Take Down} and
 * {@code perc.ui.activity.gadget@Take Downs}: if a future back-fill
 * reintroduces any of them, the build fails. The pinned replacements live
 * in {@code scripts/i18n_glossary.py}, which both translate CLIs consult
 * before the engine.
 */
public class PSZhTwTraditionalTmxTest {

  /** Canonical TMX bundles covered by the guards. */
  private static final List<String> CATALOG_FILES =
      List.of("CmsUi.tmx", "SystemResources.tmx", "DeveloperUi.tmx");

  /**
   * Curated Simplified-only characters (see class JavaDoc for derivation).
   * A {@code zh-tw} segment identical to its {@code zh-cn} source must not
   * contain any of these.
   */
  private static final String SIMPLIFIED_ONLY_CHARS =
      "与专业丢两严临为么义乐书于产仅仪们价优会传侧储册冲决况减几凭击划则创别办务动区协历参双变号后吗启员响团围图场坏处备复够头夹实审宽导尝属币帐帜带帮并库应开弃张弹当录径态总悬户执扩护报担拟拥损换据数旧显暂术机权来杰极构栈栏树样档检欢毕汇没测浏满滤灯灵状环现电监盖盘着码础离种积称筛签简红级纯线组细织终绍经绑给络绝继续维缀缓缩网职胜脚节范获虚补装见规视览触计订认讨议记讲许论设访评词译试话该详说谅谢负财账货购贴资赖赛跃踪转轻较输边达迁过运还这进远连适递邮采释里针钟钥钮链销锁锚键镜长闭闲间阅队阶际陆险随隐页顶顺须预领频颜额风饰馆验骤黄齐";

  private static final Set<Integer> SIMPLIFIED_ONLY =
      SIMPLIFIED_ONLY_CHARS.codePoints().boxed().collect(Collectors.toUnmodifiableSet());

  /**
   * Exact bad literals removed by the #7131 repair, keyed by tuid. A future
   * back-fill must never reintroduce these (glossary:
   * {@code scripts/i18n_glossary.py}).
   *
   * <p>Two old literals are deliberately absent: bare {@code Снять} (ru)
   * and {@code Зняти} (uk) are substrings of their pinned replacements
   * ({@code Снять с публикации} / {@code Зняти з публікації}), so a
   * substring guard cannot pin them — the glossary hook owns those.
   */
  private static final Map<String, List<String>> TAKE_DOWN_KNOWN_BAD = Map.of(
      "perc.ui.finder.view@Take Down",
      List.of("Notieren", "Derribar", "Démonter", "记录下来", "Prendere nota",
          "Zapisać", "Devirmek", "Neerhalen", "Derrubar", "降ろす",
          "إنزال", "টেক ডাউন", "नीचे करें", "Взять дауны",
          "Зняти Даунс", "Zdejmij upadki", "Düşüşler", "Ta ned", "Ta ner",
          "Ta Downs", "Abattre", "Abbattimenti",
          "خذ داونز", "קח דאונס", "לְהוֹרִיד", "उतारो"),
      "perc.ui.activity.gadget@Take Downs",
      List.of("Derribar", "Démonter", "记录下来", "击倒",
          "Takedowns", "Neerhalen", "Derrubar", "テイクダウン", "Взять дауны",
          "Abattre", "Abbattimenti", "خذ داونز", "קח דאונס", "उतारो",
          "টেক ডাউন", "Ta Downs", "Zdejmij upadki", "Düşüşler",
          "Зняти Даунс"));

  /**
   * Regression guard for issue #7131: no {@code zh-tw} segment may equal its
   * {@code zh-cn} source while containing a Simplified-only character.
   */
  @Test
  public void zhTwSegments_haveTraditionalScript() throws Exception {
    List<String> offenders = new ArrayList<>();
    for (String name : CATALOG_FILES) {
      Path f = resolveCatalogTmx(name);
      assertTrue(f != null, name + " not found");
      Document doc = parse(f);
      NodeList tus = doc.getElementsByTagName("tu");
      for (int i = 0; i < tus.getLength(); i++) {
        Element tu = (Element) tus.item(i);
        Map<String, String> segs = segsByLang(tu);
        String cn = segs.get("zh-cn");
        String tw = segs.get("zh-tw");
        if (cn != null && tw != null && cn.equals(tw) && containsSimplifiedOnly(tw)) {
          offenders.add(name + " :: " + tu.getAttribute("tuid"));
        }
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "zh-tw segments identical to zh-cn with Simplified-only characters "
            + "(re-run scripts/repair_zh_tw_takedown.py): " + offenders);
  }

  /**
   * Regression guard for issue #7131: the Take Down TUs must not contain the
   * literal mistranslations the repair removed.
   */
  @Test
  public void takeDownSegments_useProductSense() throws Exception {
    List<String> offenders = new ArrayList<>();
    Path f = resolveCatalogTmx("CmsUi.tmx");
    assertTrue(f != null, "CmsUi.tmx not found");
    Document doc = parse(f);
    Map<String, Element> byTuid = new LinkedHashMap<>();
    NodeList tus = doc.getElementsByTagName("tu");
    for (int i = 0; i < tus.getLength(); i++) {
      Element tu = (Element) tus.item(i);
      byTuid.putIfAbsent(tu.getAttribute("tuid"), tu);
    }
    TAKE_DOWN_KNOWN_BAD.forEach((tuid, badLiterals) -> {
      Element tu = byTuid.get(tuid);
      if (tu == null) {
        offenders.add("CmsUi.tmx :: " + tuid + " (missing <tu>)");
        return;
      }
      segsByLang(tu).forEach((lang, seg) -> {
        if ("en-us".equals(lang) || "lou".equals(lang)) {
          return;
        }
        for (String bad : badLiterals) {
          if (seg != null && seg.contains(bad)) {
            offenders.add("CmsUi.tmx :: " + tuid + " [" + lang + "]"
                + " reintroduces " + bad);
          }
        }
      });
    });
    assertTrue(offenders.isEmpty(), "Take Down mistranslations reintroduced: " + offenders);
  }

  private static boolean containsSimplifiedOnly(String seg) {
    return seg.codePoints().anyMatch(SIMPLIFIED_ONLY::contains);
  }

  private static Map<String, String> segsByLang(Element tu) {
    Map<String, String> segs = new LinkedHashMap<>();
    NodeList tuvs = tu.getElementsByTagName("tuv");
    for (int j = 0; j < tuvs.getLength(); j++) {
      Element tuv = (Element) tuvs.item(j);
      segs.put(normalizeLang(readXmlLang(tuv)),
          tuvs.item(j).getTextContent());
    }
    return segs;
  }

  /**
   * Resolve a canonical bundle: prefer the test classpath resource (Maven
   * surefire), then fall back to common source-tree locations relative to
   * the process working directory.
   */
  private static Path resolveCatalogTmx(String name) throws URISyntaxException {
    URL url = PSZhTwTraditionalTmxTest.class.getResource("/i18n/" + name);
    if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
      Path p = Paths.get(url.toURI());
      if (Files.isRegularFile(p)) {
        return p;
      }
    }
    Path[] candidates =
        new Path[] {
          Paths.get("src", "main", "resources", "i18n", name),
          Paths.get("modules", "perc-i18n", "src", "main", "resources", "i18n", name),
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
