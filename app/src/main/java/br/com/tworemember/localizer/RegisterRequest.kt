package br.com.tworemember.localizer

import java.io.Serializable

data class RegisterRequest(
    val userId: String,
    val macaddress: String
) : Serializable