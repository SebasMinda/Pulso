package com.sebas.pulso;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;

/** Barra de avance ondulada; conserva el arrastre y la accesibilidad de SeekBar. */
public final class WaveSeekBar extends SeekBar {
 private final Paint wavePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path wavePath=new Path();
 private int accent=ThemePalette.ACCENT,remaining=0xffbfb3a4;
 private boolean playing;
 private float phase;
 private ValueAnimator animator;

 public WaveSeekBar(Context context){
  super(context);
  setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
  setSplitTrack(false);
  setBackground(null);
  setContentDescription("Posición de la canción");
  wavePaint.setStyle(Paint.Style.STROKE);
  wavePaint.setStrokeCap(Paint.Cap.ROUND);
  wavePaint.setStrokeJoin(Paint.Join.ROUND);
 }
 private float dp(float n){return n*getResources().getDisplayMetrics().density;}
 public void setWaveColors(int active,int inactive){accent=active;remaining=inactive;invalidate();}
 public void setPlaying(boolean value){
  if(playing==value&&!(animator!=null&&!ValueAnimator.areAnimatorsEnabled()))return;
  playing=value;updateAnimation();
 }
 private void stopAnimation(){if(animator!=null){animator.cancel();animator=null;}}
 private void updateAnimation(){
  if(wavePaint==null)return;
  stopAnimation();
  if(!playing||!isAttachedToWindow()||getWindowVisibility()!=View.VISIBLE||!isShown()||!ValueAnimator.areAnimatorsEnabled())return;
  float start=phase;
  animator=ValueAnimator.ofFloat(start,start+(float)(Math.PI*2));
  animator.setDuration(1500);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());
  animator.addUpdateListener(value->{phase=(float)value.getAnimatedValue();invalidate();});animator.start();
 }
 @Override protected void onDraw(Canvas canvas){
  float left=getPaddingLeft(),right=getWidth()-getPaddingRight(),cy=getHeight()/2f;
  float fraction=getMax()>0?Math.max(0,Math.min(1,(float)getProgress()/getMax())):0;
  boolean rtl=getLayoutDirection()==View.LAYOUT_DIRECTION_RTL;
  float start=rtl?right:left,end=rtl?left:right,thumb=start+(end-start)*fraction;
  float direction=rtl?-1:1,gap=dp(11);
  wavePaint.setStrokeWidth(dp(2));wavePaint.setColor(remaining);wavePaint.setAlpha(100);
  float after=thumb+direction*gap;
  if(direction*(end-after)>0)canvas.drawLine(after,cy,end,cy,wavePaint);
  float length=Math.max(0,Math.abs(thumb-start)-gap);
  if(length>0){
   wavePaint.setColor(accent);wavePaint.setAlpha(255);wavePath.reset();wavePath.moveTo(start,cy);
   float amplitude=dp(3),wavelength=dp(22),fade=dp(10),step=dp(1.5f);
   for(float distance=step;distance<length;distance+=step){
    float envelope=Math.min(1,Math.min(distance/fade,(length-distance)/fade));
    float y=cy+(float)Math.sin(distance/wavelength*Math.PI*2-phase)*amplitude*envelope;
    wavePath.lineTo(start+direction*distance,y);
   }
   wavePath.lineTo(start+direction*length,cy);canvas.drawPath(wavePath,wavePaint);
  }
  // Android dibuja el control circular y gestiona el toque y las acciones accesibles.
  super.onDraw(canvas);
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();updateAnimation();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);updateAnimation();}
 @Override protected void onVisibilityChanged(View changedView,int visibility){super.onVisibilityChanged(changedView,visibility);updateAnimation();}
 @Override protected void onDetachedFromWindow(){stopAnimation();super.onDetachedFromWindow();}
}
