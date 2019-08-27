package br.com.tworemember.localizer

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class ConfiguracaoRaio(
    @SerializedName("Lat") val latitude: Double,
    @SerializedName("Lon") val longitude: Double,
    @SerializedName("Raio") val raio: Int
) : Serializable