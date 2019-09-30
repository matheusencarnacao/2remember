@file:Suppress("DEPRECATION")

package br.com.tworemember.localizer.activities

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.tworemember.localizer.R
import br.com.tworemember.localizer.providers.Preferences
import br.com.tworemember.localizer.providers.DialogProvider
import br.com.tworemember.localizer.webservices.Functions
import br.com.tworemember.localizer.webservices.RetrofitClient
import br.com.tworemember.localizer.webservices.model.CurrentPositionRequest
import br.com.tworemember.localizer.webservices.model.CurrentPositionResponse
import br.com.tworemember.localizer.webservices.model.RegisterRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_no_device_bonded.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private var loading: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        qr_scanner.setOnClickListener { scanQrCode() }

        safe_position.setOnClickListener {
            startActivity(
                Intent(
                    this@HomeActivity,
                    SafePlaceActivity::class.java
                )
            )
        }

        //TODO: iniciar serviço que irá realizar requisição.
    }

    override fun onResume() {
        super.onResume()
        verifyRegister()
    }

    private fun verifyRegister(){
        val prefs = Preferences(this)
        content_no_device.visibility = if (prefs.getMacAddress() == null) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun scanQrCode() {
        if (checkPermission()) {
            askPermission(arrayOf(Manifest.permission.CAMERA))
            return
        }
        startActivityForResult(
            Intent(
                this@HomeActivity, ScannerActivity::class.java
            ), 4
        )

    }

    private fun setLocationInMap(myLocation: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(myLocation).title("Device is here!"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f))
    }

    private fun askPermission(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, 100)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100) {
            if (permissions[0] == Manifest.permission.CAMERA) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    scanQrCode()
                else
                    Toast.makeText(
                        this,
                        getString(R.string.msg_camera_required),
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val macAddress = Preferences(this).getMacAddress()
        if (macAddress != null)
            callLastPositionFunction(macAddress)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 4 && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, "Erro ao ler QR Code.", Toast.LENGTH_LONG).show()
            } else {
                //TODO:Verificar se o macaddress é valido.
                val macaddress = data.getStringExtra("value")
                registerDevice(macaddress)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun registerDevice(macAddress: String) {
        loading = createLoading("Vinculando dispositivo...")
        val user = Preferences(this).getUser()
        if (user == null) {
            loading?.dismiss()
            Toast.makeText(
                this,
                "Usuário não encontrado, refaça o login por favor",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val req = RegisterRequest(user.uuid, macAddress)
        callRegisterFunction(req)
    }

    private fun createLoading(message: String): ProgressDialog {
        val dialog =
            DialogProvider.showProgressDialog(this, message)
        return dialog
    }

    private fun callRegisterFunction(req: RegisterRequest) {

        val functions = RetrofitClient.getInstance().create(Functions::class.java)
        val registerCall = functions.newRegister(req)

        registerCall.enqueue(object : Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(
                    this@HomeActivity,
                    "Erro ao vincular dispositivo",
                    Toast.LENGTH_SHORT
                ).show()
                loading?.dismiss()
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Dispositivo vinculado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    Preferences(this@HomeActivity).setMacAddress(req.macaddress)
                    verifyRegister()
                    callLastPositionFunction(req.macaddress)
                } else if (response.code() == 400) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Houve um problema ao encontrar o dispositivo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    fun callLastPositionFunction(macAddress: String) {
        loading?.dismiss()
        loading = createLoading("Localizando dispositivo, aguarde...")
        val functions = RetrofitClient.getInstance().create(Functions::class.java)
        val currentPositionRequest = CurrentPositionRequest(macAddress)
        val positionCall = functions.lastPosition(currentPositionRequest)

        positionCall.enqueue(object : Callback<CurrentPositionResponse> {
            override fun onFailure(call: Call<CurrentPositionResponse>, t: Throwable) {
                Toast.makeText(
                    this@HomeActivity, "Erro ao carregar localização do dispositivo",
                    Toast.LENGTH_SHORT
                ).show()
                loading?.dismiss()
            }

            override fun onResponse(
                call: Call<CurrentPositionResponse>,
                response: Response<CurrentPositionResponse>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(this@HomeActivity, "LOcalização atualizada", Toast.LENGTH_SHORT)
                        .show()
                    Log.d("Position", response.body()?.toString())
                    val position = response.body()
                    position?.let { setLocationInMap(LatLng(it.lat, it.lng)) }
                    loading?.dismiss()
                } else {
                    Toast.makeText(
                        this@HomeActivity, "Erro ao carregar localização do dispositivo",
                        Toast.LENGTH_SHORT
                    ).show()
                    loading?.dismiss()
                }
            }
        })
    }
}
