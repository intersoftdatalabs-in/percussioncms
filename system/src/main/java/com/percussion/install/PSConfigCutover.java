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
package com.percussion.install;

import com.percussion.utils.jdbc.PSJdbcUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Multi-file configuration cutover with rollback for CMS Derby → H2 migration (#548 T060, FR-013,
 * QC-009).
 *
 * <p>Writes {@code rxrepository.properties} and Jetty {@code perc-ds.properties} atomically after
 * backing up previous files. On failure, restores from the cutover backup set.
 */
public final class PSConfigCutover {

  private static final Logger LOG = Logger.getLogger(PSConfigCutover.class.getName());

  /** Relative path to CMS repository properties. */
  public static final String RXREPOSITORY_RELATIVE =
      Path.of("rxconfig", "Installer", "rxrepository.properties").toString();

  /** Primary Jetty datasource properties path. */
  public static final String PERC_DS_RELATIVE =
      Path.of("jetty", "base", "etc", "perc-ds.properties").toString();

  /** Alternate Jetty module path sometimes present on installs. */
  public static final String PERC_DS_MODULE_RELATIVE =
      Path.of("jetty", "base", "modules", "perc-ds", "etc", "perc-ds.properties").toString();

  private PSConfigCutover() {}

  /**
   * Cutover result.
   *
   * @param filesWritten absolute paths updated
   * @param backupDir directory holding pre-cutover copies
   */
  public record Result(List<Path> filesWritten, Path backupDir) {}

  /**
   * Apply H2 repository properties and align perc-ds when present.
   *
   * @param installRoot CMS install root
   * @param h2RepositoryProperties complete new rxrepository content
   * @return cutover result
   * @throws IOException on I/O failure after attempted rollback
   */
  public static Result cutoverToH2(Path installRoot, Properties h2RepositoryProperties)
      throws IOException {
    Objects.requireNonNull(installRoot, "installRoot");
    Objects.requireNonNull(h2RepositoryProperties, "h2RepositoryProperties");

    Path backupDir =
        installRoot
            .resolve("PreInstall")
            .resolve("cutover-backup")
            .resolve(Long.toString(System.currentTimeMillis()));
    Files.createDirectories(backupDir);

    Map<Path, Path> backups = new LinkedHashMap<>();
    List<Path> written = new ArrayList<>();

    try {
      Path rxPath = installRoot.resolve(RXREPOSITORY_RELATIVE);
      backupIfExists(rxPath, backupDir, backups);
      writePropertiesAtomic(rxPath, h2RepositoryProperties);
      written.add(rxPath);

      String driverName =
          h2RepositoryProperties.getProperty(
              PSRepositoryConnectionHelper.KEY_DB_DRIVER_NAME, PSJdbcUtils.H2_DRIVER);
      String driverClass =
          h2RepositoryProperties.getProperty(
              PSRepositoryConnectionHelper.KEY_DB_DRIVER_CLASS, PSJdbcUtils.H2_DRIVER_CLASS);
      String server =
          h2RepositoryProperties.getProperty(PSRepositoryConnectionHelper.KEY_DB_SERVER, "");
      String uid = h2RepositoryProperties.getProperty(PSRepositoryConnectionHelper.KEY_UID, "sa");
      String pwd = h2RepositoryProperties.getProperty(PSRepositoryConnectionHelper.KEY_PWD, "");

      for (String rel : List.of(PERC_DS_RELATIVE, PERC_DS_MODULE_RELATIVE)) {
        Path percDs = installRoot.resolve(rel);
        if (!Files.isRegularFile(percDs)) {
          continue;
        }
        backupIfExists(percDs, backupDir, backups);
        Properties perc = loadProperties(percDs);
        perc.setProperty("perc.ds.1.driver.name", driverName);
        perc.setProperty("perc.ds.1.driver.class", driverClass);
        perc.setProperty("perc.ds.1.server", server);
        perc.setProperty("perc.ds.1.uid", uid);
        perc.setProperty("perc.ds.1.pwd", pwd);
        perc.setProperty("perc.ds.1.connectiontest", "SELECT 1");
        writePropertiesAtomic(percDs, perc);
        written.add(percDs);
      }

      LOG.info(() -> "Cutover wrote " + written.size() + " file(s); backup=" + backupDir);
      return new Result(List.copyOf(written), backupDir);
    } catch (IOException e) {
      rollback(backups);
      throw new IOException(
          "Cutover failed; restored previous configs from " + backupDir + ": " + e.getMessage(), e);
    }
  }

  /**
   * Restore files from a previous cutover backup map.
   *
   * @param backups live path → backup path
   */
  public static void rollback(Map<Path, Path> backups) {
    if (backups == null) {
      return;
    }
    for (Map.Entry<Path, Path> e : backups.entrySet()) {
      try {
        if (Files.isRegularFile(e.getValue())) {
          Files.createDirectories(e.getKey().getParent());
          Files.copy(e.getValue(), e.getKey(), StandardCopyOption.REPLACE_EXISTING);
        }
      } catch (IOException ex) {
        LOG.severe("Failed to restore " + e.getKey() + " from " + e.getValue() + ": " + ex);
      }
    }
  }

  /**
   * Restore all files under a cutover backup directory into install root (by matching relative
   * names under the backup tree).
   *
   * @param installRoot install root
   * @param backupDir backup directory created by {@link #cutoverToH2}
   */
  public static void rollbackFromBackupDir(Path installRoot, Path backupDir) throws IOException {
    Objects.requireNonNull(installRoot, "installRoot");
    Objects.requireNonNull(backupDir, "backupDir");
    if (!Files.isDirectory(backupDir)) {
      return;
    }
    try (var walk = Files.walk(backupDir)) {
      walk.filter(Files::isRegularFile)
          .forEach(
              backupFile -> {
                try {
                  Path rel = backupDir.relativize(backupFile);
                  Path live = installRoot.resolve(rel.toString());
                  Files.createDirectories(live.getParent());
                  Files.copy(backupFile, live, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                  throw new RuntimeException(ex);
                }
              });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException ioe) {
        throw ioe;
      }
      throw e;
    }
  }

  private static void backupIfExists(Path live, Path backupDir, Map<Path, Path> backups)
      throws IOException {
    if (!Files.isRegularFile(live)) {
      return;
    }
    // Short unique name: original filename + collision-resistant path digest
    String name = live.getFileName() != null ? live.getFileName().toString() : "config.properties";
    String abs = live.toAbsolutePath().normalize().toString();
    String hash = shortPathDigest(abs);
    String safe = name + "." + hash + ".bak";
    if (safe.length() > 200) {
      safe = "cutover." + hash + ".bak";
    }
    Path dest = backupDir.resolve(safe);
    Files.copy(live, dest, StandardCopyOption.REPLACE_EXISTING);
    backups.put(live, dest);
  }

  /**
   * Collision-resistant short digest of an absolute path for backup basenames (not {@link
   * String#hashCode()}).
   */
  static String shortPathDigest(String absolutePath) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] dig = md.digest(absolutePath.getBytes(StandardCharsets.UTF_8));
      // 16 hex chars = 64 bits; ample for config-path uniqueness in one install tree
      return HexFormat.of().formatHex(dig, 0, 8);
    } catch (NoSuchAlgorithmException e) {
      // Unreachable on a standard JRE; fall back to hex of UTF-8 bytes length+prefix
      return Integer.toHexString(absolutePath.length())
          + Integer.toHexString(absolutePath.hashCode()).replace('-', 'n');
    }
  }

  private static Properties loadProperties(Path path) throws IOException {
    Properties p = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      p.load(in);
    }
    return p;
  }

  private static void writePropertiesAtomic(Path path, Properties props) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
    try (OutputStream out = Files.newOutputStream(tmp)) {
      props.store(out, "Percussion CMS repository config — H2 cutover (#548); do not log PWD");
    }
    try {
      Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
