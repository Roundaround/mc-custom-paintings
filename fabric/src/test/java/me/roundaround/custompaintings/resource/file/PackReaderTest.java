package me.roundaround.custompaintings.resource.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import me.roundaround.custompaintings.testing.TestPacks;
import me.roundaround.custompaintings.testing.TestPacks.PackBuilder;
import me.roundaround.custompaintings.util.CustomId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PackReaderTest extends BaseMinecraftTest {
  private static final String MODERN_JSON = """
      {
        "format": 2,
        "id": "mypack",
        "name": "My Pack",
        "description": "modern",
        "paintings": [
          { "id": "foo", "name": "Foo", "artist": "A", "height": 2, "width": 3 },
          { "id": "bar", "height": 1, "width": 1 }
        ]
      }
      """;
  private static final String LEGACY_JSON = """
      {
        "id": "mypack",
        "name": "My Pack",
        "paintings": [ { "id": "foo", "name": "Foo", "artist": "A", "height": 2, "width": 3 } ]
      }
      """;
  private static final String MCMETA = """
      { "pack": { "pack_format": 6, "description": "Legacy description" } }
      """;

  private static PackBuilder modernPack() throws IOException {
    return TestPacks.builder()
        .file("custompaintings.json", MODERN_JSON)
        .png("images/foo.png", 4, 4)
        .png("images/bar.png", 2, 2)
        .png("pack.png", 8, 8);
  }

  private static PackBuilder legacyPack() throws IOException {
    return TestPacks.builder()
        .file("custompaintings.json", LEGACY_JSON)
        .file("pack.mcmeta", MCMETA)
        .png("assets/mypack/textures/painting/foo.png", 4, 4)
        .png("pack.png", 8, 8);
  }

  @Test
  void readsModernDirectoryPack(@TempDir Path dir) throws IOException {
    Metadata metadata = PackReader.readMetadata(modernPack().writeDirectory(dir.resolve("mypack")));

    assertNotNull(metadata);
    assertFalse(metadata.isLegacy());
    assertNotNull(metadata.icon(), "pack.png should be read as the icon");
    assertNotNull(metadata.fileUid());

    Pack pack = metadata.pack();
    assertNotNull(pack);
    assertEquals(2, pack.format());
    assertEquals("mypack", pack.id());
    assertEquals("modern", pack.description());
    assertNull(pack.sourceLegacyPack());
    assertEquals(2, pack.paintings().size());
  }

  @Test
  void readsModernZipPack(@TempDir Path dir) throws IOException {
    Metadata metadata = PackReader.readMetadata(modernPack().writeZip(dir.resolve("mypack.zip")));

    assertNotNull(metadata);
    assertFalse(metadata.isLegacy());
    assertNotNull(metadata.icon());
    assertEquals("mypack", metadata.pack().id());
    assertEquals(2, metadata.pack().paintings().size());
  }

  @Test
  void readsModernZipNestedUnderTopLevelFolder(@TempDir Path dir) throws IOException {
    Path zip = modernPack().writeZip(dir.resolve("mypack.zip"), "mypack");
    Metadata metadata = PackReader.readMetadata(zip);

    assertNotNull(metadata, "the single top-level folder prefix should be transparently stripped");
    assertEquals("mypack", metadata.pack().id());

    HashMap<CustomId, Image> images = PackReader.readPaintingImages(metadata);
    assertTrue(images.containsKey(new CustomId("mypack", "foo")));
  }

  @Test
  void detectsLegacyPackWhenMcmetaPresent(@TempDir Path dir) throws IOException {
    Metadata metadata = PackReader.readMetadata(legacyPack().writeDirectory(dir.resolve("mypack")));

    assertNotNull(metadata);
    assertTrue(metadata.isLegacy());

    Pack pack = metadata.pack();
    assertNotNull(pack);
    assertEquals(-1, pack.format(), "the legacy Pack constructor marks format as -1");
    assertEquals("Legacy description", pack.description(), "description comes from pack.mcmeta");
    assertNull(pack.sourceLegacyPack());
    assertEquals(1, pack.paintings().size());
  }

  @Test
  void recognizesUnderscoreJsonFilename(@TempDir Path dir) throws IOException {
    Path pack = TestPacks.builder()
        .file("custom_paintings.json", MODERN_JSON)
        .png("images/foo.png", 4, 4)
        .writeDirectory(dir.resolve("mypack"));

    Metadata metadata = PackReader.readMetadata(pack);
    assertNotNull(metadata);
    assertEquals("mypack", metadata.pack().id());
  }

  @Test
  void fallsBackToIconPngThenNull(@TempDir Path dir) throws IOException {
    Metadata withIconPng = PackReader.readMetadata(TestPacks.builder()
        .file("custompaintings.json", MODERN_JSON)
        .png("images/foo.png", 4, 4)
        .png("icon.png", 8, 8)
        .writeDirectory(dir.resolve("with-icon-png")));
    assertNotNull(withIconPng.icon(), "icon.png is used when pack.png is absent");

    Metadata withoutIcon = PackReader.readMetadata(TestPacks.builder()
        .file("custompaintings.json", MODERN_JSON)
        .png("images/foo.png", 4, 4)
        .writeDirectory(dir.resolve("no-icon")));
    assertNotNull(withoutIcon, "a missing icon does not prevent reading the pack");
    assertNull(withoutIcon.icon());
  }

  @Test
  void returnsNullWhenNoMetadataFile(@TempDir Path dir) throws IOException {
    Path notAPack = TestPacks.builder().png("pack.png", 8, 8).writeDirectory(dir.resolve("not-a-pack"));
    assertNull(PackReader.readMetadata(notAPack));
  }

  @Test
  void returnsNullMetadataForMissingPath(@TempDir Path dir) {
    assertNull(PackReader.readMetadata(dir.resolve("does-not-exist")));
  }

  @Test
  void packIsNullWhenNoPaintings(@TempDir Path dir) throws IOException {
    Path pack = TestPacks.builder()
        .file("custompaintings.json", """
            { "format": 1, "id": "mypack", "name": "My Pack", "paintings": [] }
            """)
        .writeDirectory(dir.resolve("empty"));

    Metadata metadata = PackReader.readMetadata(pack);
    assertNotNull(metadata, "metadata is still produced");
    assertNull(metadata.pack(), "but a pack with no paintings is rejected to a null pack");
  }

  @Test
  void packIsNullWhenJsonMalformed(@TempDir Path dir) throws IOException {
    Path pack = TestPacks.builder()
        .file("custompaintings.json", "{ this is not valid json ")
        .writeDirectory(dir.resolve("broken"));

    Metadata metadata = PackReader.readMetadata(pack);
    assertNotNull(metadata);
    assertNull(metadata.pack());
  }

  @Test
  void readPaintingImagesUsesModernLayoutAndSkipsMissing(@TempDir Path dir) throws IOException {
    // foo.png exists, bar.png is intentionally absent.
    Path pack = TestPacks.builder()
        .file("custompaintings.json", MODERN_JSON)
        .png("images/foo.png", 4, 4)
        .writeDirectory(dir.resolve("mypack"));

    Metadata metadata = PackReader.readMetadata(pack);
    HashMap<CustomId, Image> images = PackReader.readPaintingImages(metadata);

    assertEquals(1, images.size());
    assertTrue(images.containsKey(new CustomId("mypack", "foo")));
    assertFalse(images.containsKey(new CustomId("mypack", "bar")), "missing image files are skipped");
    assertEquals(4, images.get(new CustomId("mypack", "foo")).width());
  }

  @Test
  void readPaintingImagesUsesLegacyAssetLayout(@TempDir Path dir) throws IOException {
    Metadata metadata = PackReader.readMetadata(legacyPack().writeDirectory(dir.resolve("mypack")));
    HashMap<CustomId, Image> images = PackReader.readPaintingImages(metadata);

    assertEquals(1, images.size());
    assertTrue(
        images.containsKey(new CustomId("mypack", "foo")),
        "legacy images live under assets/<pack>/textures/painting/");
  }
}
