package com.nexora.ev;
public final class RangeEngine {
 public static double estimate(double soc,double whPerKm,double usableFraction){
 if(!Double.isFinite(soc)||!Double.isFinite(whPerKm)||!Double.isFinite(usableFraction)||soc<0||soc>100||whPerKm<=0||usableFraction<=0||usableFraction>1) throw new IllegalArgumentException("Invalid inputs");
 return 4000*usableFraction*Math.max(0,soc-10)/100/whPerKm;
 }
}
