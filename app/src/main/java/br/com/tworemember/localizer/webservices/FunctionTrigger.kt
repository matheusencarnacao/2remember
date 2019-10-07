package br.com.tworemember.localizer.webservices

import br.com.tworemember.localizer.webservices.model.CurrentPositionRequest
import br.com.tworemember.localizer.webservices.model.CurrentPositionResponse
import org.greenrobot.eventbus.EventBus
import retrofit2.Callback

class FunctionTrigger {

    companion object {
        fun callLastPositionFunction(macAddress: String) {
            EventBus.getDefault().post(LastLocationLoadingEvent(true))
            val functions = RetrofitClient.getInstance().create(Functions::class.java)
            val currentPositionRequest = CurrentPositionRequest(macAddress)
            val positionCall = functions.lastPosition(currentPositionRequest)

            positionCall.enqueue(object : Callback<CurrentPositionResponse> {
                override fun onFailure(call: retrofit2.Call<CurrentPositionResponse>, t: Throwable) {
                    EventBus.getDefault().post(LastLocationLoadingEvent(false))
                    EventBus.getDefault().post(LastLocationFailureEvent(t.localizedMessage))
                }

                override fun onResponse(
                    call: retrofit2.Call<CurrentPositionResponse>,
                    response: retrofit2.Response<CurrentPositionResponse>
                ) {
                    EventBus.getDefault().post(LastLocationLoadingEvent(false))
                    if (response.isSuccessful){
                        val position = response.body()
                        position?.let{ EventBus.getDefault().post(LastLocationSucessEvent(it)) }
                        return
                    }
                    EventBus.getDefault().post(LastLocationFailureEvent(response.message()))
                }

            })
        }
    }
}