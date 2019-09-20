package br.com.tworemember.localizer.webservices.model

import java.io.Serializable

data class RegisterRequest(
    val userId: String,
    val macaddress: String
) : Serializable