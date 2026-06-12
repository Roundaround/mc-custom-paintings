package me.roundaround.custompaintings.resource.file.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import me.roundaround.custompaintings.CustomPaintingsMod;
import me.roundaround.custompaintings.resource.file.Migration;
import me.roundaround.custompaintings.resource.file.Painting;
import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import me.roundaround.custompaintings.util.InvalidIdException;
import org.junit.jupiter.api.Test;

public class CustomPaintingsJsonTest extends BaseMinecraftTest {
  private static CustomPaintingsJson parse(String json) {
    return CustomPaintingsMod.GSON.fromJson(json, CustomPaintingsJson.class);
  }

  @Test
  void parsesAllFields() {
    CustomPaintingsJson pack = parse("""
        {
          "format": 3,
          "id": "mypack",
          "name": "My Pack",
          "description": "A description",
          "sourceLegacyPack": "1abc",
          "paintings": [
            { "id": "foo", "name": "Foo", "artist": "Artist", "height": 2, "width": 3 },
            { "id": "bar" }
          ],
          "migrations": [
            { "id": "m1", "description": "moved", "pairs": [["a:b", "c:d"]] }
          ]
        }
        """);

    assertEquals(3, pack.format());
    assertEquals("mypack", pack.id());
    assertEquals("My Pack", pack.name());
    assertEquals("A description", pack.description());
    assertEquals("1abc", pack.sourceLegacyPack());

    assertEquals(2, pack.paintings().size());
    Painting foo = pack.paintings().getFirst();
    assertEquals("foo", foo.id());
    assertEquals("Foo", foo.name());
    assertEquals("Artist", foo.artist());
    assertEquals(2, foo.height());
    assertEquals(3, foo.width());

    Painting bar = pack.paintings().get(1);
    assertEquals("bar", bar.id());
    assertNull(bar.name(), "missing painting fields parse to null");
    assertNull(bar.height());

    assertEquals(1, pack.migrations().size());
    Migration migration = pack.migrations().getFirst();
    assertEquals("m1", migration.id());
    assertEquals("moved", migration.description());
    assertEquals(List.of(List.of("a:b", "c:d")), migration.pairs());
  }

  @Test
  void formatDefaultsToOneWhenMissing() {
    CustomPaintingsJson pack = parse("""
        { "id": "mypack", "name": "My Pack", "paintings": [ { "id": "foo" } ] }
        """);
    assertEquals(1, pack.format());
    assertNull(pack.description());
    assertNull(pack.sourceLegacyPack());
    assertTrue(pack.migrations().isEmpty(), "missing migrations parse to an empty list, not null");
  }

  @Test
  void explicitNullsFallBackToDefaults() {
    CustomPaintingsJson pack = parse("""
        {
          "format": null,
          "id": "mypack",
          "name": "My Pack",
          "description": null,
          "sourceLegacyPack": null,
          "paintings": null,
          "migrations": null
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
    CustomPaintingsJson pack = parse("""
        {
          "id": "mypack",
          "name": "My Pack",
          "extraNumber": 42,
          "extraObject": { "nested": [1, 2, 3] },
          "paintings": [ { "id": "foo", "unexpected": true } ]
        }
        """);
    assertEquals("mypack", pack.id());
    assertEquals(1, pack.paintings().size());
    assertEquals("foo", pack.paintings().getFirst().id());
  }

  @Test
  void writeOmitsBlankOptionalFields() {
    CustomPaintingsJson pack = new CustomPaintingsJson(
        1, "mypack", "My Pack", null, null, List.of(new Painting("foo", null, null, null, null)), List.of());
    String json = CustomPaintingsMod.GSON.toJson(pack);

    assertFalse(json.contains("description"), "null description should be omitted");
    assertFalse(json.contains("sourceLegacyPack"), "null sourceLegacyPack should be omitted");
    assertFalse(json.contains("migrations"), "empty migrations should be omitted");
    assertTrue(json.contains("\"paintings\""), "paintings array is always written");
  }

  @Test
  void roundTripPreservesData() {
    CustomPaintingsJson original = new CustomPaintingsJson(
        2,
        "mypack",
        "My Pack",
        "A description",
        "1abc",
        List.of(new Painting("foo", "Foo", "Artist", 2, 3)),
        List.of(new Migration("m1", "moved", List.of(List.of("a:b", "c:d")))));

    CustomPaintingsJson reparsed = parse(CustomPaintingsMod.GSON.toJson(original));
    assertEquals(original, reparsed);
  }

  @Test
  void validateIdsAcceptsValidAndRejectsBadPaintingId() {
    CustomPaintingsJson valid = new CustomPaintingsJson(
        1, "mypack", "My Pack", null, null, List.of(new Painting("foo", null, null, null, null)), List.of());
    assertDoesNotThrow(valid::validateIds);

    CustomPaintingsJson invalid = new CustomPaintingsJson(
        1, "mypack", "My Pack", null, null, List.of(new Painting("bad:id", null, null, null, null)), List.of());
    assertThrows(InvalidIdException.class, invalid::validateIds);
  }
}
