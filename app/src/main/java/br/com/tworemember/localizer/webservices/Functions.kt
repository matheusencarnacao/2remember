package br.com.tworemember.localizer.webservices

import br.com.tworemember.localizer.webservices.model.CurrentPositionRequest
import br.com.tworemember.localizer.webservices.model.CurrentPositionResponse
import br.com.tworemember.localizer.webservices.model.RegisterRequest
import br.com.tworemember.localizer.webservices.model.TokenRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface Functions {

    @POST("/newRegister")
    fun newRegister(@Body req: RegisterRequest) : Call<Void>

    @POST("/newToken")
    fun newToken(@Body token: TokenRequest) : Call<Void>

    @POST("/lastPosition")
    fun lastPosition(@Body positionRequest: CurrentPositionRequest) : Call<CurrentPositionResponse>
}