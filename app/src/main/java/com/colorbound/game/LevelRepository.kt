package com.colorbound.game
import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader
class LevelRepository(private val context:Context){
 private var cache:List<Level>?=null
 fun all():List<Level>{cache?.let{return it};val files=context.assets.list("levels")?.filter{it.endsWith(".json")}?.sorted().orEmpty();val result=buildList{for(file in files){val text=context.assets.open("levels/$file").bufferedReader().use(BufferedReader::readText);val array=JSONArray(text);for(i in 0 until array.length())add(parseLevel(array.getJSONObject(i)))}}.sortedBy{it.id};check(result.size==1000){"Expected exactly 1000 levels, got ${result.size}"};cache=result;return result}
 private fun parseLevel(o:org.json.JSONObject):Level{val n=o.getInt("size");val gj=o.getJSONArray("grid");val grid=List(n){r->List(n){c->gj.getJSONArray(r).getInt(c)}};val sj=o.getJSONArray("solution");val solution=buildSet{for(i in 0 until sj.length()){val a=sj.getJSONArray(i);add(Cell(a.getInt(0),a.getInt(1)))}};val cj=o.optJSONArray("startingClues")?:JSONArray();val clues=buildSet{for(i in 0 until cj.length()){val a=cj.getJSONArray(i);add(Cell(a.getInt(0),a.getInt(1)))}};val meta=o.optJSONObject("metadata");return Level(o.getInt("id"),n,o.getString("difficulty"),o.getInt("colors"),grid,solution,clues,o.optInt("maxMistakes",3),meta?.optString("patternFamily","")?:"",meta?.optDouble("estimatedDifficulty",1.0)?:1.0)}
}