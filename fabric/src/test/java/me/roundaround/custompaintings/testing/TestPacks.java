package me.roundaround.custompaintings.testing;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

/**
 * Test fixtures for the file-management code. Builds Custom Paintings packs on disk as either an
 * exploded directory or a zip archive (optionally nested under a single top-level folder, which is
 * how packs zipped from a containing folder look), plus tiny in-memory PNGs to stand in for pack
 * icons and painting textures.
 */
public final class TestPacks {
  private TestPacks() {
  }

  /** A solid-color PNG encoded to bytes, ready to drop into a pack as an icon or painting. */
  public static byte[] pngBytes(int width, int height, int argb) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        image.setRGB(x, y, argb);
      }
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "png", out);
    return out.toByteArray();
  }

  public static byte[] pngBytes(int width, int height) throws IOException {
    return pngBytes(width, height, 0xFFFF0000);
  }

  public static PackBuilder builder() {
    return new PackBuilder();
  }

  /**
   * Collects a set of pack-relative entries, then materializes them either as a directory tree or a
   * zip. Entry paths always use {@code /} separators; on the directory writer they are resolved
   * against the host filesystem, which is fine for the POSIX test environment.
   */
  public static final class PackBuilder {
    private final LinkedHashMap<String, byte[]> entries = new LinkedHashMap<>();

    public PackBuilder file(String path, String content) {
      this.entries.put(path, content.getBytes(StandardCharsets.UTF_8));
      return this;
    }

    public PackBuilder file(String path, byte[] content) {
      this.entries.put(path, content);
      return this;
    }

    public PackBuilder png(String path, int width, int height) throws IOException {
      return this.file(path, pngBytes(width, height));
    }

    public Path writeDirectory(Path dir) throws IOException {
      Files.createDirectories(dir);
      for (var entry : this.entries.entrySet()) {
        Path target = dir.resolve(entry.getKey());
        Files.createDirectories(target.getParent());
        Files.write(target, entry.getValue());
      }
      return dir;
    }

    public Path writeZip(Path zipFile) throws IOException {
      return this.writeZip(zipFile, null);
    }

    /**
     * @param folderPrefix when non-null, every entry is nested under this top-level directory (and
     *                     the directory entry itself is written first), reproducing a pack that was
     *                     zipped together with its containing folder.
     */
    public Path writeZip(Path zipFile, String folderPrefix) throws IOException {
      Files.createDirectories(zipFile.getParent());
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
        String prefix = "";
        if (folderPrefix != null && !folderPrefix.isEmpty()) {
          prefix = folderPrefix.endsWith("/") ? folderPrefix : folderPrefix + "/";
          zos.putNextEntry(new ZipEntry(prefix));
          zos.closeEntry();
        }
        for (var entry : this.entries.entrySet()) {
          zos.putNextEntry(new ZipEntry(prefix + entry.getKey()));
          zos.write(entry.getValue());
          zos.closeEntry();
        }
      }
      return zipFile;
    }
  }
}
