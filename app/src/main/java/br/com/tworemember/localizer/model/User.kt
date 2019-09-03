package br.com.tworemember.localizer.model

import java.util.*
import kotlin.collections.ArrayList

class User(
    val uuid: String,
    var name: String?,
    val email: String
) {
    var photoUrl: String? = null
    var birthday: Date? = null
    val devices: ArrayList<String> = ArrayList()
}