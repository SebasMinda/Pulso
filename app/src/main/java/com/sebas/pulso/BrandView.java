package com.sebas.pulso;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** A centered wordmark that gently alternates with Pulso's musical note. */
public final class BrandView extends FrameLayout {
 private final TextView wordmark;
 private final ImageView note;
 private ValueAnimator animator;

 public BrandView(Context context) {
  super(context);
  setContentDescription("Pulso, tu música");
  setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  wordmark=new TextView(context);
  wordmark.setText("PULSO");
  wordmark.setTextSize(30);
  wordmark.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
  wordmark.setLetterSpacing(.18f);
  wordmark.setGravity(Gravity.CENTER);
  wordmark.setIncludeFontPadding(false);
  wordmark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  addView(wordmark,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
  note=new ImageView(context);
  note.setImageResource(R.drawable.brand_note);
  note.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  int size=Math.round(44*getResources().getDisplayMetrics().density);
  addView(note,new LayoutParams(size,size,Gravity.CENTER));
  showWordmark();
 }

 public void setAccent(int color) {
  wordmark.setTextColor(color);
  note.setColorFilter(color);
 }

 private void showWordmark() {
  wordmark.setAlpha(1);wordmark.setScaleX(1);wordmark.setScaleY(1);
  note.setAlpha(0);note.setScaleX(.85f);note.setScaleY(.85f);
 }

 private void updateAnimation() {
  if(wordmark==null)return;
  if(animator!=null){animator.cancel();animator=null;}
  showWordmark();
  if(!isAttachedToWindow()||getWindowVisibility()!=View.VISIBLE||!isShown()||!ValueAnimator.areAnimatorsEnabled())return;
  animator=ValueAnimator.ofFloat(0,1);
  animator.setDuration(10000);
  animator.setRepeatCount(ValueAnimator.INFINITE);
  animator.setInterpolator(new LinearInterpolator());
  animator.addUpdateListener(value->{
   float phase=(float)value.getAnimatedValue();
   float blend=phase<.4f?0:phase<.48f?(phase-.4f)/.08f:phase<.72f?1:phase<.8f?1-(phase-.72f)/.08f:0;
   // Smooth the two transitions; leave both identities still between them.
   blend=blend*blend*(3-2*blend);
   wordmark.setAlpha(1-blend);wordmark.setScaleX(1-.08f*blend);wordmark.setScaleY(1-.08f*blend);
   note.setAlpha(blend);note.setScaleX(.85f+.15f*blend);note.setScaleY(.85f+.15f*blend);
  });
  animator.start();
 }

 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();updateAnimation();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);updateAnimation();}
 @Override protected void onVisibilityChanged(View changedView,int visibility){super.onVisibilityChanged(changedView,visibility);updateAnimation();}
 @Override protected void onDetachedFromWindow(){if(animator!=null){animator.cancel();animator=null;}showWordmark();super.onDetachedFromWindow();}
}
