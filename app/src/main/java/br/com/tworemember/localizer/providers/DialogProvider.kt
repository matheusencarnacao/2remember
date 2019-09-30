package br.com.tworemember.localizer.providers

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface

class DialogProvider {

    companion object{
        fun showProgressDialog(context: Context, message: String, title: String? = null) : ProgressDialog{
            val dialog = ProgressDialog(context)
            dialog.setMessage(message)
            dialog.setCancelable(false)
            title?.let { dialog.setTitle(it) }
            dialog.show()

            return dialog
        }

        fun showAlertDialog(context: Context, message: String, title: String,
                            positiveClick: DialogInterface.OnClickListener,
                            negativeClick: DialogInterface.OnClickListener? = null) : AlertDialog {
            val dialog = AlertDialog.Builder(context)
            dialog.setTitle(title)
            dialog.setMessage(message)
            dialog.setPositiveButton("OK", positiveClick)
            negativeClick?.let { dialog.setNegativeButton("Cancelar", it) }
            return dialog.show()
        }

    }
}