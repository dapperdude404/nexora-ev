import com.nexora.ev.RangeEngine;
public class RangeTest {public static void main(String[] a){if(Math.abs(RangeEngine.estimate(75,35,.9)-66.857142857)>0.00001)throw new AssertionError();if(RangeEngine.estimate(5,35,.9)!=0)throw new AssertionError();try{RangeEngine.estimate(110,35,.9);throw new AssertionError();}catch(IllegalArgumentException expected){}System.out.println("3 checks passed");}}
