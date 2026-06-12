package me.roundaround.custompaintings.resource.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;

import me.roundaround.custompaintings.resource.file.Image.Color;
import me.roundaround.custompaintings.resource.file.Image.Operation;
import me.roundaround.custompaintings.testing.BaseMinecraftTest;
import me.roundaround.custompaintings.testing.TestPacks;
import org.junit.jupiter.api.Test;

public class ImageTest extends BaseMinecraftTest {
  @Test
  void emptyImageIsEmpty() {
    Image empty = Image.empty();
    assertTrue(empty.isEmpty());
    assertEquals(0, empty.getSize());
    assertEquals(0, empty.width());
    assertEquals(0, empty.height());
  }

  @Test
  void fromBytesRoundTripsThroughGetBytes() {
    byte[] bytes = {10, 20, 30, 40, 50, 60, 70, (byte) 200};
    Image image = Image.fromBytes(bytes, 2, 1);

    assertEquals(2, image.width());
    assertEquals(1, image.height());
    assertEquals(8, image.getSize());
    assertArrayEquals(bytes, image.getBytes());
  }

  @Test
  void fromBytesPadsShortInput() {
    // Only one pixel's worth of data for a two-pixel image; the remainder is zero-filled.
    byte[] bytes = {10, 20, 30, 40};
    Image image = Image.fromBytes(bytes, 2, 1);

    assertEquals(8, image.getSize());
    Color second = image.getPixel(1, 0);
    assertEquals(0, second.getRed());
    assertEquals(0, second.getAlpha());
  }

  @Test
  void getIndexStoresPixelsColumnMajor() {
    assertEquals(0, Image.getIndex(2, 0, 0));
    assertEquals(1, Image.getIndex(2, 0, 1));
    assertEquals(2, Image.getIndex(2, 1, 0));
    assertEquals(3, Image.getIndex(2, 1, 1));
  }

  @Test
  void getPixelClampsOutOfBoundsToTransparent() {
    Image image = solid(2, 2, new Color(255, 255, 255, 255));
    assertTrue(image.getPixel(-1, 0).isTransparent());
    assertTrue(image.getPixel(0, 2).isTransparent());
    assertFalse(image.getPixel(1, 1).isTransparent());
  }

  @Test
  void readDecodesPngBytesPreservingColors() throws Exception {
    byte[] png = TestPacks.pngBytes(4, 3, 0xFF3366CC);
    Image image = Image.read(png);

    assertEquals(4, image.width());
    assertEquals(3, image.height());
    assertEquals(0xFF3366CC, image.getARGB(0, 0));
    assertEquals(0xFF3366CC, image.getARGB(3, 2));
  }

  @Test
  void readMatchesManualBufferedImageTranslation() throws Exception {
    BufferedImage source = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
    source.setRGB(0, 0, 0xFFAABBCC);
    source.setRGB(1, 0, 0xFF112233);

    Image image = Image.read(source);
    assertEquals(0xFFAABBCC, image.getARGB(0, 0));
    assertEquals(0xFF112233, image.getARGB(1, 0));
  }

  @Test
  void readReturnsNullForNonImageStream() throws Exception {
    assertNull(Image.read(new byte[] {0, 1, 2, 3}));
  }

  @Test
  void equalsAndHashUseHashWidthHeight() {
    byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8};
    Image a = Image.fromBytes(bytes, 2, 1);
    Image b = Image.fromBytes(bytes.clone(), 2, 1);
    Image differentDims = Image.fromBytes(bytes, 1, 2);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, differentDims);
  }

  @Test
  void colorArgbRoundTripAndAbgrSwap() {
    Color color = Color.fromARGB(0x11223344);
    assertEquals(0x22, color.getRed());
    assertEquals(0x33, color.getGreen());
    assertEquals(0x44, color.getBlue());
    assertEquals(0x11, color.getAlpha());
    assertEquals(0x11223344, color.getARGB());
    assertEquals(0x11443322, color.getABGR());
  }

  @Test
  void colorTransformations() {
    Color color = new Color(10, 20, 30, 40);
    assertEquals(new Color(245, 235, 225, 40), color.invert());
    assertEquals(255, color.removeAlpha().getAlpha());
    assertTrue(Color.transparent().isTransparent());
    assertEquals(new Color(50, 50, 50, 100), Color.average(new Color(0, 0, 0, 100), new Color(100, 100, 100, 100)));
  }

  @Test
  void colorLayerShortCircuitsOnTransparency() {
    Color opaque = new Color(255, 0, 0, 255);
    assertEquals(opaque, Color.layer(opaque, Color.transparent()));
    assertEquals(opaque, Color.layer(Color.transparent(), opaque));
  }

  @Test
  void invertOperationInvertsEveryPixel() {
    Image image = solid(2, 2, new Color(10, 20, 30, 255));
    Image inverted = image.apply(Operation.invert());
    assertEquals(new Color(245, 235, 225, 255), inverted.getPixel(0, 0));
    assertEquals(new Color(245, 235, 225, 255), inverted.getPixel(1, 1));
  }

  @Test
  void resizeOperationGrowsWithTransparentFill() {
    Image image = solid(2, 2, new Color(10, 20, 30, 255));
    Image resized = image.apply(Operation.resize(3, 3));

    assertEquals(3, resized.width());
    assertEquals(3, resized.height());
    assertFalse(resized.getPixel(0, 0).isTransparent(), "original region is preserved");
    assertTrue(resized.getPixel(2, 2).isTransparent(), "new cells default to transparent");
  }

  @Test
  void translateOperationShiftsPixels() {
    Color filled = new Color(10, 20, 30, 255);
    Image image = solid(2, 2, filled);
    Image translated = image.apply(Operation.translate(1, 0));

    assertTrue(translated.getPixel(0, 0).isTransparent(), "vacated column becomes empty");
    assertEquals(filled, translated.getPixel(1, 0), "content shifts one column to the right");
  }

  private static Image solid(int width, int height, Color color) {
    Color[] pixels = new Color[width * height];
    for (int i = 0; i < pixels.length; i++) {
      pixels[i] = color;
    }
    return Image.fromPixels(pixels, width, height);
  }
}
