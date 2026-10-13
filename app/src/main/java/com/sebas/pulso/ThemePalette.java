package com.sebas.pulso;

/** Define los colores del tema y calcula tonos y texto legible para cada fondo. */
public final class ThemePalette {
  public static final int BACKGROUND = 0xff080808, BUTTON = 0xff571923, ACCENT = 0xffc9ad72;
  public final int background, button, accent;

  public ThemePalette(int background, int button, int accent) {
    this.background = background | 0xff000000;
    this.button = button | 0xff000000;
    this.accent = accent | 0xff000000;
  }

  public boolean original() {
    return background == BACKGROUND && button == BUTTON && accent == ACCENT;
  }

  // Mezcla dos colores con un peso limitado entre cero y uno.
  public static int mix(int first, int second, float weight) {
    float blend = Math.max(0, Math.min(1, weight));
    int result = 0xff000000;
    // ARGB usa ocho bits por canal. 16, 8 y 0 son las posiciones de rojo, verde y azul.
    for (int shift : new int[] {16, 8, 0}) {
      int firstChannel = (first >> shift) & 255;
      int secondChannel = (second >> shift) & 255;
      result |= Math.round(firstChannel * (1 - blend) + secondChannel * blend) << shift;
    }
    return result;
  }

  /** Elige texto claro u oscuro según la luminosidad del fondo. */
  public static int foreground(int color) {
    double luminance = 0;
    int[] channelShifts = {16, 8, 0};
    double[] weights = {.2126, .7152, .0722};
    for (int index = 0; index < channelShifts.length; index++) {
      double channel = ((color >> channelShifts[index]) & 255) / 255.0;
      // Convierte el canal a luz lineal; el verde tiene mayor peso en la visión humana.
      double linear = channel <= .04045 ? channel / 12.92 : Math.pow((channel + .055) / 1.055, 2.4);
      luminance += weights[index] * linear;
    }
    return luminance > .179 ? 0xff101010 : 0xfffafafa;
  }

  public int card() {
    return original() ? 0xff1a1414 : mix(background, foreground(background), .075f);
  }

  public int muted() {
    return original() ? 0xffbfb3a4 : mix(background, foreground(background), .72f);
  }

  // Lee un código de seis dígitos como #C9AD72 y rechaza valores inválidos.
  public static int parse(String value) {
    String hex = value.trim();
    if (hex.startsWith("#")) hex = hex.substring(1);
    if (!hex.matches("[0-9a-fA-F]{6}"))
      throw new IllegalArgumentException("Usa seis caracteres, por ejemplo #C9AD72.");
    return (int) Long.parseLong(hex, 16) | 0xff000000;
  }

  // Convierte un color a su código hexadecimal.
  public static String hex(int color) {
    return String.format(java.util.Locale.ROOT, "#%06X", color & 0xffffff);
  }
}
