@file:Suppress("DEPRECATION")

package br.com.tworemember.localizer.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.tworemember.localizer.providers.Preferences
import br.com.tworemember.localizer.providers.ProgressDialogProvider
import br.com.tworemember.localizer.R
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

        btn_bluetooth.setOnClickListener {
            startActivity(
                Intent(this@HomeActivity, BluetoothListActivity::class.java)
            )
        }

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

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun scanQrCode() {
        if (checkPermission(Manifest.permission.CAMERA)) {
            askPermission(arrayOf(Manifest.permission.CAMERA))
            return
        }
        startActivityForResult(
            Intent(
                this@HomeActivity, ScannerActivity::class.java
            ), 4
        )

    }

    private fun getLocation() {
        if (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            askPermission(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            return
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000,
            5f,
            locationListener
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val criteria = Criteria()
        val bestProvider = locationManager.getBestProvider(criteria, false)
        val location = locationManager.getLastKnownLocation(bestProvider)
        setLocationInMap(getLatLntFromLocation(location))
    }

    private fun getLatLntFromLocation(location: Location): LatLng {
        val lat = try {
            location.latitude
        } catch (e: NullPointerException) {
            -1.0
        }
        val lng = try {
            location.longitude
        } catch (e: NullPointerException) {
            -1.0
        }

        return LatLng(lat, lng)
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
            } else {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    getLocation()
                else
                    Toast.makeText(
                        this,
                        getString(R.string.msg_location_required),
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            if (location != null)
                setLocationInMap(getLatLntFromLocation(location))
            else
                getLastLocation()
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String?) {}

        override fun onProviderDisabled(provider: String?) {
            Toast.makeText(
                this@HomeActivity,
                "O recurso de GPS está desabilitado neste aparelho",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
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

    fun registerDevice(macAddress: String) {
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

    fun createLoading(message: String): ProgressDialog {
        val dialog =
            ProgressDialogProvider.showProgressDialog(this, message)
        return dialog
    }

    fun callRegisterFunction(req: RegisterRequest) {

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
