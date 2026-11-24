package com.nexora.ev
import java.net.NetworkInterface
import java.net.Inet6Address
object NetworkInfo {
 fun interfaces(): String = try {
 val rows=mutableListOf<String>()
 val all=NetworkInterface.getNetworkInterfaces()
 while(all!=null && all.hasMoreElements()){
 val n=all.nextElement()
 if(!n.isUp || n.isLoopback) continue
 val addresses=n.inetAddresses
 while(addresses.hasMoreElements()){
 val a=addresses.nextElement()
 if(a is Inet6Address && a.isLinkLocalAddress) rows.add(n.name+": "+a.hostAddress)
 }
 }
 if(rows.isEmpty()) "No visible link-local IPv6 interfaces" else rows.joinToString("\n")
 } catch(e:Exception){"Network information unavailable"}
}
