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
package com.percussion.services.virtualsite;

import com.percussion.services.sitemgr.IPSSite;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fetches or clones a Git remote into a contained NIO work directory before Virtual Site discover.
 *
 * <p>Uses {@link ProcessBuilder} with an argument list (never a shell). Fail-closed on unsafe URLs,
 * {@code ..} paths, and option-injection. Never logs credentials — remote URLs are redacted.
 */
public class PSGitRemoteCheckout {

  private static final Logger log = LogManager.getLogger(PSGitRemoteCheckout.class);

  static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);

  private static final int MAX_URL_LENGTH = 2048;
  private static final int MAX_OUTPUT_CHARS = 4000;
  private static final Pattern SAFE_BRANCH =
      Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/-]{0,199}$");
  private static final Pattern SCP_LIKE =
      Pattern.compile("^[A-Za-z0-9._-]+@[A-Za-z0-9.-]+:[A-Za-z0-9._/+-]+(?:\\.git)?$");
  private static final Pattern SAFE_HOST =
      Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?$");
  private static final Pattern UNSAFE_URL_CHARS = Pattern.compile("[\\s;|&$`<>\"'\\\\]");

  /** Runs {@code git} with a portable argument list. */
  @FunctionalInterface
  public interface ProcessRunner {
    /**
     * Execute {@code command} with optional working directory.
     *
     * @param workingDirectory may be null (inherit)
     * @param command argument list; first element is the executable
     * @param output combined stdout/stderr (already redacted by the caller after return)
     * @return process exit code
     * @throws IOException when the process cannot be started
     * @throws InterruptedException when interrupted while waiting
     */
    int run(Path workingDirectory, List<String> command, StringBuilder output)
        throws IOException, InterruptedException;
  }

  private final ProcessRunner runner;
  private final Duration timeout;

  public PSGitRemoteCheckout() {
    this(null, DEFAULT_TIMEOUT);
  }

  /**
   * @param runner process runner; null uses the default {@link ProcessBuilder} implementation
   * @param timeout wait limit per git invocation
   */
  public PSGitRemoteCheckout(ProcessRunner runner, Duration timeout) {
    this.timeout = timeout != null && !timeout.isNegative() && !timeout.isZero() ? timeout : DEFAULT_TIMEOUT;
    this.runner = runner != null ? runner : this::runGitProcess;
  }

  /**
   * Clone or fetch the site remote into {@code workBase}/{siteKey} and return the discover root.
   *
   * @param site virtual site with a remote
   * @param workBase contained parent directory (created if missing)
   * @return discover root (checkout or relative sub-path)
   * @throws VirtualSiteException validation or git failure
   * @throws IOException filesystem failure
   */
  public Path ensureCurrent(IPSSite site, Path workBase)
      throws VirtualSiteException, IOException {
    Objects.requireNonNull(site, "site");
    String url =
        PSVirtualSiteHelper.remoteUrl(site)
            .orElseThrow(
                () ->
                    new VirtualSiteException(
                        PSVirtualSiteHelper.PROP_REMOTE_URL + " is required for Git remote checkout."));
    return ensureCurrent(
        url,
        PSVirtualSiteHelper.branch(site),
        PSVirtualSiteHelper.siteKey(site),
        workBase,
        site);
  }

  /**
   * Clone or fetch {@code remoteUrl} at {@code branch} into a contained work directory.
   *
   * @param remoteUrl validated Git URL
   * @param branch validated branch (blank ⇒ {@link PSVirtualSiteHelper#DEFAULT_BRANCH})
   * @param siteKey used as the work-directory name
   * @param workBase parent directory
   * @param site optional site for relative {@code virtual.rootPath}; may be null
   * @return discover root
   */
  public Path ensureCurrent(
      String remoteUrl, String branch, String siteKey, Path workBase, IPSSite site)
      throws VirtualSiteException, IOException {
    String safeUrl = requireSafeRemoteUrl(remoteUrl);
    String safeBranch = requireSafeBranch(StringUtils.isBlank(branch) ? PSVirtualSiteHelper.DEFAULT_BRANCH : branch);
    Path safeBase = requireSafeWorkBase(workBase);
    String segment = safeWorkSegment(siteKey);
    Path workDir = safeBase.resolve(segment).normalize();
    if (!workDir.startsWith(safeBase) || !PSVirtualSiteHelper.isSafeRootPath(workDir)) {
      throw new VirtualSiteException("Git checkout work directory is not contained under the work base.");
    }
    Files.createDirectories(safeBase);
    if (Files.exists(workDir) && !Files.isDirectory(workDir)) {
      throw new VirtualSiteException("Git checkout path exists and is not a directory.");
    }
    Path gitDir = workDir.resolve(".git");
    if (Files.exists(gitDir)) {
      fetchExisting(workDir, safeUrl, safeBranch);
    } else {
      cloneFresh(workDir, safeUrl, safeBranch);
    }
    return PSVirtualSiteHelper.resolveDiscoverRoot(site, workDir);
  }

  /**
   * Fail-closed Git remote validation. Never returns a URL that starts with {@code -} or contains
   * {@code ..} / shell metacharacters.
   *
   * @param raw operator-supplied URL
   * @return trimmed URL
   * @throws VirtualSiteException when the URL is unsafe or unsupported
   */
  public static String requireSafeRemoteUrl(String raw) throws VirtualSiteException {
    if (StringUtils.isBlank(raw)) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " must not be blank when set.");
    }
    String url = raw.trim();
    if (url.length() > MAX_URL_LENGTH) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " exceeds maximum length.");
    }
    if (url.startsWith("-")) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " must not start with '-'.");
    }
    if (url.indexOf('\0') >= 0 || UNSAFE_URL_CHARS.matcher(url).find()) {
      throw new VirtualSiteException(
          PSVirtualSiteHelper.PROP_REMOTE_URL
              + " contains unsafe characters (whitespace, quotes, or shell metacharacters).");
    }
    if (url.contains("..")) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " must not contain '..'.");
    }
    String lower = url.toLowerCase(Locale.ROOT);
    if (lower.startsWith("https://")) {
      return requireSafeHierarchicalUrl(url, "https");
    }
    if (lower.startsWith("ssh://")) {
      return requireSafeHierarchicalUrl(url, "ssh");
    }
    if (lower.startsWith("file:")) {
      return requireSafeFileUrl(url);
    }
    if (SCP_LIKE.matcher(url).matches()) {
      return url;
    }
    throw new VirtualSiteException(
        PSVirtualSiteHelper.PROP_REMOTE_URL
            + " must be https://, ssh://, file://, or git@host:path (http and other schemes are"
            + " rejected).");
  }

  /**
   * Fail-closed Git branch / ref name. Rejects option injection and {@code ..}.
   *
   * @param raw branch name
   * @return trimmed branch
   */
  public static String requireSafeBranch(String raw) throws VirtualSiteException {
    if (StringUtils.isBlank(raw)) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_BRANCH + " must not be blank when set.");
    }
    String branch = raw.trim();
    if (branch.startsWith("-") || branch.startsWith("/") || branch.endsWith(".lock")) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_BRANCH + " is not a safe Git ref name.");
    }
    if (branch.contains("..") || !SAFE_BRANCH.matcher(branch).matches()) {
      throw new VirtualSiteException(
          PSVirtualSiteHelper.PROP_BRANCH
              + " must be a simple ref (letters, digits, '.', '_', '/', '-') with no '..'.");
    }
    return branch;
  }

  /**
   * Redact userinfo / obvious secrets from a remote URL or process output for logs and exceptions.
   *
   * @param text may be null
   * @return redacted text, never null
   */
  public static String redact(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    String s = text.replaceAll("://[^\\s/@]+@", "://***@");
    return s.replaceAll("(?i)(token|password|passwd|secret|authorization)[=:]\\S+", "$1=***");
  }

  static String safeWorkSegment(String siteKey) {
    String raw = StringUtils.isBlank(siteKey) ? "default" : siteKey.trim();
    StringBuilder b = new StringBuilder(Math.min(raw.length(), 80));
    for (int i = 0; i < raw.length() && b.length() < 80; i++) {
      char c = raw.charAt(i);
      if ((c >= 'A' && c <= 'Z')
          || (c >= 'a' && c <= 'z')
          || (c >= '0' && c <= '9')
          || c == '.'
          || c == '_'
          || c == '-') {
        b.append(c);
      } else {
        b.append('_');
      }
    }
    String segment = b.toString();
    if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
      return "default";
    }
    return segment;
  }

  private void cloneFresh(Path workDir, String safeUrl, String safeBranch)
      throws VirtualSiteException, IOException {
    if (Files.exists(workDir) && !isEmptyDirectory(workDir)) {
      throw new VirtualSiteException(
          "Git checkout directory exists and is not an empty Git work tree. Refusing to clone.");
    }
    Files.createDirectories(workDir.getParent());
    List<String> command = new ArrayList<>();
    command.add("git");
    command.add("clone");
    command.add("--depth");
    command.add("1");
    command.add("--branch");
    command.add(safeBranch);
    command.add("--single-branch");
    command.add("--");
    command.add(safeUrl);
    command.add(workDir.toString());
    runChecked(workDir.getParent(), command, "clone");
    if (!Files.isDirectory(workDir.resolve(".git")) && !Files.isRegularFile(workDir.resolve(".git"))) {
      throw new VirtualSiteException("Git clone did not produce a .git directory.");
    }
  }

  private void fetchExisting(Path workDir, String safeUrl, String safeBranch)
      throws VirtualSiteException, IOException {
    String origin = readOriginUrl(workDir);
    if (StringUtils.isNotBlank(origin) && !sameRemote(origin, safeUrl)) {
      throw new VirtualSiteException(
          "Existing checkout origin does not match the configured remote. Refusing to fetch.");
    }
    List<String> fetch = List.of("git", "fetch", "--depth", "1", "--", "origin", safeBranch);
    runChecked(workDir, fetch, "fetch");
    List<String> checkout = List.of("git", "checkout", "--force", "-B", safeBranch, "FETCH_HEAD");
    runChecked(workDir, checkout, "checkout");
  }

  private String readOriginUrl(Path workDir) throws VirtualSiteException {
    StringBuilder output = new StringBuilder();
    List<String> command = List.of("git", "remote", "get-url", "origin");
    try {
      int code = runner.run(workDir, command, output);
      if (code != 0) {
        return "";
      }
      return output.toString().trim();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new VirtualSiteException("Interrupted while reading Git origin URL.", e);
    } catch (IOException e) {
      throw new VirtualSiteException("Unable to read Git origin URL: " + e.getMessage(), e);
    }
  }

  private static boolean sameRemote(String existing, String configured) {
    String a = normalizeRemoteForCompare(existing);
    String b = normalizeRemoteForCompare(configured);
    return a.equalsIgnoreCase(b);
  }

  private static String normalizeRemoteForCompare(String url) {
    String s = redact(url).trim();
    if (s.endsWith(".git")) {
      s = s.substring(0, s.length() - 4);
    }
    if (s.endsWith("/")) {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }

  private void runChecked(Path cwd, List<String> command, String action)
      throws VirtualSiteException {
    StringBuilder output = new StringBuilder();
    int code;
    try {
      code = runner.run(cwd, command, output);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new VirtualSiteException("Interrupted during Git " + action + ".", e);
    } catch (IOException e) {
      throw new VirtualSiteException(
          "Unable to run git " + action + " (is git on PATH?): " + redact(e.getMessage()), e);
    }
    if (code != 0) {
      String snippet = redact(truncate(output.toString(), MAX_OUTPUT_CHARS));
      log.warn("Git {} failed with exit {} (credentials redacted).", action, code);
      throw new VirtualSiteException("Git " + action + " failed (exit " + code + "): " + snippet);
    }
    log.info("Git {} completed for Virtual Site checkout.", action);
  }

  private int runGitProcess(Path workingDirectory, List<String> command, StringBuilder output)
      throws IOException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder(command);
    if (workingDirectory != null) {
      builder.directory(workingDirectory.toFile());
    }
    builder.redirectErrorStream(true);
    Process process = builder.start();
    try (InputStream in = process.getInputStream()) {
      byte[] buf = in.readAllBytes();
      if (output != null) {
        output.append(new String(buf, StandardCharsets.UTF_8));
      }
    }
    boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    if (!finished) {
      process.destroyForcibly();
      throw new IOException("git timed out after " + timeout.toSeconds() + "s");
    }
    return process.exitValue();
  }

  private static String requireSafeHierarchicalUrl(String url, String scheme)
      throws VirtualSiteException {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " is not a valid URI.", e);
    }
    if (!scheme.equalsIgnoreCase(uri.getScheme())) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " scheme is not " + scheme + ".");
    }
    String host = uri.getHost();
    if (StringUtils.isBlank(host) || !SAFE_HOST.matcher(host).matches() || host.contains("..")) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " host is missing or unsafe.");
    }
    String path = uri.getPath();
    if (path != null && path.contains("..")) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " path must not contain '..'.");
    }
    return url;
  }

  private static String requireSafeFileUrl(String url) throws VirtualSiteException {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " is not a valid file URI.", e);
    }
    if (!"file".equalsIgnoreCase(uri.getScheme())) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " file URL must use the file scheme.");
    }
    String host = uri.getHost();
    if (StringUtils.isNotBlank(host) && !"localhost".equalsIgnoreCase(host)) {
      throw new VirtualSiteException(
          PSVirtualSiteHelper.PROP_REMOTE_URL + " file URL host must be empty or localhost.");
    }
    Path path;
    try {
      path = Path.of(uri).normalize();
    } catch (RuntimeException e) {
      throw new VirtualSiteException(PSVirtualSiteHelper.PROP_REMOTE_URL + " file URL is not a usable path.", e);
    }
    if (!PSVirtualSiteHelper.isSafeRootPath(path)) {
      throw new VirtualSiteException(
          PSVirtualSiteHelper.PROP_REMOTE_URL + " file URL path is unsafe after normalize.");
    }
    return url;
  }

  private static Path requireSafeWorkBase(Path workBase) throws VirtualSiteException {
    if (workBase == null) {
      throw new VirtualSiteException("Git checkout work base is required.");
    }
    Path normalized = workBase.normalize();
    if (!PSVirtualSiteHelper.isSafeRootPath(normalized)) {
      throw new VirtualSiteException("Git checkout work base is not a safe path.");
    }
    return normalized;
  }

  private static boolean isEmptyDirectory(Path dir) throws IOException {
    if (!Files.isDirectory(dir)) {
      return false;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      return !stream.iterator().hasNext();
    }
  }

  private static String truncate(String text, int max) {
    if (text == null) {
      return "";
    }
    if (text.length() <= max) {
      return text;
    }
    return text.substring(0, max) + "...";
  }
}
