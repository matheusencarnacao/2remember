package br.com.tworemember.localizer.activities

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.tworemember.localizer.R
import br.com.tworemember.localizer.providers.DialogProvider
import br.com.tworemember.localizer.providers.Mask
import br.com.tworemember.localizer.providers.Preferences
import kotlinx.android.synthetic.main.activity_configuracao.*

class ConfiguracoesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracao)

        title = getString(R.string.configuracoes)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        }

        edt_tel.addTextChangedListener(Mask.mask("(##) #####-####", edt_tel))

        btn_salvar.setOnClickListener { showConfirmaDIalog() }

        val prefs = Preferences(this)
        edt_tel.setText(prefs.getCelular())
        edt_nome.setText(prefs.getNome())
    }

    private fun salvar() {
        val prefs = Preferences(this)
        val nome = edt_nome.text.toString()
        prefs.setNome(nome)

        val phone = edt_tel.text.toString()
        prefs.setCelular(phone)

        finish()
    }

    private fun showConfirmaDIalog(){
        DialogProvider.showAlertDialog(this,
            "Deseja confirmar as informações?",
            "Confirmar",
            DialogInterface.OnClickListener { dialog, which ->
                salvar()
                dialog.dismiss()
            }).show()
    }
}