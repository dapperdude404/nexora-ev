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
    private var page = "Connect"
    private var checking = false
    private var lastVersion: String? = null
    private var lastVersionAt = 0L
    private var connectionGeneration = 0
    private val ink = Color.rgb(231,240,249)
    private val muted = Color.rgb(156,179,195)
    private val accent = Color.rgb(45,212,191)
    private val panelColor = Color.rgb(20,26,33)
    private fun dp(n: Int) = (n * resources.displayMetrics.density + 0.5f).toInt()
    private fun fmt(n: Double) = String.format(Locale.US, "%.1f", n)
    private fun round(c: Int) = GradientDrawable().apply { setColor(c); cornerRadius = dp(14).toFloat() }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Color.rgb(10,18,27)
        window.navigationBarColor = Color.rgb(10,18,27)
        show(state?.getString("page") ?: "Connect")
    }
    override fun onSaveInstanceState(out: Bundle) { out.putString("page", page); super.onSaveInstanceState(out) }

    private fun text(parent: LinearLayout, s: String, size: Float = 16f, color: Int = ink): TextView =
        TextView(this).apply {
            this.text = s; textSize = size; typeface = Typeface.create("sans-serif",Typeface.NORMAL); setTextColor(color); setPadding(0,dp(5),0,dp(5))
            parent.addView(this)
        }
    private fun button(parent: LinearLayout, title: String, action: () -> Unit) {
        parent.addView(Button(this).apply {
            text = title; isAllCaps = false; typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL); setTextColor(ink); background=round(Color.rgb(35,48,60)); minHeight=dp(48); setPadding(dp(12),dp(8),dp(12),dp(8)); setOnClickListener { action() }; layoutParams=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(7) }
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
        connectionGeneration++
        checking = false
        page = next
        mediaState = null
        root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(10,18,27)); setPadding(dp(12),dp(8),dp(12),dp(8)) }
        text(root,"NEXORA  /  EV",24f,accent).setTypeface(null,Typeface.BOLD)
        text(root,"ADAPTIVE LITE PRO  •  NO LIVE TELEMETRY",12f,muted)
        body = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(body) }
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        when(page) {
            "Energy" -> energy()
            "Learn" -> learn()
            "History" -> history()
            "Connect" -> connect()
            "Media" -> media()
            "System" -> system()
            "Modules" -> modules()
            "Tools" -> tools()
            "Help" -> help()
            else -> home()
        }
        val nav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,dp(5),0,0) }
        val selected=when(page) { "Connect" -> "Setup"; "Modules","Media" -> "Modules"; "Tools","System","Energy","Learn","History" -> "Tools"; "Help" -> "Help"; else -> "My EV" }
        listOf("My EV" to "Home","Setup" to "Connect","Modules" to "Modules","Tools" to "Tools","Help" to "Help").forEach { (label,target) ->
            nav.addView(TextView(this).apply {
                text=label; textSize=12f; typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL); gravity=android.view.Gravity.CENTER
                setTextColor(if(label==selected) accent else muted); background=round(if(label==selected) panelColor else Color.TRANSPARENT)
                minHeight=dp(54); isClickable=true; isFocusable=true; setOnClickListener { show(target) }
            },LinearLayout.LayoutParams(0,dp(54),1f))
        }
        root.addView(nav)
        if(Build.VERSION.SDK_INT >= 35) { root.setOnApplyWindowInsetsListener { v, ins -> val bars=ins.getInsets(android.view.WindowInsets.Type.systemBars()); v.setPadding(dp(12)+bars.left,dp(8)+bars.top,dp(12)+bars.right,dp(8)+bars.bottom); ins } }
        setContentView(root)
        root.requestApplyInsets()
    }
    private fun tiles(parent: LinearLayout, values: List<Pair<String,String>>, columns: Int=2) {
        values.chunked(columns).forEach { items ->
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            items.forEach { (label,value) ->
                val tile=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=round(Color.rgb(32,41,50)); setPadding(dp(10),dp(10),dp(8),dp(10)) }
                text(tile,value,22f,ink).setTypeface(null,Typeface.BOLD)
                text(tile,label,12f,muted)
                row.addView(tile,LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(dp(3),dp(4),dp(3),dp(4)) })
            }
            repeat(columns-items.size) { row.addView(View(this),LinearLayout.LayoutParams(0,1,1f)) }
            parent.addView(row)
        }
    }
    private fun home() {
        val p=panel("My EV")
        text(p,"S1 Pro Gen 3",25f)
        text(p,"Selected profile • MoveOS 5.6.2 (user reported)",13f,muted)
        text(p,if(lastVersion==null) "●  NOT CONNECTED" else "●  VERSION RESPONSE RECEIVED",16f,if(lastVersion==null) Color.rgb(251,146,120) else accent)
        lastVersion?.let { text(p,"Reported software: "+it+" • "+java.text.DateFormat.getTimeInstance().format(java.util.Date(lastVersionAt)),14f,muted) }
        button(p,"Open Smart Connect") { show("Connect") }
        val q=panel("Battery & range")
        text(q,"— %",42f)
        text(q,"State of charge • awaiting vehicle data",13f,muted)
        tiles(q,listOf("Normal range" to "— km","Sport range" to "— km","Hyper range" to "— km"),3)
        tiles(q,listOf("Battery health" to "— %","Motor temp" to "— °C","Odometer" to "— km","Charge cycles" to "—","Speed" to "— km/h","Ride mode" to "—"))
        val d=panel("System health diagnostics")
        text(d,"No live BMS, motor, MCU, VCU or charger data available. Missing readings are never replaced with sample values.",14f,muted)
        button(d,"Local device diagnostics") { show("System") }
        val t=panel("Offline intelligence")
        button(t,"Trip planner") { show("Energy") }
        button(t,"TrueRange learning") { show("Learn") }
        button(t,"Saved trip journal") { show("History") }
    }
    private fun launchReference() {
        try {
            val launch=packageManager.getLaunchIntentForPackage("com.asjohn720.s1mods")
            if(launch!=null) startActivity(launch) else Toast.makeText(this,"S1Mods is not available on this device",Toast.LENGTH_LONG).show()
        } catch(e:Exception) { Toast.makeText(this,"Unable to open S1Mods",Toast.LENGTH_LONG).show() }
    }
    private fun modules() {
        val p=panel("Module center")
        text(p,"Local functions",15f,muted)
        button(p,"Media Hub • local music") { show("Media") }
        button(p,"TrueRange • trip learning") { show("Learn") }
        button(p,"Battery IQ • offline planner") { show("Energy") }
        val q=panel("Scooter infotainment")
        listOf("Smart Ball","Android Auto","OpenPlay","Remote APK manager").forEach { name ->
            text(q,name,18f)
            text(q,"Not integrated into NEXORA",13f,muted)
        }
        button(q,"Open installed S1Mods companion") { launchReference() }
        text(q,"Opens a separate app. NEXORA does not install or remove scooter modules.",13f,muted)
    }
    private fun tools() {
        val p=panel("Intelligence & diagnostics")
        button(p,"Battery IQ • trip planner") { show("Energy") }
        button(p,"TrueRange • learning") { show("Learn") }
        button(p,"Trip journal & CSV sharing") { show("History") }
        button(p,"RAM, storage & thermal diagnostics") { show("System") }
        val q=panel("This device • settings")
        listOf("Wi-Fi" to android.provider.Settings.ACTION_WIFI_SETTINGS,"Bluetooth" to android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,"Apps" to android.provider.Settings.ACTION_APPLICATION_SETTINGS,"Display" to android.provider.Settings.ACTION_DISPLAY_SETTINGS,"Sound" to android.provider.Settings.ACTION_SOUND_SETTINGS,"Date and time" to android.provider.Settings.ACTION_DATE_SETTINGS).forEach { (label,action) ->
            button(q,label) { try { startActivity(Intent(action)) } catch(e:Exception) { Toast.makeText(this,"Setting unavailable",Toast.LENGTH_SHORT).show() } }
        }
        text(q,"Settings open on the Android device running NEXORA, not remotely.",13f,muted)
    }
    private fun help() {
        val p=panel("Rex • Help center")
        val questions=listOf("How do I connect?" to "Park, keep the scooter awake and join the phone hotspot from its normal Wi-Fi settings. Smart Connect can request the software version from a known authorized endpoint. A response is not an authenticated vehicle session.","Why are readings blank?" to "NEXORA has no verified live telemetry adapter. Use the original companion for live vehicle status.","Can I install scooter mods?" to "Remote installation is not supported in this build. Use manufacturer-supported installation and permission prompts.","How does TrueRange work?" to "Enter completed trip distance and start/end charge. The model learns energy-weighted consumption using an assumed usable battery capacity.","Does the app read my scooter RAM?" to "Only the RAM of the Android device where NEXORA runs is measured.")
        questions.forEach { (question,answer) -> button(p,question) { AlertDialog.Builder(this).setTitle(question).setMessage(answer).setPositiveButton("Close",null).show() } }
        val q=panel("Credits")
        text(q,"NEXORA EV • independent project")
        text(q,"Interface inspiration: S1Mods by Anand (@asjohn720). Not affiliated with Ola Electric. No S1Mods code or bundled APKs included.",14f,muted)
        button(q,"Open S1Mods companion") { launchReference() }
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
    private fun measuredTrips(): JSONArray = try { JSONArray(prefs.getString("measuredTrips","[]") ?: "[]") } catch(e:Exception) { JSONArray() }
    private fun trained(): TripLearning {
        val m=TripLearning()
        val a=measuredTrips()
        for(i in 0 until a.length()) {
            try { val x=a.getJSONObject(i); m.add(x.getDouble("km"),x.getDouble("wh")) } catch(e:Exception) {}
        }
        return m
    }
    private fun learn() {
        val p=panel("TrueRange • Personal efficiency")
        text(p,"Record completed trips without charging between the start and end readings. Energy is estimated from manually entered battery percentage and assumed usable capacity of 3.6 kWh.",14f,muted)
        val m=trained()
        if(m.count()>0) {
            text(p,fmt(m.consumption())+" Wh/km",30f,accent)
            text(p,m.count().toString()+" completed trips • "+fmt(m.distance())+" km of observations")
            val soc=prefs.getString("soc","75")?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..100.0 } ?: 75.0
            text(p,"Range at last entered charge, above 10% reserve: "+fmt(m.range(3600.0,soc,10.0))+" km")
            button(p,"Use learned consumption in planner") { prefs.edit().putString("eff",m.consumption().toString()).apply(); show("Energy") }
        } else text(p,"No observations yet. Add your first completed trip.")
        val km=field(p,"Actual distance (km)","")
        val start=field(p,"Starting battery (%)","")
        val end=field(p,"Ending battery (%)","")
        val status=text(p,"Park before entering readings.",14f,muted)
        button(p,"Save completed trip") {
            try {
                val a=km.text.toString().toDouble()
                val b=start.text.toString().toDouble()
                val c=end.text.toString().toDouble()
                val wh=TripLearning.energy(3600.0,b,c)
                val check=TripLearning(); check.add(a,wh)
                val old=measuredTrips()
                val next=JSONArray().put(JSONObject().put("km",a).put("wh",wh).put("start",b).put("end",c).put("time",System.currentTimeMillis()))
                for(i in 0 until minOf(199,old.length())) next.put(old.getJSONObject(i))
                prefs.edit().putString("measuredTrips",next.toString()).apply()
                show("Learn")
            } catch(e:Exception) { status.text="Check distance (1–1000 km), falling charge and plausible consumption (5–250 Wh/km)." }
        }
        button(p,"Reset learning history") { AlertDialog.Builder(this).setMessage("Delete completed trip observations?").setNegativeButton("Cancel",null).setPositiveButton("Delete") { _,_ -> prefs.edit().remove("measuredTrips").apply(); show("Learn") }.show() }
        text(p,"Statistical estimate, not live BMS data or a validated vehicle range prediction.",13f,muted)
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
        val p=panel("Smart Connect")
        text(p,if(lastVersion==null) "NOT CONNECTED" else "LAST VERSION QUERY SUCCEEDED",24f,accent)
        text(p,"1. Park your scooter and turn on the phone hotspot.\n2. Join the hotspot from the scooter Wi-Fi settings.\n3. Check connected devices in your hotspot settings.\n4. Enter the scooter address for an authorized read-only version query.")
        val status=text(p,"No vehicle session established.",16f,muted)
        button(p,"Open Wi-Fi settings") { try { startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) } catch(e:Exception) { status.text="Wi-Fi settings unavailable" } }
        button(p,"Inspect local interfaces") { status.text=NetworkInfo.interfaces() }
        text(p,"Scooter IPv6 address with phone-side interface scope",14f,muted)
        val address=EditText(this).apply { setSingleLine(true); hint="fe80::…%interface"; setTextColor(ink); inputType=1; setText(prefs.getString("endpoint","")); p.addView(this) }
        val port=field(p,"Diagnostics service port","50005")
        button(p,"Read scooter software version") {
            if(checking) return@button
            val raw=address.text.toString().trim()
            val n=port.text.toString().toIntOrNull()
            val parts=raw.split('%')
            if(n==null || n !in 1..65535 || parts.size!=2 || !parts[0].contains(':') || !parts[0].matches(Regex("[a-fA-F0-9:]+")) || !parts[1].matches(Regex("[a-zA-Z0-9_.-]+"))) {
                status.text="Enter a scoped link-local IPv6 address and an authorized service port."; return@button
            }
            lastVersion=null
            checking=true
            val generation=++connectionGeneration
            status.text="Requesting software version…"
            Thread {
                var version: String?=null
                val result=try {
                    val unscoped=java.net.InetAddress.getByName(parts[0]) as? java.net.Inet6Address ?: throw IllegalArgumentException()
                    require(unscoped.isLinkLocalAddress)
                    val iface=java.net.NetworkInterface.getByName(parts[1]) ?: parts[1].toIntOrNull()?.let { java.net.NetworkInterface.getByIndex(it) } ?: throw IllegalArgumentException()
                    require(iface.isUp)
                    val peer=java.net.Inet6Address.getByAddress(null,unscoped.address,iface)
                    version=VehicleVersion.read(peer,n)
                    "Version response: "+version+" • Read-only query completed. Identity and live battery telemetry not verified."
                } catch(e:java.net.SocketTimeoutException) { "Connection timed out. Check hotspot, scope and service availability." }
                catch(e:java.net.ConnectException) { "Connection refused or unreachable. No vehicle session." }
                catch(e:Exception) { "Cannot reach endpoint. Check address and phone-side interface." }
                runOnUiThread {
                    if(!isFinishing && !isDestroyed && page=="Connect" && generation==connectionGeneration) { checking=false; status.text=result; lastVersion=version; if(version!=null) { lastVersionAt=System.currentTimeMillis(); prefs.edit().putString("endpoint",raw).apply() } }
                }
            }.start()
        }
        button(p,"Open installed S1Mods companion") { launchReference() }
        button(p,"Continue offline") { show("Home") }
        text(p,"Only GetSwVersion is requested over the local network; no shell commands or authentication changes. Transport is not authenticated. A version response is not live BMS connectivity.",14f,muted)
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
