package br.com.tworemember.localizer.webservices.model

import java.io.Serializable

data class CurrentPositionRequest( val macadress: String ) : Serializable

data class CurrentPositionResponse( val lat: Double, val lng: Double): Serializable