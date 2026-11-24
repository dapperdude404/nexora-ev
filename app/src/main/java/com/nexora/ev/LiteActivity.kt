package com.nexora.ev

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.PowerManager
import android.os.StatFs
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class LiteActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("local", MODE_PRIVATE) }
    private lateinit var body: LinearLayout
    private lateinit var root: LinearLayout
    private var player: MediaPlayer? = null
    private var mediaReady = false
    private var mediaState: TextView? = null
    private var mediaTitle = "No audio selected"
    private var page = "Home"
    private val ink = Color.rgb(231,240,249)
    private val muted = Color.rgb(156,179,195)
    private val accent = Color.rgb(45,212,191)
    private val panelColor = Color.rgb(24,39,53)
    private fun dp(n: Int) = (n * resources.displayMetrics.density + 0.5f).toInt()
    private fun fmt(n: Double) = String.format(Locale.US, "%.1f", n)
    private fun round(c: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(14).toFloat() }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Color.rgb(10,18,27)
        window.navigationBarColor = Color.rgb(10,18,27)
        show(state?.getString("page") ?: "Home")
    }
    override fun onSaveInstanceState(out: Bundle) { out.putString("page", page); super.onSaveInstanceState(out) }

    private fun text(parent: LinearLayout, s: String, size: Float = 16f, color: Int = ink): TextView =
        TextView(this).apply {
            this.text = s; textSize = size; setTextColor(color); setPadding(0,dp(5),0,dp(5))
            parent.addView(this)
        }
    private fun button(parent: LinearLayout, title: String, action: () -> Unit) {
        parent.addView(Button(this).apply {
            text = title; isAllCaps = false; setOnClickListener { action() }
        })
    }
    private fun panel(title: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = round(panelColor)
        setPadding(dp(15),dp(10),dp(15),dp(10))
        body.addView(this, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(12) })
        text(this,title,19f,accent).setTypeface(null,Typeface.BOLD)
    }
    private fun field(parent: LinearLayout, label: String, value: String): EditText {
        text(parent,label,14f,muted)
        return EditText(this).apply {
            setText(value); setTextColor(ink); setSingleLine(true); inputType=8194
            parent.addView(this,LinearLayout.LayoutParams(-1,-2))
        }
    }
    private fun show(next: String) {
        page = next
        mediaState = null
        root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(10,18,27)); setPadding(dp(12),dp(8),dp(12),dp(8)) }
        text(root,"NEXORA  /  EV",24f,accent).setTypeface(null,Typeface.BOLD)
        text(root,"ADAPTIVE LITE PRO  •  OFFLINE / MANUAL DATA",12f,muted)
        val nav = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false }
        val tabs = LinearLayout(this)
        listOf("Home","Energy","History","Connect","Media","System").forEach { name ->
            button(tabs,if(name==page) "• "+name else name) { show(name) }
        }
        nav.addView(tabs); root.addView(nav)
        body = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(body) }
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        when(page) {
            "Energy" -> energy()
            "History" -> history()
            "Connect" -> connect()
            "Media" -> media()
            "System" -> system()
            else -> home()
        }
        setContentView(root)
    }
    private fun home() {
        val p=panel("Vehicle profile")
        text(p,"Ola S1 Pro Gen 3 • 4 kWh",24f)
        text(p,"Parked use only. No vehicle telemetry connected.",14f,muted)
        val charge=prefs.getString("soc","75")?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..100.0 } ?: 75.0
        val wh=prefs.getString("eff","35")?.toDoubleOrNull()?.takeIf { it.isFinite() && it>0 } ?: 35.0
        text(p,fmt(charge)+"%  •  last manual charge",30f,accent)
        p.addView(ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply { max=100; progress=charge.toInt() })
        text(p,"Estimated range: "+fmt(4000*charge/100*0.9/wh)+" km",19f)
        text(p,"Assumes 90% usable fraction; not an OEM estimate.",13f,muted)
        val q=panel("Quick actions")
        button(q,"Plan a trip") { show("Energy") }
        button(q,"Inspect local network") { show("Connect") }
        button(q,"Open local music") { show("Media") }
        button(q,"View trip journal") { show("History") }
        val s=panel("Integration status")
        text(s,"Live BMS, CAN, projection and vehicle-side floating controls are not connected.")
        text(s,"This app does not alter MoveOS, vehicle settings or access permissions.",14f,muted)
    }
    private fun journal(): JSONArray = try { JSONArray(prefs.getString("journal","[]") ?: "[]") } catch(e:Exception) { JSONArray() }
    private fun energy() {
        val p=panel("Battery IQ • manual planner")
        val soc=field(p,"Battery charge (%)",prefs.getString("soc","75") ?: "75")
        val eff=field(p,"Consumption (Wh/km)",prefs.getString("eff","35") ?: "35")
        val km=field(p,"Trip distance (km)","20")
        val reserve=field(p,"Desired reserve (% of usable capacity)","10")
        val output=text(p,"Enter actual values to estimate trip energy.")
        button(p,"Calculate and save") {
            val a=soc.text.toString().toDoubleOrNull()
            val b=eff.text.toString().toDoubleOrNull()
            val c=km.text.toString().toDoubleOrNull()
            val d=reserve.text.toString().toDoubleOrNull()
            if(a==null || b==null || c==null || d==null || !a.isFinite() || !b.isFinite() || !c.isFinite() || !d.isFinite() || a !in 0.0..100.0 || b<=0 || b>1000 || c !in 0.0..10000.0 || d !in 0.0..100.0) {
                output.text="Invalid inputs"; return@button
            }
            val available=3600*a/100
            val needed=c*b
            val range=((available-3600*d/100).coerceAtLeast(0.0))/b
            val remaining=(available-needed)/3600*100
            output.text="Range above reserve: "+fmt(range)+" km\nTrip energy: "+fmt(needed/1000)+" kWh\nEstimated arrival charge: "+fmt(remaining.coerceAtLeast(0.0))+"%\n"+if(c<=range) "Within selected reserve estimate" else "Reserve not met — plan charging"
            val old=journal()
            val arr=JSONArray().put(JSONObject().put("time",System.currentTimeMillis()).put("charge",a).put("wh",b).put("distance",c).put("range",range).put("arrival",remaining))
            for(i in 0 until minOf(old.length(),199)) { arr.put(old.getJSONObject(i)) }
            prefs.edit().putString("soc",a.toString()).putString("eff",b.toString()).putString("journal",arr.toString()).apply()
        }
        text(p,"Planning estimate only: terrain, temperature and battery health can change real range.",13f,muted)
    }
    private fun history() {
        val p=panel("Local trip journal")
        val arr=journal()
        text(p,arr.length().toString()+" saved estimates (max 200)")
        if(arr.length()>0) {
            var total=0.0
            for(i in 0 until arr.length()) total+=arr.getJSONObject(i).optDouble("wh",0.0)
            text(p,"Mean entered consumption: "+fmt(total/arr.length())+" Wh/km",18f,accent)
        }
        button(p,"Share CSV report") {
            val csv=StringBuilder("timestamp_ms,charge_pct,wh_per_km,trip_km,range_km,arrival_pct\n")
            for(i in 0 until arr.length()) {
                val x=arr.getJSONObject(i)
                csv.append(listOf(x.optLong("time"),x.optDouble("charge"),x.optDouble("wh"),x.optDouble("distance"),x.optDouble("range"),x.optDouble("arrival")).joinToString(",")).append('\n')
            }
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,csv.toString()) },"Share estimates"))
        }
        button(p,"Clear journal") { AlertDialog.Builder(this).setMessage("Delete saved trip estimates?").setNegativeButton("Cancel",null).setPositiveButton("Delete") { _,_ -> prefs.edit().remove("journal").apply(); show("History") }.show() }
        for(i in 0 until minOf(arr.length(),20)) {
            val x=arr.getJSONObject(i)
            val q=panel(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(x.optLong("time"))))
            text(q,fmt(x.optDouble("distance"))+" km trip • "+fmt(x.optDouble("charge"))+"% starting charge")
            text(q,fmt(x.optDouble("wh"))+" Wh/km • "+fmt(x.optDouble("range"))+" km range above reserve",14f,muted)
        }
        val legacy=prefs.getString("history","").orEmpty()
        if(legacy.isNotBlank()) text(panel("Earlier estimates"),legacy,14f)
    }
    private fun connect() {
        val p=panel("Smart Connect • local inspection")
        text(p,"Connect the parked scooter to your phone hotspot using its normal Wi-Fi settings.")
        val status=text(p,"No scooter session established",18f,accent)
        button(p,"Refresh IPv6 interfaces") { status.text=NetworkInfo.interfaces() }
        button(p,"Open system Wi-Fi settings") { try { startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) } catch(e:Exception) { status.text="Wi-Fi settings unavailable" } }
        text(p,"Manual link-local address (with local interface scope)",14f,muted)
        val address=EditText(this).apply { setSingleLine(true); hint="fe80::…%interface"; setTextColor(ink); inputType=1; p.addView(this) }
        button(p,"Validate address format") {
            val parts=address.text.toString().trim().split('%')
            val valid=try { parts.size==2 && parts[0].contains(':') && parts[0].matches(Regex("[0-9a-fA-F:]+")) && parts[1].matches(Regex("[a-zA-Z0-9_.-]+")) && (java.net.InetAddress.getByName(parts[0]) as? java.net.Inet6Address)?.isLinkLocalAddress==true } catch(e:Exception) { false }
            status.text=if(valid) "Valid IPv6 format. No remote session established." else "Invalid scoped link-local IPv6 address."
        }
        text(p,"No remote commands, discovery probes or privileged authorization are performed.",14f,muted)
    }
    private fun media() {
        val p=panel("Media Hub • local audio")
        mediaState=text(p,mediaTitle,18f,accent)
        button(p,"Choose audio file") {
            try { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="audio/*"; addCategory(Intent.CATEGORY_OPENABLE) },12) }
            catch(e:Exception) { mediaState?.text="No compatible file picker" }
        }
        button(p,"Play / pause") {
            if(mediaReady) try { player?.let { if(it.isPlaying) { it.pause(); mediaState?.text="Paused" } else { it.start(); mediaState?.text="Playing local audio" } } } catch(e:Exception) { mediaState?.text="Playback unavailable" }
        }
        button(p,"Stop and release") { releasePlayer(); mediaState?.text="Stopped" }
        text(p,"Audio plays on this device only while the app is in the foreground. No scooter projection.",14f,muted)
    }
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=12 || resultCode!=RESULT_OK) return
        val uri=data?.data ?: return
        releasePlayer()
        mediaTitle="Preparing audio"
        mediaState?.text=mediaTitle
        try {
            val m=MediaPlayer()
            player=m
            m.setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
            m.setDataSource(this,uri)
            m.setOnPreparedListener { if(player===it) { mediaReady=true; mediaTitle="Audio ready — tap Play"; mediaState?.text=mediaTitle } }
            m.setOnErrorListener { mp,_,_ -> if(player===mp) { releasePlayer(); mediaTitle="Unsupported audio"; mediaState?.text=mediaTitle }; true }
            m.setOnCompletionListener { mediaState?.text="Playback finished" }
            m.prepareAsync()
        } catch(e:Exception) { releasePlayer(); mediaTitle="Unable to open audio"; mediaState?.text=mediaTitle }
    }
    private fun releasePlayer() { player?.release(); player=null; mediaReady=false; mediaTitle="No audio selected" }
    override fun onStop() { super.onStop(); releasePlayer(); mediaState?.text=mediaTitle }
    override fun onDestroy() { releasePlayer(); super.onDestroy() }
    private fun system() {
        val p=panel("Memory IQ • this device")
        val status=text(p,"Tap sample to read resource information.")
        button(p,"Sample device and app") {
            val info=Debug.MemoryInfo(); Debug.getMemoryInfo(info)
            val am=getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            val mem=android.app.ActivityManager.MemoryInfo(); am.getMemoryInfo(mem)
            val stat=StatFs(filesDir.absolutePath)
            val thermal=if(Build.VERSION.SDK_INT>=29) (getSystemService(POWER_SERVICE) as PowerManager).currentThermalStatus.toString() else "unavailable"
            status.text="Android "+Build.VERSION.RELEASE+" • "+Build.SUPPORTED_ABIS.joinToString()+"\nApp PSS: "+info.totalPss/1024+" MiB\nAvailable device RAM: "+mem.availMem/1048576+" MiB\nFree app-volume storage: "+stat.availableBytes/1048576+" MiB\nAndroid thermal status: "+thermal
        }
        text(p,"Device readings are local, not scooter readings when running on a phone.",14f,muted)
        val q=panel("About & privacy")
        text(q,"50 MB APK budget • No background polling • Local journal • No vehicle modifications")
        text(q,"Independent companion. S1Mods APK was examined as a feature reference; no upstream binaries or privileged code are bundled.",14f,muted)
    }
}
