package br.com.tworemember.localizer

import android.app.ProgressDialog
import android.content.Context

class ProgressDialogProvider {

    companion object{
        fun showProgressDialog(context: Context, message: String, title: String? = null) : ProgressDialog{
            val dialog = ProgressDialog(context)
            dialog.setMessage(message)
            title?.let { dialog.setTitle(it) }
            dialog.show()

            return dialog
        }
    }
}