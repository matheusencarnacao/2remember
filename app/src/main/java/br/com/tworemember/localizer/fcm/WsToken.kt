package br.com.tworemember.localizer.fcm

import android.content.Context
import android.util.Log
import br.com.tworemember.localizer.model.User
import br.com.tworemember.localizer.providers.Preferences
import br.com.tworemember.localizer.webservices.Functions
import br.com.tworemember.localizer.webservices.RetrofitClient
import br.com.tworemember.localizer.webservices.model.TokenRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WsToken(private val context: Context) {

    fun sendTokenToFunction(token: String){
        val user = Preferences(context).getUser()
        user?.let { callTokenFunction(token, it) }
    }

    private fun callTokenFunction(token: String, user: User){
        val functions = RetrofitClient.getInstance().create(Functions::class.java)
        val body = TokenRequest(user.uuid, token)

        val tokenCall = functions.newToken(body)
        tokenCall.enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e("Token: ", "Erro ao enviar o token: E: ${t.message}")
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                Log.d("Token: ", "token enviado com sucesso!")
            }

        })

    }
}