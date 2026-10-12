package com.sebas.pulso;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Marca compacta: las letras se reúnen en una esfera y esta se convierte en un disco. */
public final class BrandView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final float[] widths=new float[5];
 private ValueAnimator animator;
 private float phase;
 private int accent=ThemePalette.ACCENT;
 private Shader sphereShader;
 private float shaderRadius;
 private static final String WORD="PULSO";

 public BrandView(Context context){
  super(context);
  setContentDescription("Pulso");
  setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  paint.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
 }
 public void setAccent(int color){accent=color;sphereShader=null;invalidate();}
 private float dp(float value){return value*getResources().getDisplayMetrics().density;}
 private static float smooth(float value){float x=Math.max(0,Math.min(1,value));return x*x*(3-2*x);}
 private static float ramp(float value,float start,float end){return smooth((value-start)/(end-start));}
 private void stop(){if(animator!=null){animator.cancel();animator=null;}phase=0;invalidate();}
 private void updateAnimation(){
  if(paint==null)return;
  stop();
  if(!isAttachedToWindow()||getWindowVisibility()!=VISIBLE||!isShown()||!ValueAnimator.areAnimatorsEnabled())return;
  animator=ValueAnimator.ofFloat(0,1);animator.setDuration(10000);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());
  animator.addUpdateListener(value->{phase=(float)value.getAnimatedValue();invalidate();});animator.start();
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  float cx=getWidth()/2f,cy=getHeight()/2f;
  float gather=phase<.86f?ramp(phase,.30f,.44f):1-ramp(phase,.86f,.98f);
  float disc=phase<.74f?ramp(phase,.48f,.60f):1-ramp(phase,.74f,.84f);
  float radius=Math.min(dp(16),getHeight()*.43f);
  float sphereAlpha=ramp(gather,.35f,.90f);
  if(sphereAlpha>0){
   canvas.save();canvas.translate(cx,cy);canvas.scale(.5f+.5f*gather,.5f+.5f*gather);
   paint.setStyle(Paint.Style.FILL);paint.setShader(null);
   paint.setColor(ThemePalette.mix(accent,Color.BLACK,.88f));paint.setAlpha(Math.round(255*sphereAlpha));canvas.drawCircle(0,0,radius,paint);
   if(disc<1){
    if(sphereShader==null||shaderRadius!=radius){shaderRadius=radius;sphereShader=new RadialGradient(-radius*.35f,-radius*.4f,radius*1.5f,new int[]{ThemePalette.mix(accent,Color.WHITE,.7f),accent,ThemePalette.mix(accent,Color.BLACK,.55f)},new float[]{0,.45f,1},Shader.TileMode.CLAMP);}
    paint.setShader(sphereShader);paint.setAlpha(Math.round(255*sphereAlpha*(1-disc)));canvas.drawCircle(0,0,radius,paint);paint.setShader(null);
   }
   if(disc>0){
    canvas.saveLayerAlpha(-radius-dp(2),-radius-dp(2),radius+dp(2),radius+dp(2),Math.round(255*sphereAlpha*disc));
    canvas.rotate((phase-.48f)*240);
    paint.setAlpha(255);paint.setColor(accent);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(.8f));
    canvas.drawCircle(0,0,radius,paint);
    for(float fraction:new float[]{.54f,.69f,.82f})canvas.drawCircle(0,0,radius*fraction,paint);
    paint.setColor(ThemePalette.mix(accent,Color.WHITE,.4f));paint.setStrokeWidth(dp(1.3f));
    RectF rim=new RectF(-radius*.92f,-radius*.92f,radius*.92f,radius*.92f);canvas.drawArc(rim,205,35,false,paint);canvas.drawArc(rim,25,35,false,paint);
    paint.setStyle(Paint.Style.FILL);paint.setColor(accent);canvas.drawCircle(0,0,radius*.32f,paint);
    paint.setColor(ThemePalette.mix(accent,Color.BLACK,.88f));canvas.drawCircle(0,0,radius*.09f,paint);canvas.restore();
   }
   canvas.restore();
  }
  paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setColor(accent);
  paint.setTextSize(Math.min(24*getResources().getDisplayMetrics().scaledDensity,getHeight()*.68f));
  paint.getTextWidths(WORD,widths);float gap=paint.getTextSize()*.14f,total=gap*4;for(float width:widths)total+=width;
  if(total>getWidth()-dp(12)){paint.setTextSize(paint.getTextSize()*Math.max(0,getWidth()-dp(12))/total);paint.getTextWidths(WORD,widths);gap=paint.getTextSize()*.14f;total=gap*4;for(float width:widths)total+=width;}
  float x=cx-total/2,baseline=cy-(paint.ascent()+paint.descent())/2;
  paint.setAlpha(Math.round(255*(1-ramp(gather,.45f,1))));
  for(int i=0;i<WORD.length();i++){
   float home=x+widths[i]/2;double angle=-Math.PI/2+i*Math.PI*2/WORD.length()+gather*Math.PI;
   float targetX=cx+(float)Math.cos(angle)*radius*.50f,targetY=cy+(float)Math.sin(angle)*radius*.50f;
   float centerX=home+(targetX-home)*gather,centerY=cy+(targetY-cy)*gather;
   canvas.save();canvas.translate(centerX,centerY);canvas.rotate(gather*(i-2)*28);canvas.scale(1-.65f*gather,1-.65f*gather);
   canvas.drawText(WORD,i,i+1,-widths[i]/2,baseline-cy,paint);canvas.restore();x+=widths[i]+gap;
  }
  paint.setAlpha(255);
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();updateAnimation();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);updateAnimation();}
 @Override protected void onVisibilityChanged(View changedView,int visibility){super.onVisibilityChanged(changedView,visibility);updateAnimation();}
 @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
}
