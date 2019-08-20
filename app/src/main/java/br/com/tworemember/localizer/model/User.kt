package br.com.tworemember.localizer.model

import java.util.*

class User(
    val uuid: String,
    var name: String?,
    val email: String
) {
    var photoUrl: String? = null
    var birthday: Date? = null
}