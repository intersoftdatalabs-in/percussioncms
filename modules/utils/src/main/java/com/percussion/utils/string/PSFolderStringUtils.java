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
package com.percussion.utils.string;

import com.percussion.utils.jsr170.PSPath;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Methods to manipulate strings that express folder paths
 *
 * @author dougrand
 */
public class PSFolderStringUtils {
  /**
   * Translates an input folder list that uses '%' to indicate a wildcard and ';' to separate paths
   * to an array of compiled regex {@link Pattern}s.
   *
   * <p>Each path is split on the '%' wildcard, and each literal segment is wrapped via
   * {@link Pattern#quote(String)} so any regex meta-characters in user input (e.g. {@code \., (,
   * [)}, are treated as literals. The quoted segments are then re-joined with {@code ".*"} between
   * them so the '%' wildcard still functions as a multi-char wildcard.
   *
   * <p>This quoting strategy (introduced in T045 / PR #1295) prevents the unvalidated
   * concatenation of user-supplied folder paths into compiled regular expressions, addressing
   * the CodeQL {@code java/regex-injection} cluster (alerts #761, #765, #766, #1041, #1042,
   * #1043). The previous implementation hex-encoded all non-alphanumeric / whitespace / slash
   * characters; that approach was equivalent for matching but obscured the path semantics at
   * debug time.
   *
   * @param folderList the input folder string, may be <code>null</code> or empty
   * @return an array of patterns; empty for an empty input, never <code>null</code>
   */
  public static Pattern[] getFolderPatterns(String folderList) {
  public static Pattern[] getFolderPatterns(String folderList) {
    if (StringUtils.isBlank(folderList)) {
      return new Pattern[0];
    }
    String folderPaths[] = folderList.split(";");
    Pattern matchPatterns[] = new Pattern[folderPaths.length];

    int i = 0;

    for (String path : folderPaths) {
      // Split the path on '%' (documented wildcard) and \Q-quote each literal segment with
      // Pattern.quote so any regex meta-characters in user input are treated as literals.
      // Re-join the segments with ".*" between them so '%' still functions as a wildcard.
      StringBuilder matchpath = new StringBuilder(path.length() + 5);
      String[] segments = path.split("%", -1);
      for (int s = 0; s < segments.length; s++) {
        matchpath.append(Pattern.quote(segments[s]));
        if (s < segments.length - 1) {
          matchpath.append(".*");
        }
      }
      path = matchpath.toString();
      if (!path.endsWith(".*") && !path.endsWith("/")) {
        path = path + "/";
      }
      matchPatterns[i++] = Pattern.compile(path);
    }
    return matchPatterns;
  }

  /**
   * Find the root portion of the passed path. That is defined as the longest substring that does
   * not involve a component that contains a '%'.
   *
   * @param path the path, never <code>null</code> or empty
   * @return the largest substring
   */
  public static String getFolderRootPathFromPattern(String path) {
    PSPath pspath = new PSPath(path);

    // Find first component with a wildcard
    int first = -1;
    for (int i = 0; i < pspath.getCount(); i++) {
      String component = pspath.getName(i);
      if (component.contains("%")) {
        first = i;
        break;
      }
    }

    if (first < 0) {
      return path;
    } else {
      StringBuilder b = new StringBuilder();

      for (int i = 0; i < first; i++) {
        b.append('/');
        b.append(pspath.getName(i));
      }

      if (b.length() == 0) {
        b.append('/');
      }

      return b.toString();
    }
  }

  /**
   * Does one of the passed paths match one of the patterns?
   *
   * @param paths zero or more paths to match, assumed not <code>null</code>
   * @param matchPatterns one or more patterns to match, assumed not <code>null</code>
   * @return <code>true</code> if a path is found that matches a pattern
   */
  public static boolean oneMatched(String[] paths, Pattern[] matchPatterns) {
    for (String path : paths) {
      // Make path end in a slash
      if (!path.endsWith("/")) path += "/";
      for (Pattern pattern : matchPatterns) {
        if (pattern.matcher(path).matches()) return true;
      }
    }
    return false;
  }
}
