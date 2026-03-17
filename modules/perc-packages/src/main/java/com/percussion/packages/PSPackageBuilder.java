package com.percussion.packages;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Package builder that zips package directories directly without using Ant loops. */
public class PSPackageBuilder {

  public static void main(String[] args) throws IOException {
    if (args.length != 3) {
      System.err.println("Usage: PSPackageBuilder <sourceDir> <outputDir> <tempDir>");
      System.exit(1);
    }

    File sourceDir = new File(args[0]);
    File outputDir = new File(args[1]);
    File tempDir = new File(args[2]);

    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      System.err.println("Source directory does not exist: " + sourceDir);
      System.exit(1);
    }

    outputDir.mkdirs();
    tempDir.mkdirs();

    File[] packages = sourceDir.listFiles(File::isDirectory);
    if (packages == null) {
      return;
    }

    for (File pkg : packages) {
      String pkgName = pkg.getName();
      if ("Percussion".equals(pkgName) || "packageholder".equals(pkgName)) {
        continue; // exclude as per ant script
      }
      File outputFile = new File(outputDir, pkgName + ".ppkg");
      System.out.println("Building package: " + outputFile.getName());
      zipDirectory(pkg, outputFile);
    }
  }

  private static void zipDirectory(File dir, File zipFile) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      zipDirectoryRecursive(dir, dir, zos);
    }
  }

  private static void zipDirectoryRecursive(File rootDir, File currentDir, ZipOutputStream zos)
      throws IOException {
    File[] files = currentDir.listFiles();
    if (files == null) {
      return;
    }

    for (File file : files) {
      String relativePath = getRelativePath(rootDir, file);
      if (file.isDirectory()) {
        // Add directory entry
        if (!relativePath.isEmpty()) {
          zos.putNextEntry(new ZipEntry(relativePath + "/"));
          zos.closeEntry();
        }
        zipDirectoryRecursive(rootDir, file, zos);
      } else {
        // Add file entry
        zos.putNextEntry(new ZipEntry(relativePath));
        try (FileInputStream fis = new FileInputStream(file)) {
          byte[] buffer = new byte[1024];
          int len;
          while ((len = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
          }
        }
        zos.closeEntry();
      }
    }
  }

  private static String getRelativePath(File rootDir, File file) {
    String rootPath = rootDir.getAbsolutePath();
    String filePath = file.getAbsolutePath();
    if (filePath.startsWith(rootPath)) {
      String relative = filePath.substring(rootPath.length());
      if (relative.startsWith(File.separator)) {
        relative = relative.substring(1);
      }
      return relative;
    }
    return file.getName();
  }
}
