package br.com.tworemember.localizer.extensions

import android.util.Base64

fun String.toBase64() : String{
    return Base64.encodeToString(this.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
}

fun String.fromBase64() : String {
    return String(Base64.decode(this.toByteArray(), Base64.NO_WRAP),  Charsets.UTF_8)
}