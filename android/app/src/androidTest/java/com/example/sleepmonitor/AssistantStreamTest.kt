package com.example.sleepmonitor

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepmonitor.data.ApiException
import com.example.sleepmonitor.data.readChatStream
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.*

@RunWith(AndroidJUnit4::class)
class AssistantStreamTest {
    @Test fun firstTextArrivesBeforeCompletion()=runBlocking {
        val pipe=PipedWriter();val reader=PipedReader(pipe).buffered()
        val first=CompletableDeferred<String>()
        val reply=async(Dispatchers.IO){reader.use{readChatStream(it){text->first.complete(text)}}}
        try{
            pipe.write("{\"type\":\"delta\",\"text\":\"Giấc ngủ êm dịu\"}\n");pipe.flush()
            assertEquals("Giấc ngủ êm dịu",withTimeout(5000){first.await()})
            assertFalse(reply.isCompleted)
            pipe.write("{\"type\":\"done\",\"id\":\"test-stream\",\"text\":\"Giấc ngủ êm dịu.\"}\n");pipe.flush()
            assertEquals("Giấc ngủ êm dịu.",withTimeout(5000){reply.await()}.getString("text"))
        }finally{pipe.close();reply.cancel()}
    }
    @Test fun truncatedAndErrorStreamsNeverBecomeFinishedAnswers()=runBlocking {
        val partial="{\"type\":\"delta\",\"text\":\"Chưa xong\"}\n"
        for(raw in listOf(partial,partial+"{\"type\":\"error\",\"status\":502,\"error\":\"Mất kết nối\"}\n",partial+"invalid\n")){
            var seen=""
            try{readChatStream(StringReader(raw).buffered()){seen+=it};fail("Incomplete stream was accepted")}
            catch(error:ApiException){assertTrue(error.message!!.isNotBlank())}
            assertEquals("Chưa xong",seen)
        }
    }
    @Test fun unicodeCanCrossEveryNetworkByteBoundary()=runBlocking {
        val raw="{\"type\":\"delta\",\"text\":\"Giấc ngủ\"}\n{\"type\":\"done\",\"id\":\"utf8\",\"text\":\"Giấc ngủ\"}\n"
        val data=raw.toByteArray(Charsets.UTF_8);var index=0
        val input=object:InputStream(){
            override fun read():Int=if(index<data.size)data[index++].toInt() and 255 else -1
            override fun read(buffer:ByteArray,offset:Int,length:Int):Int{
                if(length==0)return 0
                val byte=read();if(byte<0)return -1;buffer[offset]=byte.toByte();return 1
            }
        }
        var seen="";val result=readChatStream(input.bufferedReader(Charsets.UTF_8)){seen+=it}
        assertEquals("Giấc ngủ",seen);assertEquals(seen,result.getString("text"))
    }
}
