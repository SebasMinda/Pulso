package com.sebas.pulso;

/** Convierte un deslizamiento claro en una acción; ignora toques pequeños y diagonales. */
public final class PlayerGesture {
  public enum Action {
    NONE,
    NEXT,
    PREVIOUS,
    EXPAND,
    COLLAPSE
  }

  private PlayerGesture() {}

  // Usa la dirección y una distancia mínima para elegir una sola acción.
  public static Action resolve(float dx, float dy, float threshold, boolean expanded) {
    float x = Math.abs(dx), y = Math.abs(dy);
    if (x > threshold && x > y * 1.5f) return dx < 0 ? Action.NEXT : Action.PREVIOUS;
    if (y > threshold && y > x * 1.5f) {
      if (expanded && dy > 0) return Action.COLLAPSE;
      if (!expanded && dy < 0) return Action.EXPAND;
    }
    return Action.NONE;
  }
}
