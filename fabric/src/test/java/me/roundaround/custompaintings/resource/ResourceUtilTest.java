package me.roundaround.custompaintings.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import com.google.common.io.ByteSource;
import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import me.roundaround.custompaintings.testing.TestPacks;
import me.roundaround.custompaintings.util.CustomId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResourceUtilTest extends BaseMinecraftTest {
  @Test
  void stripTrailingSeparatorHandlesBothSlashes() {
    assertEquals("path", ResourceUtil.stripTrailingSeparator("path/"));
    assertEquals("path", ResourceUtil.stripTrailingSeparator("path\\"));
    assertEquals("path", ResourceUtil.stripTrailingSeparator("path"));
    assertEquals("a/b", ResourceUtil.stripTrailingSeparator("a/b"));
  }

  @Test
  void getFolderPrefixDetectsSingleTopLevelFolder(@TempDir Path dir) throws Exception {
    Path nested = TestPacks.builder()
        .file("custompaintings.json", "{}")
        .file("images/foo.png", "x")
        .writeZip(dir.resolve("nested.zip"), "mypack");
    try (ZipFile zip = new ZipFile(nested.toFile())) {
      assertEquals("mypack/", ResourceUtil.getFolderPrefix(zip));
    }

    Path flat = TestPacks.builder()
        .file("custompaintings.json", "{}")
        .writeZip(dir.resolve("flat.zip"));
    try (ZipFile zip = new ZipFile(flat.toFile())) {
      assertEquals("", ResourceUtil.getFolderPrefix(zip));
    }
  }

  @Test
  void getImageZipEntrySanitizesPathSegments(@TempDir Path dir) throws Exception {
    Path zipPath = TestPacks.builder().file("images/foo.png", "x").writeZip(dir.resolve("pack.zip"));
    try (ZipFile zip = new ZipFile(zipPath.toFile())) {
      assertEquals("images/foo.png", ResourceUtil.getImageZipEntry(zip, "images/", "foo.png").getName());
      // Null and blank segments are dropped before joining.
      assertEquals("images/foo.png", ResourceUtil.getImageZipEntry(zip, List.of("images", "", "foo.png")).getName());
      assertNull(ResourceUtil.getImageZipEntry(zip, "images", "missing.png"));
    }
  }

  @Test
  void isPaintingPackForDirectory(@TempDir Path dir) throws Exception {
    Path pack = TestPacks.builder().file("custompaintings.json", "{}").writeDirectory(dir.resolve("pack"));
    assertTrue(ResourceUtil.isPaintingPack(pack));

    Path notPack = TestPacks.builder().file("readme.txt", "hi").writeDirectory(dir.resolve("not-a-pack"));
    assertFalse(ResourceUtil.isPaintingPack(notPack));

    assertFalse(ResourceUtil.isPaintingPack(dir.resolve("does-not-exist")));
  }

  @Test
  void isPaintingPackForZip(@TempDir Path dir) throws Exception {
    Path pack = TestPacks.builder().file("custompaintings.json", "{}").writeZip(dir.resolve("pack.zip"));
    assertTrue(ResourceUtil.isPaintingPack(pack));

    Path notPack = TestPacks.builder().file("other.json", "{}").writeZip(dir.resolve("other.zip"));
    assertFalse(ResourceUtil.isPaintingPack(notPack));
  }

  @Test
  void directorySizeAndFileSizeSumRegularFiles(@TempDir Path dir) throws Exception {
    Files.write(dir.resolve("a.bin"), new byte[5]);
    Files.write(dir.resolve("b.bin"), new byte[3]);

    assertEquals(8L, ResourceUtil.directorySize(dir));
    assertEquals(8L, ResourceUtil.fileSize(dir), "fileSize on a directory recurses into a total");
    assertEquals(5L, ResourceUtil.fileSize(dir.resolve("a.bin")));
  }

  @Test
  void lastModifiedReturnsZeroForMissingFile(@TempDir Path dir) {
    assertEquals(0L, ResourceUtil.lastModified(dir.resolve("missing")));
    assertEquals(0L, ResourceUtil.fileSize(dir.resolve("missing")));
  }

  @Test
  void getAllImageIdsCombinesPackIconsAndPaintings() {
    Set<CustomId> ids = ResourceUtil.getAllImageIds(
        List.of("p1", "p2"), List.of(new CustomId("p1", "foo")));

    assertEquals(3, ids.size());
    assertTrue(ids.contains(PackIcons.customId("p1")));
    assertTrue(ids.contains(PackIcons.customId("p2")));
    assertTrue(ids.contains(new CustomId("p1", "foo")));
  }

  @Test
  void hashOrEmptyMatchesKnownSha256() {
    // Sanity-check the hashing against a well-known SHA-256 vector.
    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        ResourceUtil.hashOrEmpty(ByteSource.wrap("abc".getBytes())));
  }

  @Test
  void hashImagesIsOrderIndependentAndContentSensitive() {
    Image first = Image.fromBytes(new byte[] {1, 2, 3, 4}, 1, 1);
    Image second = Image.fromBytes(new byte[] {5, 6, 7, 8}, 1, 1);
    CustomId idA = new CustomId("pack", "a");
    CustomId idB = new CustomId("pack", "b");

    // Insertion order differs but the hash sorts by id, so the result must match.
    String hashAb = ResourceUtil.hashImages(orderedMap(idA, first, idB, second));
    String hashBa = ResourceUtil.hashImages(orderedMap(idB, second, idA, first));
    assertEquals(hashAb, hashBa);

    String changed = ResourceUtil.hashImages(orderedMap(idA, second, idB, first));
    assertNotEquals(hashAb, changed, "different image content yields a different hash");
  }

  private static Map<CustomId, Image> orderedMap(CustomId k1, Image v1, CustomId k2, Image v2) {
    var map = new java.util.LinkedHashMap<CustomId, Image>();
    map.put(k1, v1);
    map.put(k2, v2);
    return map;
  }
}
