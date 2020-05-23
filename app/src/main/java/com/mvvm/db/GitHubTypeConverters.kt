package com.mvvm.db

import android.util.Log
import androidx.room.TypeConverter
import java.lang.NumberFormatException

object GitHubTypeConverters {
    @TypeConverter
    @JvmStatic
    fun stringToIntList(data: String?): List<Int>?{
        return data?.let {
            it.split(",").map {
                try {
                    it.toInt()
                } catch (e: NumberFormatException) {
                    Log.d("TAG1", "No puede convertir a numero")
                    null
                }
            }.filterNotNull()
        }
    }

    @TypeConverter
    @JvmStatic
    fun intToStringList(ints: List<Int>?): String? {
        return ints?.joinToString { "," }
    }
}