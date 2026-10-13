package com.sebas.pulso;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.function.IntConsumer;

/**
 * Selector de color con vista previa, barras RGB y entrada hexadecimal.
 *
 * <p>Solo devuelve el color confirmado. MainActivity decide qué parte del tema cambiar y guarda la
 * preferencia. La separación permite leer la sincronización de controles sin mezclarla con el tema.
 */
public final class ColorPickerDialog {
  private static final String[] CHANNEL_NAMES = {"Rojo", "Verde", "Azul"};
  private final Activity host;
  private final GradientDrawable preview = new GradientDrawable();
  private final SeekBar[] channels = new SeekBar[3];
  private final TextView[] labels = new TextView[3];
  private final EditText hexadecimal;
  private boolean updatingHex;

  private ColorPickerDialog(Activity host) {
    this.host = host;
    hexadecimal = new EditText(host);
  }

  public static void show(Activity host, String title, int color, IntConsumer onConfirmed) {
    new ColorPickerDialog(host).open(title, color, onConfirmed);
  }

  private int dp(int size) {
    return (int) (size * host.getResources().getDisplayMetrics().density);
  }

  private void open(String title, int color, IntConsumer onConfirmed) {
    LinearLayout content = new LinearLayout(host);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(dp(20), dp(12), dp(20), dp(12));
    addPreview(content, color);
    addHexadecimalInput(content, color);
    addRgbControls(content, color);
    watchHexadecimalInput();
    ScrollView scroll = new ScrollView(host);
    scroll.addView(content);
    AlertDialog dialog =
        new AlertDialog.Builder(host)
            .setTitle(title)
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create();
    // Sustituye el listener al mostrarlo para que un valor inválido no cierre el diálogo.
    dialog.setOnShowListener(
        ignored ->
            dialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> confirm(dialog, onConfirmed)));
    dialog.show();
  }

  private void addPreview(LinearLayout content, int color) {
    View swatch = new View(host);
    preview.setCornerRadius(dp(16));
    preview.setColor(color);
    preview.setStroke(dp(1), Color.GRAY);
    swatch.setBackground(preview);
    content.addView(swatch, new LinearLayout.LayoutParams(-1, dp(54)));
  }

  private void addHexadecimalInput(LinearLayout content, int color) {
    hexadecimal.setSingleLine(true);
    hexadecimal.setHint("#RRGGBB");
    hexadecimal.setText(ThemePalette.hex(color));
    hexadecimal.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
    content.addView(hexadecimal);
  }

  private void addRgbControls(LinearLayout content, int color) {
    for (int index = 0; index < channels.length; index++) {
      labels[index] = new TextView(host);
      content.addView(labels[index]);
      channels[index] = new SeekBar(host);
      channels[index].setMax(255);
      channels[index].setProgress((color >> (16 - 8 * index)) & 255);
      content.addView(channels[index]);
    }
    for (SeekBar channel : channels) {
      channel.setOnSeekBarChangeListener(
          new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
              if (fromUser) updateFromRgb();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {}

            @Override
            public void onStopTrackingTouch(SeekBar bar) {}
          });
    }
    updateFromRgb();
  }

  /** updatingHex evita que actualizar el texto vuelva a modificar los mismos controles. */
  private void updateFromRgb() {
    int color =
        Color.rgb(channels[0].getProgress(), channels[1].getProgress(), channels[2].getProgress());
    preview.setColor(color);
    updateLabels();
    updatingHex = true;
    hexadecimal.setText(ThemePalette.hex(color));
    updatingHex = false;
  }

  private void updateLabels() {
    for (int index = 0; index < channels.length; index++) {
      labels[index].setText(CHANNEL_NAMES[index] + ": " + channels[index].getProgress());
    }
  }

  private void watchHexadecimalInput() {
    hexadecimal.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

          @Override
          public void afterTextChanged(Editable text) {}

          @Override
          public void onTextChanged(CharSequence text, int start, int before, int count) {
            if (updatingHex) return;
            try {
              int color = ThemePalette.parse(text.toString());
              preview.setColor(color);
              for (int index = 0; index < channels.length; index++) {
                channels[index].setProgress((color >> (16 - 8 * index)) & 255);
              }
              updateLabels();
              hexadecimal.setError(null);
            } catch (IllegalArgumentException ignored) {
              // Permite escribir los seis caracteres poco a poco. Guardar hace la validación final.
            }
          }
        });
  }

  private void confirm(AlertDialog dialog, IntConsumer onConfirmed) {
    try {
      int color = ThemePalette.parse(hexadecimal.getText().toString());
      onConfirmed.accept(color);
      dialog.dismiss();
    } catch (IllegalArgumentException invalidColor) {
      hexadecimal.setError(invalidColor.getMessage());
    }
  }
}
