package me.roundaround.custompaintings.resource.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import me.roundaround.custompaintings.resource.file.Image;
import me.roundaround.custompaintings.resource.file.Metadata;
import me.roundaround.custompaintings.resource.file.Pack;
import me.roundaround.custompaintings.resource.file.PackReader;
import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import me.roundaround.custompaintings.testing.TestPacks;
import me.roundaround.custompaintings.util.CustomId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end coverage of the legacy → current pack migration: build a pre-3.0 pack on disk, read it
 * with {@link PackReader}, run it through {@link LegacyPackConverter}, then re-read the converted
 * archive and assert it is now a valid current-format pack that points back at its source.
 */
public class LegacyPackConverterTest extends BaseMinecraftTest {
  private static final String LEGACY_JSON = """
      {
        "id": "mypack",
        "name": "My Pack",
        "paintings": [ { "id": "foo", "name": "Foo", "artist": "Artist", "height": 2, "width": 3 } ],
        "migrations": [ { "id": "m1", "description": "moved", "pairs": [["a", "b"]] } ]
      }
      """;
  private static final String MCMETA = """
      { "pack": { "pack_format": 6, "description": "Legacy description" } }
      """;

  private static Metadata readLegacyPack(Path dir) throws IOException {
    Path packDir = TestPacks.builder()
        .file("custompaintings.json", LEGACY_JSON)
        .file("pack.mcmeta", MCMETA)
        .png("assets/mypack/textures/painting/foo.png", 4, 4)
        .png("pack.png", 8, 8)
        .writeDirectory(dir.resolve("mypack"));
    Metadata metadata = PackReader.readMetadata(packDir);
    assertNotNull(metadata);
    assertTrue(metadata.isLegacy());
    return metadata;
  }

  @Test
  void convertsLegacyPackIntoReadableCurrentPack(@TempDir Path dir) throws Exception {
    Metadata legacy = readLegacyPack(dir);
    String sourceUid = legacy.fileUid().stringValue();

    Path output = dir.resolve("converted").resolve("mypack.zip");
    boolean converted = LegacyPackConverter.getInstance().convertPack(legacy, output).get(30, TimeUnit.SECONDS);

    assertTrue(converted, "conversion should report success");
    assertTrue(Files.isRegularFile(output), "a zip archive should be written at the output path");

    Metadata reread = PackReader.readMetadata(output);
    assertNotNull(reread);
    assertFalse(reread.isLegacy(), "the converted pack has no pack.mcmeta, so it reads as current-format");

    Pack pack = reread.pack();
    assertNotNull(pack);
    assertEquals(1, pack.format(), "converted packs are written at format 1");
    assertEquals("mypack", pack.id());
    assertEquals("My Pack", pack.name());
    assertEquals("Legacy description", pack.description(), "the mcmeta description carries over");
    assertEquals(sourceUid, pack.sourceLegacyPack(), "the converted pack records its source UID");
  }

  @Test
  void convertedPackPreservesPaintingsMigrationsAndImages(@TempDir Path dir) throws Exception {
    Metadata legacy = readLegacyPack(dir);
    Path output = dir.resolve("converted").resolve("mypack.zip");
    assertTrue(LegacyPackConverter.getInstance().convertPack(legacy, output).get(30, TimeUnit.SECONDS));

    Metadata reread = PackReader.readMetadata(output);
    Pack pack = reread.pack();

    assertEquals(1, pack.paintings().size());
    assertEquals("foo", pack.paintings().getFirst().id());
    assertEquals(1, pack.migrations().size());
    assertEquals("m1", pack.migrations().getFirst().id());

    assertNotNull(reread.icon(), "the pack icon is rewritten into the converted archive");

    HashMap<CustomId, Image> images = PackReader.readPaintingImages(reread);
    assertTrue(
        images.containsKey(new CustomId("mypack", "foo")),
        "painting images are rewritten under the current images/ layout");
    assertEquals(4, images.get(new CustomId("mypack", "foo")).width());
  }

  @Test
  void dropsPaintingsWithMissingImageData(@TempDir Path dir) throws Exception {
    // A legacy entry whose texture file is absent should not survive conversion.
    Path packDir = TestPacks.builder()
        .file("custompaintings.json", """
            {
              "id": "mypack",
              "name": "My Pack",
              "paintings": [
                { "id": "present", "height": 1, "width": 1 },
                { "id": "missing", "height": 1, "width": 1 }
              ]
            }
            """)
        .file("pack.mcmeta", MCMETA)
        .png("assets/mypack/textures/painting/present.png", 2, 2)
        .png("pack.png", 8, 8)
        .writeDirectory(dir.resolve("mypack"));

    Metadata legacy = PackReader.readMetadata(packDir);
    Path output = dir.resolve("converted").resolve("mypack.zip");
    assertTrue(LegacyPackConverter.getInstance().convertPack(legacy, output).get(30, TimeUnit.SECONDS));

    Pack pack = PackReader.readMetadata(output).pack();
    assertEquals(1, pack.paintings().size(), "only the painting with image data is kept");
    assertEquals("present", pack.paintings().getFirst().id());
  }
}
