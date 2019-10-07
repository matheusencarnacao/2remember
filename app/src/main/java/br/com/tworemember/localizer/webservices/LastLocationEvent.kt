package br.com.tworemember.localizer.webservices

import br.com.tworemember.localizer.webservices.model.CurrentPositionResponse

data class LastLocationSucessEvent(val location: CurrentPositionResponse)

data class LastLocationFailureEvent(val errorMessage: String)

data class LastLocationLoadingEvent(val loading: Boolean)