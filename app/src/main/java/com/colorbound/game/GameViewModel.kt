package com.colorbound.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlin.math.max
import kotlin.random.Random

class GameViewModel(app: Application): AndroidViewModel(app) {
    private val repo=LevelRepository(app)
    private val store=ProgressStore(app)
    val levels: List<Level> by lazy { repo.all() }

    var screen by mutableStateOf(Screen.HOME); private set
    var currentLevel by mutableStateOf<Level?>(null); private set
    var selected by mutableStateOf<Set<Cell>>(emptySet()); private set
    var candidates by mutableStateOf<Set<Cell>>(emptySet()); private set
    var revealedEmpty by mutableStateOf<Set<Cell>>(emptySet()); private set
    var reveals by mutableStateOf<List<Reveal>>(emptyList()); private set
    var lives by mutableStateOf(3); private set
    var mistakes by mutableStateOf(0); private set
    var hintsUsed by mutableStateOf(0); private set
    var hintTokens by mutableStateOf(store.hintTokens); private set
    var hintMessage by mutableStateOf<HintMessage?>(null); private set
    var gameOver by mutableStateOf(false); private set
    var completed by mutableStateOf(false); private set
    var startedAt by mutableStateOf(System.currentTimeMillis()); private set
    var elapsedSeconds by mutableStateOf(0L); private set

    val sound get()=store.sound
    val vibration get()=store.vibration
    val colorBlind get()=store.colorBlind
    val highContrast get()=store.highContrast
    val highestUnlocked get()=store.highestUnlocked

    fun navigate(s:Screen){ screen=s }
    fun dismissHint(){ hintMessage=null }
    fun isCompleted(id:Int)=store.isCompleted(id)

    fun startLevel(id:Int){
        val level=levels.first { it.id==id }
        currentLevel=level
        selected=level.startingClues.toSet()
        candidates=emptySet(); revealedEmpty=emptySet(); reveals=emptyList()
        lives=3; mistakes=0; hintsUsed=0; hintMessage=null; gameOver=false; completed=false
        startedAt=System.currentTimeMillis(); elapsedSeconds=0
        screen=Screen.GAME
        if (PuzzleEngine.isComplete(level,selected)) finishLevel()
    }

    fun tick(){ if(screen==Screen.GAME && !gameOver && !completed) elapsedSeconds=(System.currentTimeMillis()-startedAt)/1000 }

    fun tap(cell:Cell){
        if(gameOver||completed)return
        val level=currentLevel ?: return
        if(cell in level.startingClues || cell in selected)return
        candidates=if(cell in candidates) candidates-cell else candidates+cell
    }

    fun doubleTap(cell:Cell){
        if(gameOver||completed)return
        val level=currentLevel ?: return
        if(cell in level.startingClues || cell in selected)return
        candidates=candidates-cell
        if(PuzzleEngine.isCorrect(level,cell)){
            selected=selected+cell
            if(PuzzleEngine.isComplete(level,selected)) finishLevel()
        }else{
            mistakes++
            lives=max(0,lives-1)
            if(lives==0)gameOver=true
        }
    }

    fun useItemFinder(){
        if(!consumeHint())return
        val level=currentLevel ?: return
        val target=(level.solution-selected).firstOrNull() ?: return
        selected=selected+target; candidates=candidates-target; hintsUsed++
        if(PuzzleEngine.isComplete(level,selected)) finishLevel()
    }

    fun useLogicHint(){
        if(!consumeHint())return
        val level=currentLevel ?: return
        hintMessage=PuzzleEngine.logicHint(level,selected,revealedEmpty)
        hintsUsed++
    }

    fun useThreeCellReveal(){
        if(!consumeHint())return
        val level=currentLevel ?: return
        val unresolved=(0 until level.size).flatMap { r -> (0 until level.size).map { c -> Cell(r,c) } }
            .filter { it !in selected && it !in revealedEmpty && it !in level.startingClues }
            .shuffled(Random(level.id+hintsUsed))
            .take(3)
        var sel=selected; var empty=revealedEmpty
        unresolved.forEach { cell -> if(cell in level.solution) sel=sel+cell else empty=empty+cell }
        selected=sel; revealedEmpty=empty
        reveals=unresolved.map { Reveal(it,it in level.solution) }
        hintsUsed++
        if(PuzzleEngine.isComplete(level,selected)) finishLevel()
    }

    private fun consumeHint():Boolean{
        if(hintTokens<=0 || gameOver || completed)return false
        hintTokens--; store.hintTokens=hintTokens; return true
    }

    private fun finishLevel(){
        val level=currentLevel ?: return
        completed=true
        elapsedSeconds=(System.currentTimeMillis()-startedAt)/1000
        val score=(1000 + max(0,300-elapsedSeconds.toInt()*3) - mistakes*100 - hintsUsed*75).coerceAtLeast(100)
        store.saveBest(level.id,score,elapsedSeconds)
        store.setCompleted(level.id)
        hintTokens++; store.hintTokens=hintTokens
    }

    fun retry(){ currentLevel?.id?.let(::startLevel) }
    fun nextLevel(){ currentLevel?.let { if(it.id<1000) startLevel(it.id+1) } }
    fun addTokens(amount:Int=3){ hintTokens+=amount; store.hintTokens=hintTokens }

    fun setSound(v:Boolean){store.sound=v}
    fun setVibration(v:Boolean){store.vibration=v}
    fun setColorBlind(v:Boolean){store.colorBlind=v}
    fun setHighContrast(v:Boolean){store.highContrast=v}
}