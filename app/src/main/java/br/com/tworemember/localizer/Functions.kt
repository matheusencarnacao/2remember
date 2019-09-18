package br.com.tworemember.localizer

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface Functions {

    @POST("/newResgiter")
    fun newRegister(@Body req: RegisterRequest) : Call<Void>

    @POST("/newToken")
    fun newToken(@Body token: TokenRequest) : Call<Void>

    @POST("/lastLocation")
    fun lastLocation(@Body positionRequest: CurrentPositionRequest) : Call<CurrentPositionResponse>
}