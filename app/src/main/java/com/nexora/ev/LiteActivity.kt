package com.nexora.ev
import android.app.Activity
import android.os.Bundle
import android.os.Debug
import android.widget.*
import android.graphics.Color
class LiteActivity : Activity() {
 override fun onCreate(state: Bundle?) {
 super.onCreate(state)
 val prefs=getSharedPreferences("local", MODE_PRIVATE)
 val layout=LinearLayout(this).apply { orientation=1; setPadding(24,24,24,24); setBackgroundColor(Color.rgb(15,25,35)) }
 fun label(s:String)=TextView(this).apply { text=s; setTextColor(Color.WHITE); textSize=20f; layout.addView(this) }
 label("NEXORA EV • Lite Pro")
 label("Gen 3 / 4 kWh • Parked use • Manual data")
 fun field(h:String,v:String)=EditText(this).apply { hint=h; setText(v); inputType=8194; layout.addView(this) }
 val soc=field("Charge %",prefs.getString("soc","75") ?: "75")
 val eff=field("Consumption Wh/km",prefs.getString("eff","35") ?: "35")
 val result=label("Enter measured values")
 val history=label(prefs.getString("history","No saved estimates") ?: "No saved estimates")
 layout.addView(Button(this).apply { text="Estimate and save"; setOnClickListener {
 try { val range=RangeEngine.estimate(soc.text.toString().toDouble(),eff.text.toString().toDouble(),0.9)
 result.text=String.format(java.util.Locale.US,"%.1f km • illustrative",range)
 val entry=java.text.DateFormat.getDateTimeInstance().format(java.util.Date())+" • "+result.text
 val records=(listOf(entry)+(prefs.getString("history","") ?: "").lines().filter { it.isNotBlank() }).take(10).joinToString("\n")
 history.text=records
 prefs.edit().putString("soc",soc.text.toString()).putString("eff",eff.text.toString()).putString("history",records).apply()
 } catch(e:Exception){result.text="Invalid input"} } })
 val network=label("Offline mode • No scooter connection")
 val address=EditText(this).apply { hint="Scoped IPv6 address"; inputType=1; layout.addView(this) }
 layout.addView(Button(this).apply { text="Inspect local network"; setOnClickListener { network.text=NetworkInfo.interfaces() } })
 layout.addView(Button(this).apply { text="Validate address"; setOnClickListener {
 val raw=address.text.toString().trim()
 val parts=raw.split('%')
 val valid=try { parts.size==2 && parts[0].contains(':') && parts[0].matches(Regex("[0-9a-fA-F:]+")) && parts[1].matches(Regex("[a-zA-Z0-9_.-]+")) && (java.net.InetAddress.getByName(parts[0]) as? java.net.Inet6Address)?.isLinkLocalAddress==true } catch(e:Exception){false}
 network.text=if(valid) "Valid link-local format • Connection not established" else "Enter link-local IPv6 with interface scope"
 } })
 layout.addView(Button(this).apply { text="Continue offline"; setOnClickListener { address.text.clear(); network.text="Offline mode • Manual estimates available" } })
 val mem=label("Memory: not sampled")
 layout.addView(Button(this).apply { text="Sample app memory"; setOnClickListener { val info=Debug.MemoryInfo(); Debug.getMemoryInfo(info); mem.text="App PSS: ${info.totalPss/1024} MiB" } })
 label("Live telemetry unavailable • MoveOS compatibility unverified")
 setContentView(ScrollView(this).apply { addView(layout) })
 }
}
