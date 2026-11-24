package com.example.sleepmonitor

import android.os.Bundle
import android.content.Intent
import com.example.sleepmonitor.reminders.SleepReminders
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.sleepmonitor.data.AppearanceMode
import com.example.sleepmonitor.ui.theme.*
import com.example.sleepmonitor.ui.components.*
import com.example.sleepmonitor.ui.home.*
import com.example.sleepmonitor.ui.sleep.*
import com.example.sleepmonitor.ui.environment.*
import com.example.sleepmonitor.ui.history.*
import com.example.sleepmonitor.ui.stats.*
import com.example.sleepmonitor.ui.settings.*
import com.example.sleepmonitor.ui.chat.*
import com.example.sleepmonitor.viewmodel.*

class MainActivity:ComponentActivity(){
    private val vm:SleepViewModel by viewModels()
    private val chat:ChatViewModel by viewModels()
    private val reminderTarget=mutableStateOf<String?>(null)
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);reminderTarget.value=intent.getStringExtra("reminderDestination")}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);reminderTarget.value=intent.getStringExtra("reminderDestination");SleepReminders.schedule(this);setContent{
        val state by vm.state.collectAsStateWithLifecycle()
        SleepTheme(state.motionEnabled,state.appearanceMode){SleepApp(vm,chat,reminderTarget.value){reminderTarget.value=null}}
    }}
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SleepApp(vm:SleepViewModel,chat:ChatViewModel,reminderTarget:String?=null,consumeReminder:()->Unit={}){
    val state by vm.state.collectAsStateWithLifecycle()
    val nav=rememberNavController();val snack=remember{SnackbarHostState()}
    val route=nav.currentBackStackEntryAsState().value?.destination?.route?:"home"
    val motion=LocalMotionEnabled.current
    val dark=LocalDarkTheme.current
    val keyboard=WindowInsets.ime.getBottom(LocalDensity.current)>0
    val tabs=listOf(Triple("home","Tổng quan",Icons.Outlined.Home),Triple("sleep","Ghi ngủ",Icons.Outlined.Bedtime),Triple("history","Lịch sử",Icons.Outlined.History),Triple("stats","Thống kê",Icons.Outlined.BarChart))
    fun openTab(destination:String){
        if(nav.currentDestination?.route==destination)return
        if(destination=="home"){
            // Home stays on the stack. Restoring its saved state can revive a
            // child stack (for example chat -> history) instead of showing Home.
            if(!nav.popBackStack("home",inclusive=false,saveState=true)){
                nav.navigate("home"){launchSingleTop=true}
            }
        }else nav.navigate(destination){
            popUpTo("home"){saveState=true}
            launchSingleTop=true
            restoreState=true
        }
    }
    fun open(destination:String){
        if(tabs.any{it.first==destination})openTab(destination)
        else nav.navigate(destination){launchSingleTop=true}
    }
    LaunchedEffect(reminderTarget){reminderTarget?.let{target->if(target.startsWith("rate:"))vm.openRating(target.removePrefix("rate:"))else open(if(target=="sleep")"sleep" else "home");consumeReminder()}}
    LaunchedEffect(state.notice){if(state.notice.isNotEmpty()){snack.showSnackbar(state.notice);vm.dismissNotice()}}
    LaunchedEffect(state.savedId){state.savedId?.let{id->nav.navigate("detail/$id"){popUpTo("home");launchSingleTop=true};vm.consumeSaved()}}
    val backdrop=rememberGraphicsLayer()
    val scene=remember(backdrop){GlassScene(backdrop)}
    CompositionLocalProvider(LocalGlassScene provides scene){Box(Modifier.fillMaxSize()){
        GlassAtmosphere(Modifier.matchParentSize(),scene)
        Scaffold(containerColor=Color.Transparent,contentColor=Ink,snackbarHost={SnackbarHost(snack){Snackbar(it,containerColor=CardRaised,contentColor=Ink,actionColor=Purple)}},topBar={
            SleepToolbar(route,tabs.any{it.first==route},{nav.popBackStack()},
                {vm.appearance(if(dark)AppearanceMode.LIGHT else AppearanceMode.DARK)},{open("settings")})
        },bottomBar={if(tabs.any{it.first==route}&&!keyboard)SleepDock(route,::openTab,{open("chat")})}){padding->
            NavHost(nav,startDestination="home",Modifier.padding(padding).consumeWindowInsets(padding),
                enterTransition={fadeIn(tween(if(motion)380 else 0))+slideInHorizontally(tween(if(motion)380 else 0)){it/18}},
                exitTransition={fadeOut(tween(if(motion)160 else 0))},popEnterTransition={fadeIn(tween(if(motion)320 else 0))},popExitTransition={fadeOut(tween(if(motion)160 else 0))+slideOutHorizontally(tween(if(motion)260 else 0)){it/18}}){
                composable("home"){HomeScreen(state,vm::startSleep,vm::wakeUp,environment={open("env")},review=vm::openRating,history={open("history")},statistics={open("stats")})}
                composable("sleep"){SleepScreen(null){id,start,end,q,n,i->vm.saveManual(id,start,end,q,n,i)}}
                composable("env"){EnvironmentScreen(state,vm::lamp)}
                composable("stats"){StatisticsScreen(state.sessions)}
                composable("history"){HistoryScreen(state.sessions,{open("detail/$it")},{open("sleep")})}
                composable("settings"){SettingsScreen(state,vm::connect,vm::saveSettings,vm::reloadServer,vm::motion,vm::appearance,{open("env")},{open("chat")},vm::personalizeAi)}
                composable("chat"){
                    val chatState by chat.state.collectAsStateWithLifecycle()
                    LaunchedEffect(state.connection){chat.bind(state.connection);chat.refreshStatus()}
                    ChatScreen(state,chatState,chat){open(it)}
                }
                composable("detail/{id}"){entry->val id=entry.arguments?.getString("id")?:"";LaunchedEffect(id){vm.detail(id)};DetailScreen(id,state,{open("edit/$id")},{vm.deleteSession(id);nav.popBackStack()})}
                composable("edit/{id}"){entry->val id=entry.arguments?.getString("id");val s=state.sessions.find{it.id==id};SleepScreen(s){sid,start,end,q,n,i->vm.saveManual(sid,start,end,q,n,i)}}
            }
        }
    }
    }
    state.ratingId?.let{id->state.sessions.find{it.id==id}?.let{s->RatingDialog(s,vm::closeRating){q,n,i->vm.rate(id,q,n,i)}}}
}
