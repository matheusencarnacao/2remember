package br.com.tworemember.localizer.providers

import android.content.Context
import android.content.SharedPreferences
import br.com.tworemember.localizer.model.User
import br.com.tworemember.localizer.webservices.model.CurrentPositionResponse
import com.google.android.gms.maps.model.LatLng

class Preferences(context: Context) {

    private var sharedPrefences: SharedPreferences = context
        .getSharedPreferences("2remember-prefs", Context.MODE_PRIVATE)

    fun setUser(user: User){
        val userJson = GsonConverter.getJson(user)
        sharedPrefences.edit().putString("User", userJson).apply()
    }

    fun getUser() : User?{
        val userJson = sharedPrefences.getString("User", null) ?: return null
        return GsonConverter.fromJson(userJson, User::class) as User?
    }

    fun setMacAddress(macaddress: String){
        sharedPrefences.edit().putString("MacAddress", macaddress).apply()
    }

    fun getMacAddress(): String? {
        return sharedPrefences.getString("MacAddress", null)
    }

    fun setLastPosition(position: CurrentPositionResponse){
        val posJson = GsonConverter.getJson(position)
        sharedPrefences.edit().putString("LastPosition", posJson).apply()
    }

    fun getLastPosition(): CurrentPositionResponse? {
        val posJson = sharedPrefences.getString("LastPosition", null) ?: return null
        return GsonConverter.fromJson(posJson, CurrentPositionResponse::class) as CurrentPositionResponse?
    }

    fun setSafePosition(position: LatLng){
        val posJson = GsonConverter.getJson(position)
        sharedPrefences.edit().putString("SafePosition", posJson).apply()
    }

    fun getSafePosition() : LatLng? {
        val posJson = sharedPrefences.getString("SafePosition", null) ?: return null
        return GsonConverter.fromJson(posJson, LatLng::class) as LatLng?
    }

    fun setRaio(raio: Int){
        sharedPrefences.edit().putInt("Raio", raio).apply()
    }

    fun getRaio() : Int{
        return sharedPrefences.getInt("Raio", 0)
    }

    fun setPanicButtonOn(on: Boolean) {
        sharedPrefences.edit().putBoolean("panicButton", on).apply()
    }

    fun isPanicButtonOn() : Boolean{
        return sharedPrefences.getBoolean("panicButton", false)
    }

    fun setDisconnectedBand(isDiconnected: Boolean){
        sharedPrefences.edit().putBoolean("disconnectedBand", isDiconnected).apply()
    }

    fun isDisconnectedBand() : Boolean {
        return sharedPrefences.getBoolean("disconnectedBand", false)
    }

    fun setBatteryLow(isLow: Boolean) {
        sharedPrefences.edit().putBoolean("lowBattery", isLow).apply()
    }

    fun isBatteryLow() : Boolean {
        return sharedPrefences.getBoolean("lowBattery", false)
    }

    fun setNome(nome: String){
        sharedPrefences.edit().putString("nome", nome).apply()
    }

    fun getNome() : String {
        return sharedPrefences.getString("nome", "")!!
    }

    fun setCelular(phone: String){
        sharedPrefences.edit().putString("celular", phone).apply()
    }

    fun getCelular() : String {
        return sharedPrefences.getString("celular", "")!!
    }
}