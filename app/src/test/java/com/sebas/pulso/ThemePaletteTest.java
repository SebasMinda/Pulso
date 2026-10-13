package com.sebas.pulso;

import static org.junit.Assert.*;

import org.junit.Test;

/** Comprueba colores del tema, contraste y códigos hexadecimales. */
public class ThemePaletteTest {
  @Test
  public void originalKeepsExistingSurfaces() {
    ThemePalette p =
        new ThemePalette(ThemePalette.BACKGROUND, ThemePalette.BUTTON, ThemePalette.ACCENT);
    assertTrue(p.original());
    assertEquals(0xff1a1414, p.card());
    assertEquals(0xffbfb3a4, p.muted());
  }

  @Test
  public void hexadecimalRoundTrip() {
    assertEquals(0xffc9ad72, ThemePalette.parse(" #c9ad72 "));
    assertEquals("#00FF80", ThemePalette.hex(ThemePalette.parse("00ff80")));
  }

  @Test
  public void invalidCodesAreRejected() {
    for (String s : new String[] {"", "#123", "#12345678", "#GG0000", "1234567"})
      try {
        ThemePalette.parse(s);
        fail(s);
      } catch (IllegalArgumentException expected) {
      }
  }

  @Test
  public void textAdaptsToLightAndDark() {
    assertEquals(0xff101010, ThemePalette.foreground(0xffffffff));
    assertEquals(0xfffafafa, ThemePalette.foreground(0xff000000));
  }

  @Test
  public void shadesStayOpaqueAndBounded() {
    assertEquals(0xff000000, ThemePalette.mix(0xff000000, 0xffffffff, -1));
    assertEquals(0xffffffff, ThemePalette.mix(0xff000000, 0xffffffff, 2));
    assertEquals(0xff808080, ThemePalette.mix(0xff000000, 0xffffffff, .5f));
  }

  @Test
  public void inputColorsAreOpaque() {
    ThemePalette p = new ThemePalette(0xffffff, 0x123456, 0xabcdef);
    assertEquals(0xffffffff, p.background);
    assertEquals(0xff123456, p.button);
    assertEquals(0xffabcdef, p.accent);
    assertFalse(p.original());
  }
}
