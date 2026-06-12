package me.roundaround.custompaintings.resource.file;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import me.roundaround.custompaintings.entity.decoration.painting.PaintingData;
import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import me.roundaround.custompaintings.util.CustomId;
import me.roundaround.custompaintings.util.InvalidIdException;
import org.junit.jupiter.api.Test;

public class PaintingTest extends BaseMinecraftTest {
  @Test
  void toDataNamespacesIdAndCopiesDimensions() {
    PaintingData data = new Painting("foo", "Foo", "Artist", 2, 3).toData("mypack");

    assertEquals(new CustomId("mypack", "foo"), data.id());
    assertEquals(3, data.width());
    assertEquals(2, data.height());
    assertEquals("Foo", data.name());
    assertEquals("Artist", data.artist());
  }

  @Test
  void toDataConvertsNullNameAndArtistToEmptyStrings() {
    PaintingData data = new Painting("foo", null, null, 1, 1).toData("mypack");
    assertEquals("", data.name());
    assertEquals("", data.artist());
  }

  @Test
  void validateIdAcceptsValidAndRejectsInvalid() {
    assertDoesNotThrow(() -> new Painting("foo", null, null, null, null).validateId(0));
    assertThrows(InvalidIdException.class, () -> new Painting("bad:id", null, null, null, null).validateId(0));
  }
}
