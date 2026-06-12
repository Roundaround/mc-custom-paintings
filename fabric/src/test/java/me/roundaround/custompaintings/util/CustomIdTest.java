package me.roundaround.custompaintings.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.mojang.serialization.DataResult;
import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class CustomIdTest extends BaseMinecraftTest {
  @Test
  void parsesFullyQualifiedId() {
    CustomId id = CustomId.parse("mypack:foo");
    assertEquals("mypack", id.pack());
    assertEquals("foo", id.resource());
  }

  @Test
  void parseDefaultsNamespaceWhenNoColon() {
    CustomId id = CustomId.parse("foo");
    assertEquals(Identifier.DEFAULT_NAMESPACE, id.pack());
    assertEquals("foo", id.resource());
  }

  @Test
  void parseSplitsOnFirstColonOnly() {
    CustomId id = CustomId.parse("pack:nested:resource");
    assertEquals("pack", id.pack());
    assertEquals("nested:resource", id.resource());
  }

  @Test
  void toStringRoundTripsThroughParse() {
    CustomId id = new CustomId("mypack", "foo");
    assertEquals("mypack:foo", id.toString());
    assertEquals(id, CustomId.parse(id.toString()));
  }

  @Test
  void fromAndToIdentifier() {
    Identifier identifier = Identifier.fromNamespaceAndPath("mypack", "foo");
    CustomId id = CustomId.from(identifier);
    assertEquals("mypack", id.pack());
    assertEquals("foo", id.resource());
    assertEquals(identifier, id.toIdentifier());
  }

  @Test
  void translationKeys() {
    CustomId id = new CustomId("mypack", "foo");
    assertEquals("mypack.foo", id.toTranslationKey());
    assertEquals("painting.mypack.foo", id.toTranslationKey("painting"));
    assertEquals("painting.mypack.foo.title", id.toTranslationKey("painting", "title"));
  }

  @Test
  void equalsAndHashCodeUseBothParts() {
    CustomId a = new CustomId("mypack", "foo");
    CustomId b = new CustomId("mypack", "foo");
    CustomId differentResource = new CustomId("mypack", "bar");
    CustomId differentPack = new CustomId("other", "foo");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertFalse(a.equals(differentResource));
    assertFalse(a.equals(differentPack));
  }

  @Test
  void compareToOrdersByResourceThenPack() {
    CustomId resourceA = new CustomId("z", "a");
    CustomId resourceB = new CustomId("a", "b");
    assertTrue(resourceA.compareTo(resourceB) < 0, "resource 'a' should sort before resource 'b'");

    CustomId packA = new CustomId("a", "same");
    CustomId packZ = new CustomId("z", "same");
    assertTrue(packA.compareTo(packZ) < 0, "ties on resource fall back to pack ordering");
    assertEquals(0, packA.compareTo(new CustomId("a", "same")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"foo", "MixedCase", "with_underscore", "123", "a-b"})
  void validPartsAccepted(String part) {
    assertTrue(CustomId.isPartValid(part));
  }

  @ParameterizedTest
  @ValueSource(strings = {"has space", "has:colon", "has'quote", "has\"quote", ""})
  void invalidPartsRejected(String part) {
    assertFalse(CustomId.isPartValid(part));
  }

  @ParameterizedTest
  @ValueSource(strings = {"foo", "mypack:foo"})
  void validIdsAccepted(String value) {
    assertTrue(CustomId.isValid(value));
  }

  @ParameterizedTest
  @ValueSource(strings = {"two:colons:here", "has space", "trailing:"})
  void invalidIdsRejected(String value) {
    assertFalse(CustomId.isValid(value));
  }

  @Test
  void validatePartThrowsOnNullEmptyAndSeparators() {
    assertThrows(InvalidIdException.class, () -> CustomId.validatePart(null));
    assertThrows(InvalidIdException.class, () -> CustomId.validatePart(""));
    assertThrows(InvalidIdException.class, () -> CustomId.validatePart("has:colon"));
    assertThrows(InvalidIdException.class, () -> CustomId.validatePart("has space"));
    assertDoesNotThrow(() -> CustomId.validatePart("valid_part"));
  }

  @Test
  void validateAllowsSingleOrQualifiedShapeAndRejectsExtraColonsAndQuotes() {
    assertDoesNotThrow(() -> CustomId.validate("single", "resource"));
    assertDoesNotThrow(() -> CustomId.validate("pack:resource", "resource"));
    assertThrows(InvalidIdException.class, () -> CustomId.validate("too:many:colons", "resource"));
    assertThrows(InvalidIdException.class, () -> CustomId.validate("has'quote", "resource"));
    assertThrows(InvalidIdException.class, () -> CustomId.validate(null, "resource"));
  }

  @Test
  void validateDataResultSucceedsForValidAndErrorsForInvalid() {
    DataResult<CustomId> success = CustomId.validate("mypack:foo");
    assertTrue(success.result().isPresent());
    assertEquals(new CustomId("mypack", "foo"), success.result().get());

    DataResult<CustomId> failure = CustomId.validate("bad id");
    assertTrue(failure.error().isPresent());
  }
}
