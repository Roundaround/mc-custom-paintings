package me.roundaround.custompaintings.resource.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class FileUidTest {
  // stringValue layout: [0] file flag, [1,17) 16-char name hash, [17,25) base62 timestamp, [25,33) base62 size.
  private static final int LENGTH = 33;

  @Test
  void stringValueHasExpectedLayout() {
    FileUid uid = new FileUid(true, "pack", 0L, 0L);
    String value = uid.stringValue();

    assertEquals(LENGTH, value.length());
    assertEquals('1', value.charAt(0), "file flag");
    assertEquals("0000000000000000", value.substring(17), "zero timestamp and size pad to all zeros");
    assertTrue(value.substring(1, 17).matches("[0-9a-f]{16}"), "name hash is 16 lowercase hex digits");
  }

  @Test
  void fileFlagReflectsIsFile() {
    assertEquals('1', new FileUid(true, "pack", 1L, 1L).stringValue().charAt(0));
    assertEquals('0', new FileUid(false, "pack", 1L, 1L).stringValue().charAt(0));
  }

  @Test
  void sizeIsBase62ZeroPadded() {
    assertEquals("00000001", sizeSegment(new FileUid(true, "pack", 0L, 1L)));
    assertEquals("00000010", sizeSegment(new FileUid(true, "pack", 0L, 62L)));
    assertEquals("0000000Z", sizeSegment(new FileUid(true, "pack", 0L, 61L)));
    assertEquals("0000000a", sizeSegment(new FileUid(true, "pack", 0L, 10L)));
  }

  @Test
  void timestampIsBase62ZeroPadded() {
    assertEquals("00000010", timestampSegment(new FileUid(true, "pack", 62L, 0L)));
    assertEquals("0000000Z", timestampSegment(new FileUid(true, "pack", 61L, 0L)));
  }

  @Test
  void isDeterministicForSameInputs() {
    FileUid first = new FileUid(true, "pack", 123L, 456L);
    FileUid second = new FileUid(true, "pack", 123L, 456L);
    assertEquals(first.stringValue(), second.stringValue());
  }

  @Test
  void nameHashVariesWithFilename() {
    String hashA = new FileUid(true, "alpha", 0L, 0L).stringValue().substring(1, 17);
    String hashB = new FileUid(true, "beta", 0L, 0L).stringValue().substring(1, 17);
    assertNotEquals(hashA, hashB);
  }

  @Test
  void equalsComparesOnlyStringValue() {
    FileUid base = new FileUid(true, "pack", 123L, 456L);

    assertEquals(base, new FileUid(true, "pack", 123L, 456L));
    assertNotEquals(base, new FileUid(true, "pack", 123L, 457L));
    assertNotEquals(base, new FileUid(true, "other", 123L, 456L));
    assertNotEquals(base, new FileUid(false, "pack", 123L, 456L));
  }

  private static String timestampSegment(FileUid uid) {
    return uid.stringValue().substring(17, 25);
  }

  private static String sizeSegment(FileUid uid) {
    return uid.stringValue().substring(25, 33);
  }
}
