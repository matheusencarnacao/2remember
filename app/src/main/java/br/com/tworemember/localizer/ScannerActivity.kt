package br.com.tworemember.localizer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView

class ScannerActivity() : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private var scannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scannerView = ZXingScannerView(this)
        setContentView(scannerView)
    }

    override fun handleResult(result: Result) {
        scannerView!!.resumeCameraPreview(this)
        val bundle = Bundle()
        bundle.putString("value", result.text)
        val intent = Intent()
        intent.putExtras(bundle)
        setResult(Activity.RESULT_OK, intent)
    }

    override fun onResume() {
        super.onResume()
        scannerView!!.setResultHandler(this)
        scannerView!!.startCamera()
        scannerView!!.setAutoFocus(true)
        scannerView!!.setFormats(listOf(BarcodeFormat.QR_CODE))
    }

    override fun onPause() {
        super.onPause()
        scannerView!!.stopCamera()
    }
}
