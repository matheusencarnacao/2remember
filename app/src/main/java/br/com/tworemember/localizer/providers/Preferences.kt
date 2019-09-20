package br.com.tworemember.localizer.providers

import android.content.Context
import android.content.SharedPreferences
import br.com.tworemember.localizer.model.User
import com.google.gson.Gson

class Preferences(context: Context) {

    private var sharedPrefences: SharedPreferences = context
        .getSharedPreferences("2remember-prefs", Context.MODE_PRIVATE)

    fun setUser(user: User){
        val gson = Gson()
        val userJson = gson.toJson(user)
        sharedPrefences.edit().putString("User", userJson).apply()
    }

    fun getUser() : User?{
        val userJson = sharedPrefences.getString("User", null)
        if (userJson == null)
            return userJson

        val gson = Gson()
        return gson.fromJson<User>(userJson, User::class.java)

    }

    fun setMacAddress(macaddress: String){
        sharedPrefences.edit().putString("MacAddress", macaddress).apply()
    }

    fun getMacAddress(): String? {
        return sharedPrefences.getString("MacAddress", "")
    }

}