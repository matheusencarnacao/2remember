package br.com.tworemember.localizer.providers

import com.google.gson.Gson

class GsonConverter {

    companion object{
        fun getJson(obj: Any) : String{
            val gson = Gson()
            return gson.toJson(obj)
        }

        fun fromJson(json:String, kotlinClass: Class<*>) : Class<*>?{
            val gson = Gson()
            return gson.fromJson(json, kotlinClass::class.java)
        }
    }
}