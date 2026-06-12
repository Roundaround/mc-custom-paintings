package me.roundaround.custompaintings.resource.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import me.roundaround.custompaintings.entity.decoration.painting.MigrationData;
import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.InvalidIdException;
import org.junit.jupiter.api.Test;

public class MigrationTest extends BaseMinecraftTest {
  @Test
  void toDataNamespacesIdAndParsesPairs() {
    Migration migration = new Migration(
        "m1", "moved", List.of(List.of("a:b", "c:d"), List.of("plain", "other")));

    MigrationData data = migration.toData("mypack");

    assertEquals(new CustomId("mypack", "m1"), data.id());
    assertEquals("moved", data.description());
    assertEquals(2, data.pairs().size());
    assertEquals(new CustomId("c", "d"), data.pairs().get(new CustomId("a", "b")));
    // Bare ids inside pairs gain the default namespace via CustomId.parse.
    assertEquals(CustomId.parse("other"), data.pairs().get(CustomId.parse("plain")));
  }

  @Test
  void toDataWithNoPairsProducesEmptyMap() {
    MigrationData data = new Migration("m1", "desc", List.of()).toData("mypack");
    assertEquals(0, data.pairs().size());
  }

  @Test
  void validateIdsAcceptsWellFormedIds() {
    Migration migration = new Migration("m1", "desc", List.of(List.of("a:b", "c:d")));
    assertDoesNotThrow(() -> migration.validateIds(0));
  }

  @Test
  void validateIdsRejectsBadMigrationId() {
    Migration migration = new Migration("bad:id", "desc", List.of());
    assertThrows(InvalidIdException.class, () -> migration.validateIds(0));
  }

  @Test
  void validateIdsRejectsBadPairId() {
    Migration migration = new Migration("m1", "desc", List.of(List.of("ok", "too:many:colons")));
    assertThrows(InvalidIdException.class, () -> migration.validateIds(0));
  }
}
