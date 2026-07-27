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
package com.percussion.designmanagement.service.impl;

import static org.springframework.util.StringUtils.trimLeadingCharacter;
import static org.springframework.util.StringUtils.trimTrailingCharacter;

import com.percussion.designmanagement.service.IPSFileSystemService;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.security.io.PSPathInjectionGuard;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * An {@link IPSFileSystemService} implementation to handle file system operations, specially under
 * {rxDir}/web_resources (however this is configurable), in a way similar as the Finder works to
 * create pages and folders, i.e. new folders are created with the "New Folder" prefix (like the
 * Finder under "Sites").
 *
 * <p>It supports a list of includes, so only those file names under the root folder are returned.
 *
 * @author miltonpividori
 * @see IPSFileSystemService
 */
public class PSFileSystemService implements IPSFileSystemService {

  /** Prefix for new folders names. */
  public static final String NEW_FOLDER_NAME_PREFIX = "New-Folder";

  /** Max length allowed for folder names. */
  public static final Integer FOLDER_NAME_MAX_LENGTH = 255;

  /** The maximum allowed size for a file. It is configurable via spring. */
  public Float maxFileSize;

  /** A list of reserved file names. These ones cannot be used as a folder or file name. */
  private static final List<String> RESERVED_FILENAMES =
      Arrays.asList(
          new String[] {
            ".", "..", "CON", "PRN", "AUX", "CLOCK$", "NUL", "COM0", "COM1", "COM2", "COM3", "COM4",
            "COM5", "COM6", "COM7", "COM8", "COM9", "LPT0", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5",
            "LPT6", "LPT7", "LPT8", "LPT9"
          });

  /** A list of characters that can't be used in the file or folder name */
  private static final List<Character> INVALID_CHARS =
      Arrays.asList(new Character[] {'/', '\\', ':', '*', '?', '"', '<', '>', '|'});

  /** The root file system path. */
  private String rootFolderPath;

  /** The root folder File object. It's cached in this field. */
  private File rootDirectory;

  /**
   * A list of includes file names. This is only applied to the root directory, not sub directories.
   * Only these ones are visible when getting the children of the root directory.
   */
  protected List<String> includes = new ArrayList<>();

  public PSFileSystemService(String rootFolderPath) {
    this.rootFolderPath = rootFolderPath;
  }

  public String getRootFolderPath() {
    return rootFolderPath;
  }

  public void setRootFolderPath(String rootFolderPath) {
    this.rootFolderPath = rootFolderPath;
    this.rootDirectory = null;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  public Float getMaxFileSize() {
    return maxFileSize;
  }

  public void setMaxFileSize(Float maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  /**
   * Gets the root directory of this {@link IPSPathService} implementation. It caches the File
   * object returned.
   *
   * @return A {@link File} object representing the root directory of this {@link IPSPathService}
   *     implementation.
   */
  private File getRootDirectory() {
    if (rootDirectory == null) {
      rootDirectory = new File(rootFolderPath);
    }

    return rootDirectory;
  }

  /**
   * Validates a user-supplied path against the CWE-22 path-traversal defense. The defense has two
   * parts:
   *
   * <ol>
   *   <li>Every path segment (delimited by {@code /} or {@code \}) is checked against the
   *       traversal-marker contract: segments equal to {@code .} or {@code ..} are rejected. This
   *       closes the leaf-only bypass flagged by the CRITICAL review thread on PR #1210: a payload
   *       like {@code themes/../../etc/passwd} yields a leaf of {@code passwd} that passes a
   *       name-only check, yet the intermediate {@code ..} segments resolve the file outside the
   *       intended root. Checking every segment rejects this payload before the
   *       canonical-containment step even runs.
   *   <li>The resolved canonical path is verified to be contained within the server-controlled
   *       {@link #getRootDirectory() rootDirectory} (the configured web_resources root).
   *       Containment is checked against the trusted root, NOT against an input-derived parent: the
   *       latter would be a tautology that still permits traversal (a payload whose {@code ..}
   *       segments live in the parent portion canonicalizes outside the parent and the check would
   *       pass against the traversed parent). The trusted-root check closes this bypass as well.
   * </ol>
   *
   * This method is the single sanitizer boundary for the file-system service CWE-22 defense (per
   * T043d / PR #1210). It is called from every public entry point that accepts a user-supplied path
   * ({@link #getChildren}, {@link #getFile}, {@link #addFolder}, {@link #renameFolder}, {@link
   * #deleteFolder}, {@link #deleteFile}, {@link #validateFileUpload}); the downstream {@code new
   * File(root, path)} constructions in those entry points are protected by this call.
   *
   * @param path a user-supplied path (relative or absolute), never {@code null}
   * @throws IllegalArgumentException if any segment is {@code .} or {@code ..}, if the path
   *     contains a NUL byte, or if the resolved canonical path escapes the configured root
   *     directory
   */
  private void validatePath(String path) {
    if (path == null) {
      throw new IllegalArgumentException("path must not be null");
    }
    if (path.indexOf('\0') >= 0) {
      throw new IllegalArgumentException("path must not contain a NUL byte");
    }
    String[] segments = path.split("[/\\\\]");
    for (String segment : segments) {
      if ("..".equals(segment) || ".".equals(segment)) {
        throw new IllegalArgumentException(
            "path segment '"
                + segment
                + "' is not allowed in path '"
                + path
                + "' (path-traversal attempt blocked)");
      }
    }
    // codeql[java/path-injection] reason: path is a user-supplied string.
    // This method is the single sanitizer boundary for the file-system
    // service CWE-22 defense (T043d / PR #1210). The segment-marker
    // check above rejects every `.`/`..` segment in the input, and
    // the canonical-path containment check below verifies the resolved
    // path is contained within the server-controlled rootDirectory.
    // Containment is checked against the trusted root, not against an
    // input-derived parent, so traversal payloads like
    // "themes/../../etc/passwd" (which canonicalize outside the root)
    // are rejected. Absolute inputs are resolved as-is (not re-rooted
    // under rootDirectory): on Unix, `new File(root, "/etc/passwd")`
    // re-parents to root+"/etc/passwd", which would hide absolute-path
    // escapes; matching PSPathInjectionGuard.requireUnderBase.
    // The downstream new File(root, path) constructions in the public
    // entry points are all preceded by a validatePath call and inherit
    // this sanitizer.
    File candidate = new File(path);
    File resolved = candidate.isAbsolute() ? candidate : new File(getRootDirectory(), path);
    String canonical;
    String rootCanonical;
    try {
      canonical = resolved.getCanonicalPath();
      rootCanonical = getRootDirectory().getCanonicalPath();
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to resolve canonical path for input: " + path, e);
    }
    // Normalize separators so Windows prefix checks are reliable
    // (same approach as PSPathInjectionGuard.requireUnderBase).
    String canonicalNorm = canonical.replace('\\', '/');
    String rootNorm = rootCanonical.replace('\\', '/');
    String rootWithSep = rootNorm.endsWith("/") ? rootNorm : rootNorm + "/";
    if (!canonicalNorm.equals(rootNorm) && !canonicalNorm.startsWith(rootWithSep)) {
      throw new IllegalArgumentException(
          "Resolved path '"
              + canonical
              + "' is not under root '"
              + rootCanonical
              + "' (path-traversal attempt blocked)");
    }
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#getChildren(java.lang.String)
   */
  @Override
  public List<File> getChildren(String path) throws FileNotFoundException {
    validatePath(path);
    var root = getRootDirectory();
    // After validatePath, resolve under trusted root (CodeQL residual after barrier)
    String rel = path.startsWith("/") ? path.substring(1) : path;
    var pathFile = rel.isEmpty() ? root : PSPathInjectionGuard.requireUnderBase(root, rel);

    if (!pathFile.exists()) { // codeql[java/path-injection]
      throw new FileNotFoundException("The path doesn't exist: " + path);
    }

    var children = pathFile.listFiles(); // codeql[java/path-injection]
    if (children == null) {
      return List.of();
    }

    // Filter only for root path
    if (includes.isEmpty() || !StringUtils.equals(path, "/")) {
      return Arrays.asList(children);
    }

    var result = new ArrayList<File>();
    for (var child : children) {
      if (includes.contains(child.getName())) {
        result.add(child);
      }
    }
    return result;
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemManagerService#getFile(java.lang.String)
   */
  @Override
  public File getFile(String path) {
    Validate.notNull(path, "path must not be null");
    validatePath(path);
    String rel = path.startsWith("/") ? path.substring(1) : path;
    if (rel.isEmpty()) {
      return getRootDirectory();
    }
    return PSPathInjectionGuard.requireUnderBase(
        getRootDirectory(), rel); // codeql[java/path-injection]
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#addFolder(java.lang.String)
   */
  @Override
  public File addFolder(String newFolderPath) throws IOException {
    Validate.notNull(newFolderPath, "newFolderPath cannot be null");
    validatePath(newFolderPath);
    var folderPath = getFile(newFolderPath);

    if (folderPath.isFile()) {
      folderPath = folderPath.getParentFile();
    }

    var newName = getNewFolderName(folderPath.list());
    var newFolder = new File(folderPath.getAbsolutePath(), newName);

    Files.createDirectory(newFolder.toPath());
    var parent = folderPath.getParentFile();
    setParentFolderPermissionsToChild(parent, newFolder);

    return newFolder;
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#getNewFolderName(java.lang.String[])
   */
  @Override
  public String getNewFolderName(String[] filesAndFolders) {
    var regex = NEW_FOLDER_NAME_PREFIX + " [0-9]+";
    int numberOfMatches = -1;

    if (filesAndFolders != null) {
      for (var file : filesAndFolders) {
        if (NEW_FOLDER_NAME_PREFIX.equals(file) && numberOfMatches < 0) {
          numberOfMatches = 0;
        } else if (Pattern.matches(regex, file)) {
          var number = Integer.valueOf(file.substring(NEW_FOLDER_NAME_PREFIX.length() + 1));
          if (number > numberOfMatches) {
            numberOfMatches = number;
          }
        }
      }
    }
    return (numberOfMatches >= 0)
        ? NEW_FOLDER_NAME_PREFIX + " " + (numberOfMatches + 1)
        : NEW_FOLDER_NAME_PREFIX;
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#renameFolder(java.lang.String, java.lang.String)
   */
  @Override
  public File renameFolder(String oldFolderPath, String newFolderName)
      throws PSFolderOperationException {
    Validate.notNull(oldFolderPath, "oldFolderPath cannot be null");
    Validate.notNull(newFolderName, "newFolderName cannot be null");
    validatePath(oldFolderPath);

    var oldFolder = getFile(oldFolderPath);
    var parentFolder = oldFolder.getParentFile();

    // The new folder name is a single-segment value, not a multi-segment
    // path. The existing per-name checks below (containsInvalidChars,
    // isReservedFilename) already reject every traversal payload:
    //   - containsInvalidChars rejects every path separator and the
    //     reserved characters ('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    //     with the specific PSInvalidCharacterInFolderNameException.
    //   - isReservedFilename rejects `.` and `..` (they are members of
    //     RESERVED_FILENAMES) with the specific PSInvalidFolderNameException.
    // Calling requireSafeFileName(newFolderName) here would duplicate both
    // checks and downgrade the exception type to IllegalArgumentException,
    // losing the domain-specific signal callers depend on.
    if (containsInvalidChars(newFolderName)) {
      throw new PSInvalidCharacterInFolderNameException(getInvalidCharsAsString());
    }
    if (newFolderName.length() > FOLDER_NAME_MAX_LENGTH) {
      throw new PSFolderNameLengthLimitException();
    }
    if (isReservedFilename(newFolderName)) {
      throw new PSInvalidFolderNameException();
    }
    if (!foldernameAvailable(newFolderName, parentFolder.list())) {
      throw new PSExistingFolderException();
    }

    var newFolder =
        new File(parentFolder.getAbsolutePath(), newFolderName); // codeql[java/path-injection]
    oldFolder.renameTo(newFolder); // codeql[java/path-injection]

    return newFolder;
  }

  /**
   * Checks if the name contains an invalid character.
   *
   * @param name the name to check. Assumed not <code>null</code>
   * @return <code>true</code> if the name contains an invalid character. <code>false</code>
   *     otherwise.
   */
  @Override
  public boolean containsInvalidChars(String name) {
    return INVALID_CHARS.stream().anyMatch(invalidChar -> StringUtils.contains(name, invalidChar));
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#foldernameAvailable(java.lang.String, java.lang.String[])
   */
  @Override
  public boolean foldernameAvailable(String name, String[] files) {
    if (files != null) {
      return Arrays.stream(files).noneMatch(file -> file.equalsIgnoreCase(name));
    }
    return true;
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#deleteFolder(java.lang.String)
   */
  @Override
  public void deleteFolder(String folderPath) throws IOException {
    Validate.notNull(folderPath, "path cannot be null");
    validatePath(folderPath);
    var fileToDelete = getFile(folderPath);
    FileUtils.deleteDirectory(fileToDelete); // codeql[java/path-injection]
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * com.percussion.designmanagement.service.IPSFileSystemService#deleteFile
   * (java.lang.String)
   */
  @Override
  public void deleteFile(String filePath) throws PSFileOperationException {
    Validate.notNull(filePath, "path cannot be null");
    validatePath(filePath);
    var fileToDelete = getFile(filePath);
    if (fileToDelete.exists()) { // codeql[java/path-injection]
      try {
        Files.delete(fileToDelete.toPath()); // codeql[java/path-injection]
      } catch (IOException e) {
        throw new PSFileOperationException(
            "Could not delete the file '" + fileToDelete.getName() + "'. " + e.getMessage());
      }
    }
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#getNameFromFile(java.io.File)
   */
  @Override
  public String getNameFromFile(File file) {
    Validate.notNull(file, "file cannot be null");
    var name = file.getName();
    if (StringUtils.isBlank(name)) {
      name = file.getParentFile().getName();
    }
    return name;
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#getParentFolder(java.lang.String)
   */
  @Override
  public String getParentFolder(String path) {
    var parentFolder = "/";
    var paths = path.split("/");
    for (int i = 1; i < paths.length - 1; i++) {
      parentFolder += paths[i] + "/";
    }
    return parentFolder;
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#validateUploadFile(java.lang.String)
   */
  @Override
  public void validateFileUpload(String path) throws PSFileOperationException {
    validatePath(path);
    var file = getFile(path);
    var parentFolder = file.getParentFile();

    if (!isUnderThemes(path)) {
      throw new PSFileOperationException(
          "File operations are only allowed under the 'themes' folder.");
    }
    if (containsInvalidChars(file.getName())) {
      throw new PSInvalidCharacterInFileNameException(
          "File names cannot have the following characters: " + getInvalidCharsAsString());
    }
    var files = parentFolder.list(FileFilterUtils.fileFileFilter());
    if (!foldernameAvailable(file.getName(), files)) {
      throw new PSFileAlreadyExistsException(
          "A file with that name already exists in the selected folder, and will be overwritten.");
    }
    var directories = parentFolder.list(FileFilterUtils.directoryFileFilter());
    if (!foldernameAvailable(file.getName(), directories)) {
      throw new PSFileNameInUseByFolderException(
          "A folder with that name already exists in the selected location.");
    }
    if (isReservedFilename(file.getName())) {
      throw new PSReservedFileNameException(
          "Cannot create file '" + file.getName() + "' because that is a reserved file name.");
    }
  }

  /**
   * Builds a string containing the invalid characters separated with a blank
   * space.
   *
   * @return a String object, may be empty but never <code>null<code>
   */
  private String getInvalidCharsAsString() {
    var chars = new StringBuilder();
    for (var invalidChar : INVALID_CHARS) {
      chars.append(invalidChar).append(" ");
    }
    return chars.toString();
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#isReservedWord(java.lang.String)
   */
  @Override
  public boolean isReservedFilename(String name) {
    return RESERVED_FILENAMES.stream()
        .anyMatch(reservedWord -> reservedWord.equalsIgnoreCase(name));
  }

  /* (non-Javadoc)
   * @see com.percussion.designmanagement.service.IPSFileSystemService#fileUpload(java.lang.String, java.io.InputStream)
   */
  @Override
  public void fileUpload(String path, InputStream pageContent) throws PSFileOperationException {
    try (var in = new BufferedInputStream(pageContent)) {
      try {
        validateFileUpload(path);
      } catch (PSFileAlreadyExistsException fae) {
        // Already checked and confirmed by client, so can be ignored
      }
      var file = getFile(path);
      var parent = file.getParentFile();

      try (var out = new BufferedOutputStream(new FileOutputStream(file))) {
        var bytesCopied = IOUtils.copy(in, out);
        out.flush();

        if (fileSizeExceeded(bytesCopied)) {
          FileUtils.deleteQuietly(file);
          throw new PSFileSizeExceededException(
              "The maximum allowed size for a file is " + maxFileSize + " MB.");
        }
        setParentFolderPermissionsToChild(parent, file);
      }
    } catch (IOException e) {
      throw new PSFileOperationException("An error occurred when uploading the file.", e);
    }
  }

  private void setParentFolderPermissionsToChild(File parent, File child) throws IOException {
    var posixViewParent = Files.getFileAttributeView(parent.toPath(), PosixFileAttributeView.class);
    if (posixViewParent == null) {
      return;
    }
    var parentAttribs = posixViewParent.readAttributes();
    var group = parentAttribs.group();
    var owner = parentAttribs.owner();
    var permissions = parentAttribs.permissions();
    var posixViewFile = Files.getFileAttributeView(child.toPath(), PosixFileAttributeView.class);
    posixViewFile.setPermissions(permissions);
    posixViewFile.setGroup(group);
    posixViewFile.setOwner(owner);
  }

  private boolean fileSizeExceeded(int fileSize) {
    var maxSizeInBytes = Float.valueOf(maxFileSize * 1024).longValue() * 1024;
    return fileSize > maxSizeInBytes || fileSize < 0;
  }

  private boolean isUnderThemes(String path) {
    var auxPath = trimLeadingCharacter(trimTrailingCharacter(path, '/'), '/');
    var paths = auxPath.split("/");
    return (paths.length >= 2);
  }
}
