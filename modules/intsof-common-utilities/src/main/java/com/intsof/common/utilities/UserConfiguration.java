/*
 * Copyright 2026 Intersoft Data Labs (https://intsof.com)
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intsof.common.utilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-user configuration root under {@code ${user.home}/.intsof}.
 *
 * <p>Applications register a subfolder via {@link #createApplication(String)} and then manage
 * config files through {@link AppConfigurationFolder}. The root is always owned by the account that
 * is executing the code. Paths use {@link java.nio.file} APIs for Windows, Linux, and macOS
 * portability.
 *
 * <p>This type is product-agnostic: it does not encode CMS/DTS or other product schemas.
 */
public final class UserConfiguration {

  /** Directory name for the per-user Intersoft config root. */
  public static final String ROOT_DIR_NAME = ".intsof";

  private final Path rootPath;

  private UserConfiguration(Path rootPath) {
    this.rootPath = rootPath;
  }

  /**
   * Open the default configuration root under {@code System.getProperty("user.home")/.intsof},
   * creating the directory if it does not exist.
   *
   * @return configuration handle
   * @throws IOException if the root cannot be created
   * @throws IllegalStateException if {@code user.home} is unset or blank
   */
  public static UserConfiguration openDefault() throws IOException {
    String home = System.getProperty("user.home");
    if (home == null || home.isBlank()) {
      throw new IllegalStateException("System property user.home is not set");
    }
    return open(Path.of(home));
  }

  /**
   * Open a configuration root under {@code userHome/.intsof}, creating the directory if needed.
   * Prefer this overload in unit tests with a temporary directory.
   *
   * @param userHome user home directory (not the {@code .intsof} folder itself)
   * @return configuration handle
   * @throws IOException if the root cannot be created
   * @throws NullPointerException if {@code userHome} is null
   */
  public static UserConfiguration open(Path userHome) throws IOException {
    Objects.requireNonNull(userHome, "userHome");
    Path root = userHome.toAbsolutePath().normalize().resolve(ROOT_DIR_NAME);
    Files.createDirectories(root);
    return new UserConfiguration(root);
  }

  /**
   * Absolute normalized path to the {@code .intsof} root directory.
   *
   * @return root path
   */
  public Path getRootPath() {
    return rootPath;
  }

  /**
   * Register an application configuration folder under this root, creating it if it does not exist.
   * Idempotent: a second call with the same name returns a handle to the existing folder.
   *
   * @param applicationName single path segment (e.g. {@code "percussion"}, {@code "my-app"})
   * @return application folder handle
   * @throws IOException if the folder cannot be created
   * @throws IllegalArgumentException if the name is invalid
   */
  public AppConfigurationFolder createApplication(String applicationName) throws IOException {
    String name = ConfigNames.requireValidSegment(applicationName, "application name");
    Path appPath = PathsUnder.resolveUnder(rootPath, name);
    Files.createDirectories(appPath);
    return new AppConfigurationFolder(name, appPath);
  }

  /**
   * Open an existing application folder without creating it.
   *
   * @param applicationName single path segment
   * @return empty if the folder does not exist
   * @throws IllegalArgumentException if the name is invalid
   */
  public Optional<AppConfigurationFolder> findApplication(String applicationName) {
    String name = ConfigNames.requireValidSegment(applicationName, "application name");
    Path appPath = PathsUnder.resolveUnder(rootPath, name);
    if (!Files.isDirectory(appPath)) {
      return Optional.empty();
    }
    return Optional.of(new AppConfigurationFolder(name, appPath));
  }

  /**
   * Whether an application configuration folder already exists.
   *
   * @param applicationName single path segment
   * @return true when the directory exists
   * @throws IllegalArgumentException if the name is invalid
   */
  public boolean applicationExists(String applicationName) {
    return findApplication(applicationName).isPresent();
  }
}
