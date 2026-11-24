package com.example.sleepmonitor.data

import java.net.URI
import java.util.Base64
import java.security.MessageDigest

fun connectionFingerprint(c:Connection)=MessageDigest.getInstance("SHA-256").digest("${c.url}|${c.key}|${c.demo}".toByteArray()).joinToString(""){"%02x".format(it)}

/** A private pairing code is pasted explicitly; it is never accepted from an external intent. */
fun decodeRemoteConnection(code:String):Connection {
    val text=code.trim()
    require(text.startsWith("sleep1.")&&text.length<1600){"Mã kết nối không hợp lệ"}
    val payload=String(Base64.getUrlDecoder().decode(text.removePrefix("sleep1.")),Charsets.UTF_8).split('\n')
    require(payload.size==2){"Mã kết nối không hợp lệ"}
    val url=payload[0].trimEnd('/');val key=payload[1];val uri=URI(url)
    require(uri.scheme=="https"&&!uri.host.isNullOrBlank()&&uri.userInfo==null&&uri.query==null&&uri.fragment==null&&uri.path.isNullOrEmpty()){"Cần địa chỉ HTTPS hợp lệ"}
    require(key.matches(Regex("[A-Za-z0-9_-]{32,128}"))){"Khóa kết nối không hợp lệ"}
    return Connection(url,key,false)
}
