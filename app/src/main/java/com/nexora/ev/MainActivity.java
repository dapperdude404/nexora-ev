package com.nexora.ev;
import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.Color;
public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){super.onCreate(b); LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(32,32,32,32);l.setBackgroundColor(Color.rgb(15,25,35));TextView t=new TextView(this);t.setText("NEXORA EV | Lite Pro\n4 kWh • Manual estimate • Parked use only");t.setTextColor(Color.WHITE);t.setTextSize(22);l.addView(t); EditText soc=new EditText(this);soc.setHint("Charge %");soc.setText("75");soc.setInputType(8194);l.addView(soc);EditText eff=new EditText(this);eff.setHint("Wh/km");eff.setText("35");eff.setInputType(8194);l.addView(eff);TextView result=new TextView(this);result.setTextColor(Color.WHITE);result.setTextSize(24);Button btn=new Button(this);btn.setText("Estimate range");l.addView(btn);l.addView(result);btn.setOnClickListener(v->{try{result.setText(String.format(java.util.Locale.US,"%.1f km (illustrative)",RangeEngine.estimate(Double.parseDouble(soc.getText().toString()),Double.parseDouble(eff.getText().toString()),.9)));}catch(Exception e){result.setText("Check inputs");}});setContentView(l);}
}
