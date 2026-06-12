package me.roundaround.custompaintings.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import me.roundaround.custompaintings.util.InvalidIdException;
import org.junit.jupiter.api.Test;

/**
 * {@link PackResource} is the on-disk shape of a current-format pack and the exact thing the legacy
 * converter serializes. Its custom {@code TypeAdapter} mirrors
 * {@link me.roundaround.custompaintings.resource.file.json.CustomPaintingsJson}, so the same parsing
 * guarantees must hold.
 */
public class PackResourceTest extends BaseMinecraftTest {
  private static PackResource parse(String json) {
    return CustomPaintingsMod.GSON.fromJson(json, PackResource.class);
  }

  @Test
  void parsesAllFields() {
    PackResource pack = parse("""
        {
          "format": 1,
          "id": "mypack",
          "name": "My Pack",
          "description": "desc",
          "sourceLegacyPack": "1deadbeef",
          "paintings": [ { "id": "foo", "name": "Foo", "artist": "A", "height": 2, "width": 4 } ],
          "migrations": [ { "id": "m1", "description": "moved", "pairs": [["a:b", "c:d"]] } ]
        }
        """);

    assertEquals(1, pack.format());
    assertEquals("mypack", pack.id());
    assertEquals("My Pack", pack.name());
    assertEquals("desc", pack.description());
    assertEquals("1deadbeef", pack.sourceLegacyPack());

    PaintingResource painting = pack.paintings().getFirst();
    assertEquals("foo", painting.id());
    assertEquals(2, painting.height());
    assertEquals(4, painting.width());

    MigrationResource migration = pack.migrations().getFirst();
    assertEquals("m1", migration.id());
    assertEquals(List.of(List.of("a:b", "c:d")), migration.pairs());
  }

  @Test
  void appliesDefaultsForMissingAndNullFields() {
    PackResource pack = parse("""
        {
          "id": "mypack",
          "name": "My Pack",
          "description": null,
          "paintings": null
        }
        """);
    assertEquals(1, pack.format());
    assertNull(pack.description());
    assertNull(pack.sourceLegacyPack());
    assertTrue(pack.paintings().isEmpty());
    assertTrue(pack.migrations().isEmpty());
  }

  @Test
  void unknownKeysAreIgnored() {
    PackResource pack = parse("""
        { "id": "mypack", "name": "My Pack", "vendorJunk": { "x": 1 }, "paintings": [] }
        """);
    assertEquals("mypack", pack.id());
    assertTrue(pack.paintings().isEmpty());
  }

  @Test
  void writeOmitsBlankOptionalFields() {
    PackResource pack = new PackResource(
        1, "mypack", "My Pack", "  ", "", List.of(new PaintingResource("foo", null, null, null, null)), List.of());
    String json = CustomPaintingsMod.GSON.toJson(pack);

    assertFalse(json.contains("description"), "blank description is omitted");
    assertFalse(json.contains("sourceLegacyPack"), "blank sourceLegacyPack is omitted");
    assertFalse(json.contains("migrations"), "empty migrations are omitted");
    assertTrue(json.contains("\"format\""));
  }

  @Test
  void roundTripPreservesData() {
    PackResource original = new PackResource(
        1,
        "mypack",
        "My Pack",
        "desc",
        "1deadbeef",
        List.of(new PaintingResource("foo", "Foo", "Artist", 2, 4)),
        List.of(new MigrationResource("m1", "moved", List.of(List.of("a:b", "c:d")))));

    PackResource reparsed = parse(CustomPaintingsMod.GSON.toJson(original));
    assertEquals(original, reparsed);
  }

  @Test
  void validateIdsRejectsBadMigrationPairId() {
    PackResource valid = new PackResource(
        1, "mypack", "My Pack", null, null,
        List.of(new PaintingResource("foo", null, null, null, null)),
        List.of(new MigrationResource("m1", "desc", List.of(List.of("a:b", "c:d")))));
    assertDoesNotThrow(valid::validateIds);

    PackResource invalid = new PackResource(
        1, "mypack", "My Pack", null, null,
        List.of(new PaintingResource("foo", null, null, null, null)),
        List.of(new MigrationResource("m1", "desc", List.of(List.of("a:b", "bad:too:many")))));
    assertThrows(InvalidIdException.class, invalid::validateIds);
  }
}
