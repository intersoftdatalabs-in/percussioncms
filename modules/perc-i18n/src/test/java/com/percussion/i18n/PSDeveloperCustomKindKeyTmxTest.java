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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Pins the Spanish rendering of the Developer kind-chrome {@code Custom} string for issue
 * #7230 (follow-up to the Erlang review of PR #7223).
 *
 * <p>{@code perc.ui.developer@Custom} labels the <em>custom</em> kind of a search, view, or
 * API-key preset (standard | custom | user) — the adjective sense "customized", not the noun
 * "custom/habit". The pipeline backfill for #7138 rendered the wrong sense ({@code
 * Costumbre}); the same English source already ships as {@code Personalizado} for {@code
 * perc.ui.dashboard@Custom}, {@code perc.ui.template.layout@Custom}, and {@code
 * com.percussion.cx.PSFolderDialog@Custom}, and the sibling Developer TUVs agree with the
 * adjective sense (ar {@code مخصص}, nl {@code Aangepast}, pt {@code Personalizado}).
 *
 * <p>The cache-agreement test below guards the #7116 invariant for this key: {@code
 * scripts/cache/i18n_translate.json} is keyed by {@code sha256(target + "\0" + source)}, so
 * the single shared {@code es + "Custom"} entry must equal the shipped {@code es} segment —
 * otherwise a pipeline re-run would write the stale wrong-sense value back over the TMX.
 *
 * <p>Issue #7241 extends the pin to fr / de / it across all four {@code Custom} TUs
 * (Developer kind-chrome plus the three precedent TUs): {@code Coutume} → {@code
 * Personnalisé}, {@code Brauch} → {@code Benutzerdefiniert}, {@code Costume} → {@code
 * Personalizzato}, with the shared cache entries synced TMX-leads-cache-follows.
 *
 * <p>Issue #7252 (hi slice) extends the pin to Hindi across all four {@code Custom} TUs:
 * {@code रिवाज़} ("tradition/ritual", noun sense) → {@code कस्टम}, aligning the three
 * corrected TUs with the {@code perc.ui.dashboard@Custom} TUV that already ships the
 * transliteration, with the shared {@code hi + "Custom"} cache entry synced
 * TMX-leads-cache-follows. Remaining locale (he) is left for a per-group
 * native-confirmation follow-up (#7289); zh-cn / zh-tw (#7284), ru / uk (#7285), pl
 * (#7286), tr (#7287), and nb / sv (#7288) are pinned below.
 *
 * <p>Issues #7284 and #7285 extend the pin to zh-cn / zh-tw and ru / uk respectively
 * across all four {@code Custom} TUs, each following the stem-consistency invariant every
 * corrected locale already satisfies: the bare {@code Custom} uses the stem of that
 * locale's {@code perc.ui.widget.config.gadget@Custom Widgets} rendering (es {@code
 * Widgets personalizados} / {@code Personalizado}, fr {@code Widgets personnalisés} /
 * {@code Personnalisé}, de {@code Benutzerdefinierte Widgets} / {@code
 * Benutzerdefiniert}, it {@code Widget personalizzati} / {@code Personalizzato}, hi
 * {@code कस्टम विजेट} / {@code कस्टम}, zh-cn {@code 自定义小部件} / {@code 自定义}).
 * So ru ships {@code Пользовательский} (singular of the in-product {@code
 * Пользовательские виджеты:}) and uk ships {@code Спеціальний} (singular of the
 * in-product {@code Спеціальні віджети:}); the latter also keeps the standard | custom
 * | user kind-chrome distinct, where the candidate {@code Користувацький} ("user's")
 * would collide with the sibling "user" kind.
 *
 * <p>Issue #7286 extends the pin to pl across all four {@code Custom} TUs, following the
 * same stem-consistency invariant: the bare {@code Custom} uses the stem of the locale's
 * in-product {@code perc.ui.widget.config.gadget@Custom Widgets} rendering — pl {@code
 * Niestandardowe widżety:} / {@code Niestandardowy} (masculine singular adjective sense
 * "customized", as proposed on the issue). This keeps the standard | custom | user
 * kind-chrome distinct (siblings ship {@code Standard} / {@code Użytkownik}).
 *
 * <p>Issue #7287 extends the pin to tr across all four {@code Custom} TUs, following the
 * same stem-consistency invariant: the bare {@code Custom} uses the stem of the locale's
 * in-product {@code perc.ui.widget.config.gadget@Custom Widgets} rendering — tr {@code
 * Özel widget'lar:} / {@code Özel} (adjective sense "custom/special", as proposed on the
 * issue). {@code Özel} is the product's own established Turkish rendering of the adjective
 * sense (also ships in-product for {@code Özel Widget Tepsisi} and {@code Özel anahtar}),
 * not machine-grade text; the wrong-sense noun {@code Gelenek} ("tradition") never appears
 * as a Turkish rendering of any other {@code Custom}-sourced string.
 *
 * <p>Issue #7288 extends the pin to nb / sv across all four {@code Custom} TUs,
 * following the same stem-consistency invariant: the bare {@code Custom} uses the stem
 * of the locale's in-product {@code Custom Widgets} rendering — nb {@code Egendefinerte
 * widgets:} / {@code Egendefinert}, sv {@code Anpassade widgetar:} / {@code Anpassad}
 * (adjective sense "customized", as proposed on the issue). Neither target is
 * machine-grade text: each is the product's own established rendering of the adjective
 * sense, and the wrong-sense nouns ({@code Skikk} "custom/habit", {@code Beställnings}
 * "order") never appear as an nb/sv rendering of any other {@code Custom}-sourced
 * string. The kind-chrome stays distinct (nb {@code Standard} / {@code Egendefinert} /
 * {@code Bruker}, sv {@code Standard} / {@code Anpassad} / {@code Användare}).
 */
public class PSDeveloperCustomKindKeyTmxTest {

  private static final String TUID = "perc.ui.developer@Custom";

  private static final String SOURCE = "Custom";

  /** Adjective sense ("customized"), matching the dashboard / template-layout / folder-dialog
   * precedent TUs and the ar / nl / pt sibling TUVs. */
  private static final String EXPECTED_ES = "Personalizado";

  /** Issue #7241 ([7230-residual]): the same wrong-sense noun rendering shipped for fr / de /
   * it in all four {@code Custom} TUs. Corrected adjective-sense ("customized") targets. */
  private static final String EXPECTED_FR = "Personnalisé";

  private static final String EXPECTED_DE = "Benutzerdefiniert";

  private static final String EXPECTED_IT = "Personalizzato";

  /**
   * Issue #7252 (hi slice of the [7241-residual]): the same wrong-sense noun rendering
   * ({@code रिवाज़}, "tradition/ritual") shipped for hi in three of the four {@code Custom}
   * TUs. Corrected target is the transliteration {@code कस्टम} — the standard Hindi-UI
   * rendering of the adjective sense ("customized") — already shipped by the fourth TU
   * ({@code perc.ui.dashboard@Custom}), so this slice is an in-product alignment.
   */
  private static final String EXPECTED_HI = "कस्टम";

  /**
   * Issue #7284 (zh-cn/zh-tw slice of the [7252-residual]): the same wrong-sense noun
   * rendering ({@code 风俗} / {@code 風俗}, "custom/habit") shipped for zh-cn / zh-tw in all
   * four {@code Custom} TUs. Corrected targets are the standard adjective-sense ("customized")
   * renderings: Simplified {@code 自定义}, Traditional {@code 自訂}.
   */
  private static final String EXPECTED_ZH_CN = "自定义";

  private static final String EXPECTED_ZH_TW = "自訂";

  /**
   * Issue #7285 (ru/uk slice of the [7252-residual]): ru shipped the wrong-sense noun
   * {@code Обычай} ("custom/habit") and uk shipped untranslated {@code Custom} in all four
   * {@code Custom} TUs. Corrected targets are the adjective-sense ("customized") singulars of
   * each locale's in-product {@code Custom Widgets} rendering (see class Javadoc for the
   * stem-consistency invariant): ru {@code Пользовательский}, uk {@code Спеціальний}.
   */
  private static final String EXPECTED_RU = "Пользовательский";

  private static final String EXPECTED_UK = "Спеціальний";

  /**
   * Issue #7286 (pl slice of the [7252-residual]): pl shipped the wrong-sense noun
   * {@code Zwyczaj} ("custom/habit/tradition") in all four {@code Custom} TUs. Corrected
   * target is the adjective-sense ("customized") masculine singular {@code Niestandardowy} —
   * the stem of the locale's in-product {@code Custom Widgets} rendering (see class Javadoc
   * for the stem-consistency invariant).
   */
  private static final String EXPECTED_PL = "Niestandardowy";

  /**
   * Issue #7287 (tr slice of the [7252-residual]): tr shipped the wrong-sense noun
   * {@code Gelenek} ("tradition") in all four {@code Custom} TUs. Corrected target is the
   * adjective-sense ("custom/special") {@code Özel} — the stem of the locale's in-product
   * {@code Custom Widgets} rendering (see class Javadoc for the stem-consistency
   * invariant).
   */
  private static final String EXPECTED_TR = "Özel";

  /**
   * Issue #7288 (nb/sv slice of the [7252-residual]): nb shipped the wrong-sense noun
   * {@code Skikk} ("custom/habit") and sv shipped {@code Beställnings} ("order" sense) in
   * all four {@code Custom} TUs. Corrected targets are the adjective-sense ("customized")
   * singulars — the stems of each locale's in-product {@code Custom Widgets} rendering
   * (see class Javadoc for the stem-consistency invariant): nb {@code Egendefinert}, sv
   * {@code Anpassad}.
   */
  private static final String EXPECTED_NB = "Egendefinert";

  private static final String EXPECTED_SV = "Anpassad";

  /**
   * Every {@code Custom}-sourced TU (Developer kind-chrome plus the three precedent TUs from
   * #7230). Each TU lives in its canonical TMX file.
   */
  private static final Map<String, String> CUSTOM_TUID_TO_TMX =
      Map.of(
          "perc.ui.developer@Custom", "DeveloperUi.tmx",
          "perc.ui.dashboard@Custom", "CmsUi.tmx",
          "perc.ui.template.layout@Custom", "CmsUi.tmx",
          "com.percussion.cx.PSFolderDialog@Custom", "SystemResources.tmx");

  @Test
  public void customKindKey_isRegisteredWithEnglishSource() throws Exception {
    Map<String, String> segs = loadSegs(TUID);
    assertNotNull(segs, "DeveloperUi.tmx missing <tu> " + TUID);
    assertEquals(SOURCE, segs.get("en-us"), TUID + " en-us seg differs from TS fallback");
  }

  @Test
  public void customKindKey_spanishUsesAdjectiveSense() throws Exception {
    Map<String, String> segs = loadSegs(TUID);
    assertNotNull(segs, "DeveloperUi.tmx missing <tu> " + TUID);
    assertEquals(
        EXPECTED_ES,
        segs.get("es"),
        TUID + " es seg must use the adjective sense (customized), not the noun (custom/habit)");
  }

  @Test
  public void customKindKey_frenchGermanItalianUseAdjectiveSense() throws Exception {
    Map<String, String> expected =
        Map.of("fr", EXPECTED_FR, "de", EXPECTED_DE, "it", EXPECTED_IT);
    List<String> offenders = new ArrayList<>();
    for (Map.Entry<String, String> tu : CUSTOM_TUID_TO_TMX.entrySet()) {
      Map<String, String> segs = loadSegs(tu.getKey(), tu.getValue());
      assertNotNull(segs, tu.getValue() + " missing <tu> " + tu.getKey());
      for (Map.Entry<String, String> want : expected.entrySet()) {
        if (!want.getValue().equals(segs.get(want.getKey()))) {
          offenders.add(
              tu.getKey()
                  + " ["
                  + want.getKey()
                  + "]="
                  + segs.get(want.getKey()));
        }
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "Custom TUVs must use the adjective sense (customized), not the noun (custom/habit): "
            + offenders);
  }

  @Test
  public void customKindKey_hindiUsesAdjectiveSense() throws Exception {
    List<String> offenders = new ArrayList<>();
    for (Map.Entry<String, String> tu : CUSTOM_TUID_TO_TMX.entrySet()) {
      Map<String, String> segs = loadSegs(tu.getKey(), tu.getValue());
      assertNotNull(segs, tu.getValue() + " missing <tu> " + tu.getKey());
      if (!EXPECTED_HI.equals(segs.get("hi"))) {
        offenders.add(tu.getKey() + " [hi]=" + segs.get("hi"));
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "Custom TUVs must use the adjective sense (customized कस्टम), not the noun (रिवाज़): "
            + offenders);
  }

  @Test
  public void customKindKey_chineseUsesAdjectiveSense() throws Exception {
    Map<String, String> expected = Map.of("zh-cn", EXPECTED_ZH_CN, "zh-tw", EXPECTED_ZH_TW);
    List<String> offenders = new ArrayList<>();
    for (Map.Entry<String, String> tu : CUSTOM_TUID_TO_TMX.entrySet()) {
      Map<String, String> segs = loadSegs(tu.getKey(), tu.getValue());
      assertNotNull(segs, tu.getValue() + " missing <tu> " + tu.getKey());
      for (Map.Entry<String, String> want : expected.entrySet()) {
        if (!want.getValue().equals(segs.get(want.getKey()))) {
          offenders.add(
              tu.getKey()
                  + " ["
                  + want.getKey()
                  + "]="
                  + segs.get(want.getKey()));
        }
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "Custom TUVs must use the adjective sense (customized), not the noun (custom/habit): "
            + offenders);
  }

  @Test
  public void customKindKey_russianUkrainianUseAdjectiveSense() throws Exception {
    Map<String, String> expected = Map.of("ru", EXPECTED_RU, "uk", EXPECTED_UK);
    List<String> offenders = new ArrayList<>();
    for (Map.Entry<String, String> tu : CUSTOM_TUID_TO_TMX.entrySet()) {
      Map<String, String> segs = loadSegs(tu.getKey(), tu.getValue());
      assertNotNull(segs, tu.getValue() + " missing <tu> " + tu.getKey());
      for (Map.Entry<String, String> want : expected.entrySet()) {
        if (!want.getValue().equals(segs.get(want.getKey()))) {
          offenders.add(
              tu.getKey()
                  + " ["
                  + want.getKey()
                  + "]="
                  + segs.get(want.getKey()));
        }
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "Custom TUVs must use the adjective sense (customized), not the noun (custom/habit): "
            + offenders);
  }

  @Test
  public void customKindKey_polishUsesAdjectiveSense() throws Exception {
    List<String> offenders = new ArrayList<>();
    for (Map.Entry<String, String> tu : CUSTOM_TUID_TO_TMX.entrySet()) {
      Map<String, String> segs = loadSegs(tu.getKey(), tu.getValue());
      assertNotNull(segs, tu.getValue() + " missing <tu> " + tu.getKey());
      if (!EXPECTED_PL.equals(segs.get("pl"))) {
        offenders.add(tu.getKey() + " [pl]=" + segs.get("pl"));
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "Custom TUVs must use the adjective sense (customized Niestandardowy), not the noun (Zwyczaj): "
            + offenders);
  }

  @Test
  public void customKindKey_turkishUsesAdjectiveSense() throws Exception {
    List<String> offenders = new ArrayList<>();
    for (Map.Entry<String, String> tu : CUSTOM_TUID_TO_TMX.entrySet()) {
      Map<String, String> segs = loadSegs(tu.getKey(), tu.getValue());
      assertNotNull(segs, tu.getValue() + " missing <tu> " + tu.getKey());
      if (!EXPECTED_TR.equals(segs.get("tr"))) {
        offenders.add(tu.getKey() + " [tr]=" + segs.get("tr"));
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "Custom TUVs must use the adjective sense (custom/special Özel), not the noun (Gelenek): "
            + offenders);
  }

  @Test
  public void customKindKey_norwegianSwedishUseAdjectiveSense() throws Exception {
    Map<String, String> expected = Map.of("nb", EXPECTED_NB, "sv", EXPECTED_SV);
    List<String> offenders = new ArrayList<>();
    for (Map.Entry<String, String> tu : CUSTOM_TUID_TO_TMX.entrySet()) {
      Map<String, String> segs = loadSegs(tu.getKey(), tu.getValue());
      assertNotNull(segs, tu.getValue() + " missing <tu> " + tu.getKey());
      for (Map.Entry<String, String> want : expected.entrySet()) {
        if (!want.getValue().equals(segs.get(want.getKey()))) {
          offenders.add(
              tu.getKey()
                  + " ["
                  + want.getKey()
                  + "]="
                  + segs.get(want.getKey()));
        }
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "Custom TUVs must use the adjective sense (customized), not the noun (Skikk/Beställnings): "
            + offenders);
  }

  /**
   * Guards issue #7116 for this key: the shared {@code es + "Custom"} cache entry must agree
   * with the shipped {@code es} segment. Missing keys are fine (uncached); disagreements
   * fail.
   */
  @Test
  public void customKindCache_agreesWithShippedTmx() throws Exception {
    Map<String, String> segs = loadSegs(TUID);
    assertNotNull(segs, "DeveloperUi.tmx missing <tu> " + TUID);
    Path cacheFile = resolveTranslateCache();
    assertNotNull(cacheFile, "i18n_translate.json not found");
    Map<String, String> cached = parseFlatJsonStringMap(cacheFile);

    List<String> offenders = new ArrayList<>();
    Map<String, String> expected =
        Map.ofEntries(
            Map.entry("es", EXPECTED_ES),
            Map.entry("fr", EXPECTED_FR),
            Map.entry("de", EXPECTED_DE),
            Map.entry("it", EXPECTED_IT),
            Map.entry("hi", EXPECTED_HI),
            Map.entry("zh-cn", EXPECTED_ZH_CN),
            Map.entry("zh-tw", EXPECTED_ZH_TW),
            Map.entry("ru", EXPECTED_RU),
            Map.entry("uk", EXPECTED_UK),
            Map.entry("pl", EXPECTED_PL),
            Map.entry("tr", EXPECTED_TR),
            Map.entry("nb", EXPECTED_NB),
            Map.entry("sv", EXPECTED_SV));
    for (Map.Entry<String, String> want : expected.entrySet()) {
      String seg = segs.get(want.getKey());
      if (seg != null) {
        String cachedValue = cached.get(translateCacheKey(SOURCE, want.getKey()));
        if (cachedValue != null && !cachedValue.equals(seg)) {
          offenders.add(TUID + " [" + want.getKey() + "] cache=" + cachedValue + " tmx=" + seg);
        }
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "i18n_translate.json cache disagrees with shipped DeveloperUi.tmx (#7230/#7241/#7252/#7284/#7285/#7286/#7287/#7288): "
            + offenders);
  }

  private static Map<String, String> loadSegs(String tuid) throws Exception {
    return loadSegs(tuid, "DeveloperUi.tmx");
  }

  private static Map<String, String> loadSegs(String tuid, String tmxFile) throws Exception {
    Path tmx = resolveTmx(tmxFile);
    assertNotNull(tmx, tmxFile + " not found");
    Document doc = parse(tmx);
    NodeList tus = doc.getElementsByTagName("tu");
    for (int i = 0; i < tus.getLength(); i++) {
      Element tu = (Element) tus.item(i);
      if (!tuid.equals(tu.getAttribute("tuid"))) {
        continue;
      }
      Map<String, String> segs = new LinkedHashMap<>();
      NodeList tuvs = tu.getElementsByTagName("tuv");
      for (int j = 0; j < tuvs.getLength(); j++) {
        Element tuv = (Element) tuvs.item(j);
        String lang = normalizeLang(readXmlLang(tuv));
        if (lang.isBlank()) {
          continue;
        }
        NodeList segNodes = tuv.getElementsByTagName("seg");
        if (segNodes.getLength() > 0) {
          segs.putIfAbsent(lang, segNodes.item(0).getTextContent());
        }
      }
      return segs;
    }
    return null;
  }

  /** Mirror of {@code i18n_translate.cache_key}: {@code sha256(target + "\0" + text)}. */
  private static String translateCacheKey(String text, String target) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(target.getBytes(StandardCharsets.UTF_8));
    digest.update((byte) 0);
    digest.update(text.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest.digest());
  }

  /**
   * Resolve {@code scripts/cache/i18n_translate.json} portably: prefer the module-relative path
   * (Maven surefire runs with the module dir as CWD), then fall back to repo-root-relative.
   */
  private static Path resolveTranslateCache() {
    Path[] candidates =
        new Path[] {
          Paths.get("scripts", "cache", "i18n_translate.json"),
          Paths.get(
              "modules", "perc-i18n", "scripts", "cache", "i18n_translate.json"),
        };
    for (Path c : candidates) {
      Path abs = c.toAbsolutePath().normalize();
      if (Files.isRegularFile(abs)) {
        return abs;
      }
    }
    return null;
  }

  /** Parse a flat JSON {@code {"key": "value", ...}} object of string keys/values. */
  private static Map<String, String> parseFlatJsonStringMap(Path json) throws Exception {
    String raw = Files.readString(json, StandardCharsets.UTF_8).trim();
    Map<String, String> map = new LinkedHashMap<>();
    int[] pos = {0};
    skipWs(raw, pos);
    expect(raw, pos, '{');
    skipWs(raw, pos);
    if (peek(raw, pos) == '}') {
      return map;
    }
    while (true) {
      skipWs(raw, pos);
      String key = readJsonString(raw, pos);
      skipWs(raw, pos);
      expect(raw, pos, ':');
      skipWs(raw, pos);
      String value = readJsonString(raw, pos);
      map.put(key, value);
      skipWs(raw, pos);
      char c = peek(raw, pos);
      if (c == ',') {
        pos[0]++;
        continue;
      }
      if (c == '}') {
        break;
      }
      throw new IllegalArgumentException("Unexpected char '" + c + "' in cache JSON");
    }
    return map;
  }

  private static void skipWs(String raw, int[] pos) {
    while (pos[0] < raw.length()
        && (raw.charAt(pos[0]) == ' '
            || raw.charAt(pos[0]) == '\n'
            || raw.charAt(pos[0]) == '\r'
            || raw.charAt(pos[0]) == '\t')) {
      pos[0]++;
    }
  }

  private static char peek(String raw, int[] pos) {
    if (pos[0] >= raw.length()) {
      throw new IllegalArgumentException("Truncated cache JSON");
    }
    return raw.charAt(pos[0]);
  }

  private static void expect(String raw, int[] pos, char want) {
    if (peek(raw, pos) != want) {
      throw new IllegalArgumentException("Expected '" + want + "' in cache JSON");
    }
    pos[0]++;
  }

  private static String readJsonString(String raw, int[] pos) {
    expect(raw, pos, '"');
    StringBuilder sb = new StringBuilder();
    while (true) {
      char c = peek(raw, pos);
      if (c == '"') {
        pos[0]++;
        return sb.toString();
      }
      if (c != '\\') {
        sb.append(c);
        pos[0]++;
        continue;
      }
      pos[0]++;
      char e = peek(raw, pos);
      pos[0]++;
      switch (e) {
        case '"':
          sb.append('"');
          break;
        case '\\':
          sb.append('\\');
          break;
        case '/':
          sb.append('/');
          break;
        case 'b':
          sb.append('\b');
          break;
        case 'f':
          sb.append('\f');
          break;
        case 'n':
          sb.append('\n');
          break;
        case 'r':
          sb.append('\r');
          break;
        case 't':
          sb.append('\t');
          break;
        case 'u':
          if (pos[0] + 4 > raw.length()) {
            throw new IllegalArgumentException("Truncated unicode escape in cache JSON");
          }
          sb.append((char) Integer.parseInt(raw.substring(pos[0], pos[0] + 4), 16));
          pos[0] += 4;
          break;
        default:
          throw new IllegalArgumentException("Bad escape '\\" + e + "' in cache JSON"); // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #7230, same as #7138/#7116) — backslash is a JSON-escape in the test helper message, not an OS path; real paths use Paths.get joins
      }
    }
  }

  /**
   * Resolve a canonical {@code i18n/*.tmx} file portably: prefer the test classpath resource
   * (Maven surefire), then fall back to common source-tree locations relative to the process
   * working directory.
   */
  private static Path resolveTmx(String tmxFile) throws URISyntaxException {
    URL url = PSDeveloperCustomKindKeyTmxTest.class.getResource("/i18n/" + tmxFile);
    if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
      Path p = Paths.get(url.toURI());
      if (Files.isRegularFile(p)) {
        return p;
      }
    }
    Path[] candidates =
        new Path[] {
          Paths.get("src", "main", "resources", "i18n", tmxFile),
          Paths.get("modules", "perc-i18n", "src", "main", "resources", "i18n", tmxFile),
        };
    for (Path c : candidates) {
      Path abs = c.toAbsolutePath().normalize();
      if (Files.isRegularFile(abs)) {
        return abs;
      }
    }
    return null;
  }

  private static Document parse(Path tmx) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    try (var reader = Files.newBufferedReader(tmx)) {
      return factory.newDocumentBuilder().parse(new InputSource(reader));
    }
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
    return raw.trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
  }
}
