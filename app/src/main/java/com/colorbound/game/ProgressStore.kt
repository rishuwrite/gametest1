package com.colorbound.game

import android.content.Context

class ProgressStore(context: Context) {
    private val prefs=context.getSharedPreferences("colorbound_progress",Context.MODE_PRIVATE)
    var highestUnlocked: Int
        get()=prefs.getInt("highestUnlocked",1)
        set(v)=prefs.edit().putInt("highestUnlocked",v.coerceIn(1,1000)).apply()
    var hintTokens: Int
        get()=prefs.getInt("hintTokens",5)
        set(v)=prefs.edit().putInt("hintTokens",v.coerceAtLeast(0)).apply()
    var sound: Boolean
        get()=prefs.getBoolean("sound",true)
        set(v)=prefs.edit().putBoolean("sound",v).apply()
    var vibration: Boolean
        get()=prefs.getBoolean("vibration",true)
        set(v)=prefs.edit().putBoolean("vibration",v).apply()
    var colorBlind: Boolean
        get()=prefs.getBoolean("colorBlind",false)
        set(v)=prefs.edit().putBoolean("colorBlind",v).apply()
    var highContrast: Boolean
        get()=prefs.getBoolean("highContrast",false)
        set(v)=prefs.edit().putBoolean("highContrast",v).apply()
    private var lastDailyClaim: String
        get()=prefs.getString("lastDailyClaim","") ?: ""
        set(v)=prefs.edit().putString("lastDailyClaim",v).apply()

    fun claimDailyTokens(): Boolean {
        val today=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date())
        if(lastDailyClaim==today) return false
        lastDailyClaim=today
        return true
    }

    fun isCompleted(id:Int)=prefs.getBoolean("completed_$id",false)
    fun setCompleted(id:Int) { prefs.edit().putBoolean("completed_$id",true).apply(); if(id<1000 && highestUnlocked<id+1) highestUnlocked=id+1 }
    fun bestScore(id:Int)=prefs.getInt("score_$id",0)
    fun bestTime(id:Int)=prefs.getLong("time_$id",Long.MAX_VALUE)
    fun saveBest(id:Int,score:Int,time:Long) { val e=prefs.edit(); if(score>bestScore(id))e.putInt("score_$id",score); if(time<bestTime(id))e.putLong("time_$id",time); e.apply() }
}
