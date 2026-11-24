package com.example.sleepmonitor
import com.example.sleepmonitor.data.decodeRemoteConnection
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test
class RemoteConnectionTest {
    private fun code(url:String,key:String="x".repeat(43))="sleep1."+Base64.getUrlEncoder().withoutPadding().encodeToString("$url\n$key".toByteArray())
    @Test fun validCodeKeepsCredentialsAndUsesRealData(){val c=decodeRemoteConnection(code("https://example.trycloudflare.com"));assertEquals("https://example.trycloudflare.com",c.url);assertEquals(43,c.key.length);assertFalse(c.demo)}
    @Test fun rejectsPlainHttpCredentialsInUrlAndMalformedCodes(){
        for(v in listOf("bad",code("http://example.com"),code("https://user:pass@example.com"),code("https://example.com?key=x"),code("https://example.com","demo"))){
            assertThrows(Exception::class.java){decodeRemoteConnection(v)}
        }
    }
}
