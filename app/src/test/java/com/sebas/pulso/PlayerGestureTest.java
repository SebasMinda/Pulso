package com.sebas.pulso;

import static com.sebas.pulso.PlayerGesture.Action.*;
import static org.junit.Assert.*;

import org.junit.Test;

/** Comprueba direcciones, distancia mínima y gestos diagonales en ambos reproductores. */
public class PlayerGestureTest {
  @Test
  public void horizontalDirectionIsTheSameInBothPlayers() {
    for (boolean expanded : new boolean[] {false, true}) {
      assertEquals(NEXT, PlayerGesture.resolve(-80, 12, 48, expanded));
      assertEquals(PREVIOUS, PlayerGesture.resolve(80, -12, 48, expanded));
    }
  }

  @Test
  public void tapsAndSmallDragsDoNotSkip() {
    assertEquals(NONE, PlayerGesture.resolve(0, 0, 48, false));
    assertEquals(NONE, PlayerGesture.resolve(-48, 0, 48, true));
    assertEquals(NONE, PlayerGesture.resolve(30, 20, 48, false));
  }

  @Test
  public void diagonalsDoNotChooseTwoActions() {
    for (boolean expanded : new boolean[] {false, true}) {
      assertEquals(NONE, PlayerGesture.resolve(80, 80, 48, expanded));
      assertEquals(NONE, PlayerGesture.resolve(-90, -60, 48, expanded));
    }
  }

  @Test
  public void verticalSwipesKeepTheExpandAndCollapseDirections() {
    assertEquals(EXPAND, PlayerGesture.resolve(10, -80, 48, false));
    assertEquals(COLLAPSE, PlayerGesture.resolve(-10, 80, 48, true));
    assertEquals(NONE, PlayerGesture.resolve(0, 80, 48, false));
    assertEquals(NONE, PlayerGesture.resolve(0, -80, 48, true));
  }

  @Test
  public void thresholdScalesWithScreenDensity() {
    assertEquals(NONE, PlayerGesture.resolve(-80, 0, 96, true));
    assertEquals(NEXT, PlayerGesture.resolve(-120, 0, 96, true));
  }
}
