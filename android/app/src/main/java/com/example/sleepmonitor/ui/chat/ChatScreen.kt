package com.example.sleepmonitor.ui.chat

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepmonitor.ui.components.*
import com.example.sleepmonitor.ui.theme.*
import com.example.sleepmonitor.viewmodel.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable fun ChatScreen(app:AppState,state:ChatState,vm:ChatViewModel,navigate:(String)->Unit){
    val lifecycleOwner=LocalLifecycleOwner.current
    LaunchedEffect(vm,lifecycleOwner){
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
            while(isActive){vm.receiveVoice();delay(3000)}
        }
    }
    var draft by rememberSaveable{mutableStateOf("")}
    var clearDialog by remember{mutableStateOf(false)}
    var showCoach by rememberSaveable{mutableStateOf(false)}
    val focus by vm.focus.collectAsState()
    val style by vm.style.collectAsState()
    val useTopic by vm.useTopic.collectAsState()
    val list=rememberLazyListState()
    val keyboard=LocalSoftwareKeyboardController.current
    val submit={if(draft.isNotBlank()&&state.status.ready&&!state.sending){vm.send(draft,app);draft="";keyboard?.hide()}}
    LaunchedEffect(state.messages.size,state.sending){if(list.layoutInfo.totalItemsCount>0)list.animateScrollToItem(list.layoutInfo.totalItemsCount-1)}
    LaunchedEffect(state.partialAnswer){
        if(state.partialAnswer.isNotEmpty()&&!list.isScrollInProgress){
            val last=list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?:0
            if(last>=list.layoutInfo.totalItemsCount-2)list.scrollToItem(list.layoutInfo.totalItemsCount-1)
        }
    }
    Column(Modifier.fillMaxSize().imePadding()){
        Row(Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
            Spacer(Modifier.weight(1f))
            TextButton({showCoach=true}){Text("Gợi ý cho tôi",fontSize=13.sp)}
            if(state.messages.isNotEmpty())LiquidIconButton(Icons.Outlined.DeleteOutline,"Xóa hội thoại",{clearDialog=true},enabled=!state.sending)
        }
        LazyColumn(state=list,modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(22.dp,14.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            if(!state.status.ready&&!state.status.checking)item{
                Surface(modifier=Modifier.fillMaxWidth(),color=Card,shape=RoundedCornerShape(10.dp),border=BorderStroke(1.dp,Border)){
                    Column(Modifier.padding(14.dp)){
                        Text(state.status.message,color=Amber,fontWeight=FontWeight.Medium,fontSize=12.sp)
                        TextButton({vm.refreshStatus()}){Text("Thử kết nối lại",fontSize=12.sp)}
                    }
                }
            }
            if(state.messages.isEmpty())item{
                Reveal{Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
                    MoonOrb(Modifier.padding(bottom=12.dp).size(80.dp),progress=.86f,ai=true)
                    val prompts=listOf("Nhìn lại giấc ngủ 7 ngày qua","Phòng ngủ của tôi có gì cần chú ý?","Gợi ý thói quen thư giãn trước khi ngủ")
                    prompts.forEach{prompt->
                        Surface(onClick={draft=prompt},color=Card,shape=RoundedCornerShape(10.dp),border=BorderStroke(1.dp,Border),modifier=Modifier.fillMaxWidth().padding(bottom=9.dp)){
                            Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Text(prompt,Modifier.weight(1f),fontSize=13.sp);Icon(Icons.Outlined.NorthWest,null,tint=Purple,modifier=Modifier.size(17.dp))}
                        }
                    }
                }}
            }
            items(state.messages,key={it.id}){message->
                val user=message.role=="user"
                Column(Modifier.fillMaxWidth(),horizontalAlignment=if(user)Alignment.End else Alignment.Start){
                    Text(if(user)"BẠN" else "SLEEP AI",color=if(user)Muted else Purple,fontSize=10.sp,letterSpacing=1.sp,modifier=Modifier.padding(bottom=7.dp,start=3.dp,end=3.dp))
                    Surface(color=if(user)Lavender else Card,shape=RoundedCornerShape(12.dp,12.dp,if(user)4.dp else 12.dp,if(user)12.dp else 4.dp),border=BorderStroke(1.dp,if(user)Purple.copy(alpha=.25f)else Border),modifier=Modifier.widthIn(max=440.dp)){
                        SelectionContainer{Text(message.text,Modifier.padding(17.dp),fontSize=15.sp,lineHeight=24.sp)}
                    }
                    message.error?.let{error->
                        Text(error,color=Amber,fontSize=12.sp,lineHeight=18.sp,modifier=Modifier.padding(top=8.dp))
                        if(message.id==state.messages.lastOrNull()?.id)TextButton({vm.send(message.text,app,true)},enabled=state.status.ready&&!state.sending){Icon(Icons.Outlined.Refresh,null,Modifier.size(16.dp));Text("Thử lại",Modifier.padding(start=6.dp))}
                    }
                }
            }
            if(state.sending)item{
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    if(state.partialAnswer.isNotEmpty()){
                        Text("SLEEP AI",color=Purple,fontSize=10.sp,letterSpacing=1.sp,modifier=Modifier.padding(start=3.dp))
                        Surface(color=Card,shape=RoundedCornerShape(12.dp,12.dp,12.dp,4.dp),border=BorderStroke(1.dp,Border),modifier=Modifier.widthIn(max=440.dp)){
                            Text(state.partialAnswer,Modifier.padding(17.dp),fontSize=15.sp,lineHeight=24.sp)
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically){TypingDots();Text(if(state.partialAnswer.isEmpty())"Đang chuẩn bị câu trả lời…" else "Đang viết…",color=Muted,fontSize=12.sp)}
                }
            }
        }
        LiquidToolbarSurface(Modifier.fillMaxWidth().padding(horizontal=14.dp).padding(bottom=10.dp),corner=24){
            Column(Modifier.fillMaxWidth().padding(horizontal=12.dp).padding(top=10.dp,bottom=10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(9.dp)){
                    OutlinedTextField(draft,{draft=it.take(2000)},placeholder={Text("Bạn muốn ngủ tốt hơn thế nào?",fontSize=13.sp)},modifier=Modifier.weight(1f),shape=RoundedCornerShape(12.dp),maxLines=4,
                        keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={submit()}),
                        colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Border,focusedBorderColor=Purple,unfocusedContainerColor=Card,focusedContainerColor=Card))
                    LiquidIconButton(if(state.sending)Icons.Outlined.Stop else Icons.Outlined.ArrowUpward,
                        if(state.sending)"Dừng trả lời" else "Gửi câu hỏi",if(state.sending)({vm.cancel()})else submit,
                        enabled=state.sending||(draft.isNotBlank()&&state.status.ready),prominent=true)
                }
            }
        }
    }
    if(showCoach)PersonalCoachDialog(app,focus,style,useTopic,vm::chooseFocus,vm::chooseStyle,vm::useTopicInChat,navigate){showCoach=false}
    if(clearDialog)AlertDialog(onDismissRequest={clearDialog=false},title={Text("Xóa hội thoại trên máy?")},text={Text("Các tin nhắn trong cuộc trò chuyện này sẽ được xóa khỏi ứng dụng.")},confirmButton={TextButton({vm.clear();clearDialog=false}){Text("Xóa")}},dismissButton={TextButton({clearDialog=false}){Text("Giữ lại")}})
}
