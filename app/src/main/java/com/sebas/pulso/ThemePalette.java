package com.sebas.pulso;

/** Three editable color roles; derived shades preserve the original default palette. */
public final class ThemePalette {
 public static final int BACKGROUND=0xff080808, BUTTON=0xff571923, ACCENT=0xffc9ad72;
 public final int background,button,accent;
 public ThemePalette(int background,int button,int accent){this.background=background|0xff000000;this.button=button|0xff000000;this.accent=accent|0xff000000;}
 public boolean original(){return background==BACKGROUND&&button==BUTTON&&accent==ACCENT;}
 public static int mix(int a,int b,float weight){float w=Math.max(0,Math.min(1,weight));int c=0xff000000;for(int shift:new int[]{16,8,0})c|=Math.round(((a>>shift)&255)*(1-w)+((b>>shift)&255)*w)<<shift;return c;}
 public static int foreground(int color){double l=0;int[] channels={16,8,0};double[] weights={.2126,.7152,.0722};for(int i=0;i<3;i++){double v=((color>>channels[i])&255)/255.0;l+=weights[i]*(v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4));}return l>.179?0xff101010:0xfffafafa;}
 public int card(){return original()?0xff1a1414:mix(background,foreground(background),.075f);}
 public int muted(){return original()?0xffbfb3a4:mix(background,foreground(background),.72f);}
 public static int parse(String value){String hex=value.trim();if(hex.startsWith("#"))hex=hex.substring(1);if(!hex.matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Usa seis caracteres, por ejemplo #C9AD72.");return (int)Long.parseLong(hex,16)|0xff000000;}
 public static String hex(int color){return String.format(java.util.Locale.ROOT,"#%06X",color&0xffffff);}
}
