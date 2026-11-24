package com.nexora.ev

import io.grpc.CallOptions
import io.grpc.MethodDescriptor
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.ClientCalls
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.Inet6Address
import java.util.concurrent.TimeUnit

/** Single read-only version request. No generic command execution or authentication bypass. */
object VehicleVersion {
    private object Bytes : MethodDescriptor.Marshaller<ByteArray> {
        override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)
        override fun parse(stream: InputStream): ByteArray {
            val data=stream.readBytes()
            require(data.size<=8192)
            return data
        }
    }
    private val versionMethod=MethodDescriptor.newBuilder<ByteArray,ByteArray>()
        .setType(MethodDescriptor.MethodType.UNARY)
        .setFullMethodName("wifican.WifiCanRpc/GetSwVersion")
        .setRequestMarshaller(Bytes).setResponseMarshaller(Bytes).build()
    fun read(peer: Inet6Address, port: Int): String {
        require(peer.isLinkLocalAddress && peer.scopeId>0)
        val channel=OkHttpChannelBuilder.forAddress(peer.hostAddress,port)
            .usePlaintext().maxInboundMessageSize(8192).build()
        try {
            val data=ClientCalls.blockingUnaryCall(channel,versionMethod,CallOptions.DEFAULT.withDeadlineAfter(5,TimeUnit.SECONDS),ByteArray(0))
            return decode(data)
        } finally { channel.shutdownNow() }
    }
    internal fun decode(data: ByteArray): String {
        var pos=0
        fun number(): Long {
            var v=0L
            for(shift in 0..56 step 7) {
                require(pos<data.size)
                val b=data[pos++].toInt() and 255
                v=v or ((b and 127).toLong() shl shift)
                if(b and 128==0) return v
            }
            throw IllegalArgumentException("Malformed response")
        }
        var result=""
        while(pos<data.size) {
            val key=number()
            require(key>0)
            when((key and 7).toInt()) {
                0 -> number()
                2 -> {
                    val length=number()
                    require(length>=0 && length<=data.size-pos)
                    if(key ushr 3==2L) result=String(data,pos,length.toInt(),Charsets.UTF_8)
                    pos+=length.toInt()
                }
                else -> throw IllegalArgumentException("Unsupported response")
            }
        }
        val line=result.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
        require(line.isNotBlank() && line.length<=160 && line.none { it.isISOControl() })
        return line
    }
}
