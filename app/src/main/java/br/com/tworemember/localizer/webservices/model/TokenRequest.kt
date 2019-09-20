package br.com.tworemember.localizer.webservices.model

import java.io.Serializable

data class TokenRequest (
    val userId: String,
    val token: String
) : Serializable