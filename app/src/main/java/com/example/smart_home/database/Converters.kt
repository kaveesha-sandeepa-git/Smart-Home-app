package com.example.smart_home.database

import androidx.room.TypeConverter
import com.example.smart_home.models.MultiSwitch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    companion object {
        private val gson = Gson()

        @TypeConverter
        @JvmStatic
        fun fromSwitchList(list: MutableList<MultiSwitch.SwitchState>?): String? {
            if (list == null) return null
            val type = object : TypeToken<MutableList<MultiSwitch.SwitchState>>() {}.type
            return gson.toJson(list, type)
        }

        @TypeConverter
        @JvmStatic
        fun toSwitchList(json: String?): MutableList<MultiSwitch.SwitchState>? {
            if (json == null) return null
            val type = object : TypeToken<MutableList<MultiSwitch.SwitchState>>() {}.type
            return gson.fromJson(json, type)
        }

        @TypeConverter
        @JvmStatic
        fun fromLong(value: Long?): String? {
            return value?.toString()
        }

        @TypeConverter
        @JvmStatic
        fun toLong(value: String?): Long? {
            return value?.toLongOrNull()
        }
    }
}
