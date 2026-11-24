import com.nexora.ev.TripLearning;
public class TripLearningTest {
 public static void main(String[] args) {
  TripLearning t=new TripLearning();
  t.add(10,300); t.add(30,1200);
  if(t.count()!=2 || Math.abs(t.consumption()-37.5)>1e-9 || Math.abs(t.range(3600,80,10)-67.2)>1e-9) throw new AssertionError();
  if(Math.abs(TripLearning.energy(3600,80,70)-360)>1e-9) throw new AssertionError();
  try { t.add(0,300); throw new AssertionError(); } catch(IllegalArgumentException expected) {}
  try { TripLearning.energy(3600,40,60); throw new AssertionError(); } catch(IllegalArgumentException expected) {}
  try { new TripLearning().consumption(); throw new AssertionError(); } catch(IllegalStateException expected) {}
  System.out.println("TripLearning tests passed");
 }
}
