package me.roundaround.custompaintings.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link PackFileUid} is the pre-3.0 sibling of {@link me.roundaround.custompaintings.resource.file.FileUid}
 * and shares its FNV-1a + base62 encoding. These tests pin the same behavior so the two stay in sync.
 */
public class PackFileUidTest {
  private static final int LENGTH = 33;

  @Test
  void stringValueHasExpectedLayout() {
    String value = new PackFileUid(true, "pack", 0L, 0L).stringValue();
    assertEquals(LENGTH, value.length());
    assertEquals('1', value.charAt(0));
    assertEquals("0000000000000000", value.substring(17));
    assertTrue(value.substring(1, 17).matches("[0-9a-f]{16}"));
  }

  @Test
  void sizeAndTimestampAreBase62ZeroPadded() {
    PackFileUid uid = new PackFileUid(true, "pack", 62L, 61L);
    assertEquals("00000010", uid.stringValue().substring(17, 25));
    assertEquals("0000000Z", uid.stringValue().substring(25, 33));
  }

  @Test
  void equalsComparesOnlyStringValue() {
    PackFileUid base = new PackFileUid(false, "pack", 5L, 9L);
    assertEquals(base, new PackFileUid(false, "pack", 5L, 9L));
    assertNotEquals(base, new PackFileUid(true, "pack", 5L, 9L));
    assertNotEquals(base, new PackFileUid(false, "renamed", 5L, 9L));
  }

  @Test
  void matchesFileUidEncodingForSameInputs() {
    // The two implementations must produce identical UIDs, since converted packs reference legacy
    // packs by this value.
    var legacy = new PackFileUid(true, "pack", 123L, 456L).stringValue();
    var current = new me.roundaround.custompaintings.resource.file.FileUid(true, "pack", 123L, 456L).stringValue();
    assertEquals(legacy, current);
  }
}
