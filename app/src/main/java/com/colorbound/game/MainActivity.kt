package com.colorbound.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlin.math.min

private val Ink=Color(0xFFE8EEF7)
private val Muted=Color(0xFF9BA9BC)
private val Surface=Color(0xFF172033)
private val Surface2=Color(0xFF202C43)
private val Background=Color(0xFF0D1422)
private val Accent=Color(0xFF63D6C4)
private val Warning=Color(0xFFFFB86B)
private val Danger=Color(0xFFFF6B7A)

private val RegionColors=listOf(
    Color(0xFF4D8DFF),Color(0xFF7C65E8),Color(0xFFE56BC2),Color(0xFFFF7F72),Color(0xFFF2B84B),
    Color(0xFF49B987),Color(0xFF39AFC3),Color(0xFF9C76D8),Color(0xFFDF6A8B),Color(0xFF8CB84F),
    Color(0xFF5E9AA8),Color(0xFFD48A55),Color(0xFF6D83C7),Color(0xFFB66A9E),Color(0xFF789E65)
)

class MainActivity: ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent { ColorboundApp() }
    }
}

@Composable
fun ColorboundApp(vm:GameViewModel=viewModel()){
    MaterialTheme(colorScheme=darkColorScheme(background=Background,surface=Surface,primary=Accent,onPrimary=Background,onSurface=Ink)){
        LaunchedEffect(vm.screen,vm.completed,vm.gameOver){ while(vm.screen==Screen.GAME && !vm.completed && !vm.gameOver){ vm.tick(); delay(1000) } }
        when(vm.screen){
            Screen.HOME->HomeScreen(vm)
            Screen.MAP->MapScreen(vm)
            Screen.GAME->GameScreen(vm)
            Screen.SETTINGS->SettingsScreen(vm)
        }
    }
}

@Composable private fun Page(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    Column(modifier.fillMaxSize().background(Background).padding(horizontal=18.dp,vertical=16.dp),content=content)
}

@Composable private fun HomeScreen(vm:GameViewModel){
    Page {
        Spacer(Modifier.height(28.dp))
        Text("COLORBOUND",fontSize=32.sp,fontWeight=FontWeight.Black,letterSpacing=2.sp,color=Ink)
        Text("Prism Grove",fontSize=16.sp,color=Accent,fontWeight=FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text("A quiet logic garden where every hue forms one connected territory.",color=Muted,fontSize=15.sp)
        Spacer(Modifier.height(28.dp))
        ProgressCard(vm)
        Spacer(Modifier.height(22.dp))
        PrimaryButton("CONTINUE LEVEL ${vm.highestUnlocked}"){vm.startLevel(vm.highestUnlocked)}
        Spacer(Modifier.height(12.dp))
        SecondaryButton("LEVEL GARDEN"){vm.navigate(Screen.MAP)}
        Spacer(Modifier.height(12.dp))
        SecondaryButton("SETTINGS"){vm.navigate(Screen.SETTINGS)}
        Spacer(Modifier.weight(1f))
        Text("1000 fixed puzzles • offline-first • original visual identity",color=Muted,fontSize=12.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())
    }
}

@Composable private fun ProgressCard(vm:GameViewModel){
    val done=(1..1000).count { vm.isCompleted(it) }
    Card(colors=CardDefaults.cardColors(containerColor=Surface),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(20.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){Text("GARDEN PROGRESS",fontSize=12.sp,color=Muted,fontWeight=FontWeight.Bold);Text("$done / 1000",fontSize=26.sp,fontWeight=FontWeight.Bold)}
                Text("◆",fontSize=28.sp,color=Accent)
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(progress={done/1000f},modifier=Modifier.fillMaxWidth(),color=Accent,trackColor=Surface2)
        }
    }
}

@Composable private fun MapScreen(vm:GameViewModel){
    Page {
        Row(verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick={vm.navigate(Screen.HOME)}){Text("‹ BACK",color=Accent,fontWeight=FontWeight.Bold)}
            Spacer(Modifier.weight(1f)); Text("LEVEL GARDEN",fontWeight=FontWeight.Black,fontSize=20.sp)
        }
        Text("Choose any unlocked grove. Completed levels remain replayable.",color=Muted,fontSize=13.sp,modifier=Modifier.padding(4.dp))
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(columns=GridCells.Fixed(5),contentPadding=PaddingValues(4.dp),horizontalArrangement=Arrangement.spacedBy(9.dp),verticalArrangement=Arrangement.spacedBy(9.dp),modifier=Modifier.fillMaxSize()){
            items(vm.levels){ level ->
                val unlocked=level.id<=vm.highestUnlocked
                val done=vm.isCompleted(level.id)
                Box(contentAlignment=Alignment.Center,modifier=Modifier.size(58.dp)){
                    Button(onClick={if(unlocked)vm.startLevel(level.id)},enabled=unlocked,shape=RoundedCornerShape(17.dp),contentPadding=PaddingValues(0.dp),colors=ButtonDefaults.buttonColors(containerColor=if(done)Color(0xFF294E57) else Surface2,disabledContainerColor=Color(0xFF141B29),contentColor=Ink,disabledContentColor=Color(0xFF465268)),modifier=Modifier.fillMaxSize()){
                        Column(horizontalAlignment=Alignment.CenterHorizontally){Text(level.id.toString(),fontWeight=FontWeight.Bold,fontSize=14.sp);if(done)Text("◆",fontSize=8.sp,color=Accent)}
                    }
                }
            }
        }
    }
}

@Composable private fun GameScreen(vm:GameViewModel){
    val level=vm.currentLevel ?: return
    Page {
        Row(verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick={vm.navigate(Screen.MAP)}){Text("‹",fontSize=30.sp,color=Ink)}
            Column(Modifier.weight(1f)){
                Text("LEVEL ${level.id}",fontSize=18.sp,fontWeight=FontWeight.Black)
                Text(difficultyLabel(level.difficulty),fontSize=12.sp,color=Accent,fontWeight=FontWeight.Bold)
            }
            Text("❤ ".repeat(vm.lives).ifEmpty{"—"},color=Danger,fontSize=16.sp)
            Spacer(Modifier.width(8.dp)); Text(formatTime(vm.elapsedSeconds),color=Muted,fontSize=13.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("Every region, row and column gets one prism. Neighbors cannot touch. Drag from any normal cell to discard • drag from a discarded cell to restore. Auto-discarded color cells are locked.",color=Muted,fontSize=12.sp)
        Spacer(Modifier.height(10.dp))
        PuzzleBoard(vm,level,Modifier.fillMaxWidth().weight(1f))
        Spacer(Modifier.height(10.dp))
        HintBar(vm)
        if(vm.hintMessage!=null){ HintCard(vm.hintMessage!!){vm.dismissHint()} }
        if(vm.gameOver) GameOverDialog(vm)
        if(vm.completed) CompleteDialog(vm)
    }
}

@Composable private fun PuzzleBoard(vm:GameViewModel,level:Level,modifier:Modifier){
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1f).padding(2.dp),contentAlignment=Alignment.Center){
        val n=level.size
        val density=androidx.compose.ui.platform.LocalDensity.current.density
        Canvas(Modifier.fillMaxSize().pointerInput(level.id,vm.gameOver,vm.completed,density){
            var lastTapTime=0L
            var lastTapCell:Cell?=null
            awaitEachGesture {
                val down=awaitFirstDown(requireUnconsumed=false)
                val startCell=pointToCell(down.position,size.width.toFloat(),size.height.toFloat(),n,density) ?: return@awaitEachGesture

                var moved=false
                var lastCell=startCell
                val discardMode=vm.shouldDiscardOnSwipe(startCell)
                drag(down.id){change->
                    moved=true
                    val currentCell=pointToCell(change.position,size.width.toFloat(),size.height.toFloat(),n,density)
                    if(currentCell!=null && currentCell!=lastCell){
                        swipeLine(lastCell,currentCell,n){cell->
                            vm.swipeCell(cell,discardMode)
                        }
                        lastCell=currentCell
                    }
                    change.consume()
                }

                if(moved){
                    lastTapCell=null
                }else{
                    val now=System.currentTimeMillis()
                    if(lastTapCell==startCell && now-lastTapTime<=350L){
                        lastTapCell=null
                        vm.doubleTap(startCell)
                    }else{
                        lastTapCell=startCell
                        lastTapTime=now
                        vm.tap(startCell)
                    }
                }
            }
        }){
            val gap=4.dp.toPx(); val cell=(size.minDimension-gap*(n-1))/n
            val board=(cell*n+gap*(n-1)); val ox=(size.width-board)/2; val oy=(size.height-board)/2
            for(r in 0 until n) for(c in 0 until n){
                val x=ox+c*(cell+gap);val y=oy+r*(cell+gap);val rid=level.grid[r][c];var color=RegionColors[rid%RegionColors.size]
                if(vm.colorBlind) color=androidx.compose.ui.graphics.lerp(color,Color.White,.12f)
                if(vm.highContrast) color=androidx.compose.ui.graphics.lerp(color,Color.Black,.15f)
                drawRoundRect(color,topLeft=Offset(x,y),size=Size(cell,cell),cornerRadius=CornerRadius(cell*.18f))
                drawPattern(rid,Offset(x,y),cell,color)
                val cc=Cell(r,c)
                if(cc in vm.revealedEmpty || cc in vm.autoDiscarded){
                    val isAuto=cc in vm.autoDiscarded
                    val alpha=if(isAuto) .78f else .70f
                    if(isAuto){
                        // Strong visual lock: a bright border, double X and center ring.
                        drawRoundRect(Accent.copy(alpha=.72f),topLeft=Offset(x+cell*.055f,y+cell*.055f),size=Size(cell*.89f,cell*.89f),cornerRadius=CornerRadius(cell*.14f),style=Stroke(width=cell*.045f))
                        drawLine(Color.White.copy(alpha=.82f),Offset(x+cell*.25f,y+cell*.25f),Offset(x+cell*.75f,y+cell*.75f),cell*.075f,StrokeCap.Round)
                        drawLine(Color.White.copy(alpha=.82f),Offset(x+cell*.75f,y+cell*.25f),Offset(x+cell*.25f,y+cell*.75f),cell*.075f,StrokeCap.Round)
                        drawCircle(Accent.copy(alpha=.95f),radius=cell*.12f,center=Offset(x+cell*.5f,y+cell*.5f),style=Stroke(width=cell*.035f))
                        drawCircle(Accent.copy(alpha=.95f),radius=cell*.035f,center=Offset(x+cell*.5f,y+cell*.5f))
                    }else{
                        // Manual discard uses the exact same bold X treatment as auto-discard.
                        drawRoundRect(Ink.copy(alpha=.28f),topLeft=Offset(x+cell*.055f,y+cell*.055f),size=Size(cell*.89f,cell*.89f),cornerRadius=CornerRadius(cell*.14f),style=Stroke(width=cell*.035f))
                        drawLine(Color.White.copy(alpha=.92f),Offset(x+cell*.22f,y+cell*.22f),Offset(x+cell*.78f,y+cell*.78f),cell*.075f,StrokeCap.Round)
                        drawLine(Color.White.copy(alpha=.92f),Offset(x+cell*.78f,y+cell*.22f),Offset(x+cell*.22f,y+cell*.78f),cell*.075f,StrokeCap.Round)
                    }
                }
                if(cc in vm.candidates){ drawCircle(Ink.copy(alpha=.9f),radius=cell*.10f,center=Offset(x+cell/2,y+cell/2)) }
                if(cc in vm.selected){
                    val cx=x+cell/2;val cy=y+cell/2;val p=Path().apply{moveTo(cx,cy-cell*.25f);lineTo(cx+cell*.25f,cy);lineTo(cx,cy+cell*.25f);lineTo(cx-cell*.25f,cy);close()}
                    drawPath(p,color=Ink);drawPath(p,color=Color.White,style=Stroke(width=2f))
                }
                if(cc in level.startingClues){ drawCircle(Warning,radius=cell*.08f,center=Offset(x+cell*.78f,y+cell*.22f)) }
            }
        }
    }
}

private fun swipeLine(from:Cell,to:Cell,n:Int,onCell:(Cell)->Unit){
    var x0=from.col; var y0=from.row
    val x1=to.col; val y1=to.row
    val dx=kotlin.math.abs(x1-x0); val sx=if(x0<x1) 1 else -1
    val dy=-kotlin.math.abs(y1-y0); val sy=if(y0<y1) 1 else -1
    var err=dx+dy
    while(true){
        if(y0 in 0 until n && x0 in 0 until n) onCell(Cell(y0,x0))
        if(x0==x1 && y0==y1) break
        val e2=2*err
        if(e2>=dy){err+=dy; x0+=sx}
        if(e2<=dx){err+=dx; y0+=sy}
    }
}

private fun pointToCell(p:Offset,width:Float,height:Float,n:Int,density:Float):Cell?{
    val gap=4f*density
    val board=min(width,height)
    val cell=(board-gap*(n-1))/n
    val ox=(width-(cell*n+gap*(n-1)))/2f
    val oy=(height-(cell*n+gap*(n-1)))/2f
    val c=((p.x-ox)/(cell+gap)).toInt()
    val r=((p.y-oy)/(cell+gap)).toInt()
    if(r !in 0 until n || c !in 0 until n)return null
    val lx=p.x-(ox+c*(cell+gap)); val ly=p.y-(oy+r*(cell+gap))
    if(lx<0 || ly<0 || lx>cell || ly>cell)return null
    return Cell(r,c)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPattern(rid:Int,top:Offset,cell:Float,color:Color){
    val a=Color.White.copy(alpha=.16f); val mode=rid%5
    when(mode){
        0->for(i in 1..2) drawCircle(a,radius=cell*.07f,center=Offset(top.x+cell*(.28f+.24f*i),top.y+cell*.72f))
        1->drawLine(a,Offset(top.x+cell*.25f,top.y+cell*.25f),Offset(top.x+cell*.75f,top.y+cell*.75f),cell*.055f,StrokeCap.Round)
        2->drawCircle(a,radius=cell*.24f,center=Offset(top.x+cell*.5f,top.y+cell*.5f),style=Stroke(width=cell*.05f))
        3->{drawLine(a,Offset(top.x+cell*.25f,top.y+cell*.65f),Offset(top.x+cell*.75f,top.y+cell*.65f),cell*.055f);drawLine(a,Offset(top.x+cell*.5f,top.y+cell*.3f),Offset(top.x+cell*.5f,top.y+cell*.65f),cell*.055f)}
        else->drawCircle(a,radius=cell*.10f,center=Offset(top.x+cell*.5f,top.y+cell*.5f))
    }
}

@Composable private fun HintBar(vm:GameViewModel){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(7.dp)){
        HintButton("FIND",vm.hintTokens){vm.useItemFinder()}
        HintButton("IDEA",vm.hintTokens){vm.useLogicHint()}
        HintButton("3 CELLS",vm.hintTokens){vm.useThreeCellReveal()}
    }
}

@Composable private fun RowScope.HintButton(label:String,tokens:Int,onClick:()->Unit){
    Button(onClick=onClick,modifier=Modifier.weight(1f).height(52.dp),shape=RoundedCornerShape(17.dp),colors=ButtonDefaults.buttonColors(containerColor=Surface2,contentColor=Ink),contentPadding=PaddingValues(4.dp)){
        Column(horizontalAlignment=Alignment.CenterHorizontally){Text(label,fontSize=11.sp,fontWeight=FontWeight.Black);Text("◆ $tokens",fontSize=9.sp,color=Accent)}
    }
}

@Composable private fun HintCard(message:HintMessage,onClose:()->Unit){
    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF243A48)),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth().padding(top=8.dp)){
        Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(message.title,fontWeight=FontWeight.Bold);Text(message.body,color=Muted,fontSize=12.sp)};TextButton(onClick=onClose){Text("OK",color=Accent)}}
    }
}

@Composable private fun GameOverDialog(vm:GameViewModel){
    AlertDialog(onDismissRequest={},title={Text("Grove paused")},text={Text("Three incorrect confirmations used all your lives. The fixed puzzle is unchanged.")},confirmButton={Button(onClick=vm::retry){Text("RETRY")}},dismissButton={TextButton(onClick={vm.navigate(Screen.MAP)}){Text("LEVEL MAP")}})
}

@Composable private fun CompleteDialog(vm:GameViewModel){
    val level=vm.currentLevel ?: return
    AlertDialog(onDismissRequest={},title={Text("PUZZLE SOLVED")},text={Text("Level ${level.id} complete in ${formatTime(vm.elapsedSeconds)}.\nMistakes: ${vm.mistakes}\nHints: ${vm.hintsUsed}")},confirmButton={Button(onClick={vm::nextLevel},enabled=level.id<1000){Text(if(level.id<1000)"NEXT LEVEL" else "DONE")}},dismissButton={TextButton(onClick={vm::retry}){Text("REPLAY")}})
}

@Composable private fun SettingsScreen(vm:GameViewModel){
    Page{
        Row(verticalAlignment=Alignment.CenterVertically){TextButton(onClick={vm.navigate(Screen.HOME)}){Text("‹ BACK",color=Accent,fontWeight=FontWeight.Bold)};Spacer(Modifier.weight(1f));Text("SETTINGS",fontSize=20.sp,fontWeight=FontWeight.Black)}
        Spacer(Modifier.height(20.dp))
        SettingRow("Sound effects","Small feedback sounds for actions",vm.sound,vm::setSound)
        SettingRow("Vibration","Haptic feedback for mistakes and success",vm.vibration,vm::setVibration)
        SettingRow("Colorblind mode","Adds stronger pattern contrast",vm.colorBlind,vm::setColorBlind)
        SettingRow("High contrast","Boosts tile and text separation",vm.highContrast,vm::setHighContrast)
        Spacer(Modifier.height(20.dp))
        Card(colors=CardDefaults.cardColors(containerColor=Surface),shape=RoundedCornerShape(20.dp)){
            Column(Modifier.padding(18.dp)){Text("HINT TOKENS",fontWeight=FontWeight.Black);Text("${vm.hintTokens} available",fontSize=26.sp,color=Accent);Text("Tokens can be earned from completed levels and optional rewards in a future online layer. The 1000-level campaign remains playable offline.",color=Muted,fontSize=12.sp);Spacer(Modifier.height(10.dp));SecondaryButton("CLAIM DAILY +3"){vm.claimDailyTokens()}}
        }
    }
}

@Composable private fun SettingRow(title:String,desc:String,value:Boolean,onChange:(Boolean)->Unit){
    Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(desc,color=Muted,fontSize=12.sp)};Switch(checked=value,onCheckedChange=onChange)}
}

@Composable private fun PrimaryButton(label:String,onClick:()->Unit)=Button(onClick=onClick,modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(20.dp),colors=ButtonDefaults.buttonColors(containerColor=Accent,contentColor=Background)){Text(label,fontWeight=FontWeight.Black)}
@Composable private fun SecondaryButton(label:String,onClick:()->Unit)=OutlinedButton(onClick=onClick,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.outlinedButtonColors(contentColor=Ink)){Text(label,fontWeight=FontWeight.Bold)}
private fun formatTime(s:Long)="%02d:%02d".format(s/60,s%60)
