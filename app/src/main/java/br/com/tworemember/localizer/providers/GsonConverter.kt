package br.com.tworemember.localizer.providers

import com.google.gson.Gson
import kotlin.reflect.KClass

class GsonConverter {

    companion object{
        fun getJson(obj: Any) : String{
            val gson = Gson()
            return gson.toJson(obj)
        }

        fun fromJson(json:String, kotlinClass: KClass<*>) : Any? {
            val gson = Gson()
            return gson.fromJson(json, kotlinClass.java)
        }
    }
}