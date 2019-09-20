package br.com.tworemember.localizer.webservices

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient {

    companion object {
        private var retrofit: Retrofit? = null
        private var url_base = "https://us-central1-remember-4c39b.cloudfunctions.net"

        fun getInstance() : Retrofit {
            if (retrofit == null){
                retrofit = Retrofit.Builder()
                    .baseUrl(url_base)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit!!
        }

    }
}