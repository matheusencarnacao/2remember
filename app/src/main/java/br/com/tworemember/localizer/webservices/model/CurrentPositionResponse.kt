package br.com.tworemember.localizer.webservices.model

import java.io.Serializable
import java.util.*

data class CurrentPositionResponse( val lat: Double, val lng: Double, val date: Date): Serializable