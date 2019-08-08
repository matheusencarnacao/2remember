package br.com.tworemember.localizer.model

import java.util.*

class TrackedUser(
    val uuid: String,
    var name: String,
    var deviceId: String
) {
    var photoUrl: String? = null
    var birtday: Date? = null
}