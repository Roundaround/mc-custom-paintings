package me.roundaround.custompaintings.resource.file.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.resource.file.Migration;
import me.roundaround.custompaintings.resource.file.Painting;
import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import org.junit.jupiter.api.Test;

public class LegacyCustomPaintingsJsonTest extends BaseMinecraftTest {
  private static LegacyCustomPaintingsJson parse(String json) {
    return CustomPaintingsMod.GSON.fromJson(json, LegacyCustomPaintingsJson.class);
  }

  @Test
  void parsesIdNameAndPaintings() {
    LegacyCustomPaintingsJson pack = parse("""
        {
          "id": "legacypack",
          "name": "Legacy Pack",
          "paintings": [
            { "id": "foo", "name": "Foo", "artist": "Artist", "height": 1, "width": 1 }
          ]
        }
        """);

    assertEquals("legacypack", pack.id());
    assertEquals("Legacy Pack", pack.name());
    assertEquals(1, pack.paintings().size());
    assertEquals("foo", pack.paintings().getFirst().id());
    assertTrue(pack.migrations().isEmpty(), "missing migrations parse to an empty list");
  }

  @Test
  void ignoresModernOnlyFields() {
    // A legacy custompaintings.json predates format/description/sourceLegacyPack; if present they
    // should simply be skipped rather than breaking the parse.
    LegacyCustomPaintingsJson pack = parse("""
        {
          "format": 1,
          "id": "legacypack",
          "name": "Legacy Pack",
          "description": "ignored here",
          "paintings": [ { "id": "foo" } ]
        }
        """);
    assertEquals("legacypack", pack.id());
    assertEquals(1, pack.paintings().size());
  }

  @Test
  void parsesMigrations() {
    LegacyCustomPaintingsJson pack = parse("""
        {
          "id": "legacypack",
          "name": "Legacy Pack",
          "paintings": [ { "id": "foo" } ],
          "migrations": [ { "id": "m1", "description": "moved", "pairs": [["a", "b"], ["c", "d"]] } ]
        }
        """);

    assertEquals(1, pack.migrations().size());
    Migration migration = pack.migrations().getFirst();
    assertEquals("m1", migration.id());
    assertEquals(List.of(List.of("a", "b"), List.of("c", "d")), migration.pairs());
  }

  @Test
  void writeOmitsEmptyMigrationsButAlwaysWritesPaintings() {
    LegacyCustomPaintingsJson pack = new LegacyCustomPaintingsJson(
        "legacypack", "Legacy Pack", List.of(new Painting("foo", null, null, null, null)), List.of());
    String json = CustomPaintingsMod.GSON.toJson(pack);

    assertTrue(json.contains("\"paintings\""));
    assertFalse(json.contains("migrations"));
    assertFalse(json.contains("format"), "legacy output never carries a format field");
  }

  @Test
  void roundTripPreservesData() {
    LegacyCustomPaintingsJson original = new LegacyCustomPaintingsJson(
        "legacypack",
        "Legacy Pack",
        List.of(new Painting("foo", "Foo", "Artist", 1, 2)),
        List.of(new Migration("m1", "moved", List.of(List.of("a", "b")))));

    LegacyCustomPaintingsJson reparsed = parse(CustomPaintingsMod.GSON.toJson(original));
    assertEquals(original, reparsed);
  }
}
