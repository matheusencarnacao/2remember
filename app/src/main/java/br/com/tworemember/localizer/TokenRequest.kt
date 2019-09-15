package br.com.tworemember.localizer

import java.io.Serializable

data class TokenRequest (
    val userId: String,
    val token: String
) : Serializable