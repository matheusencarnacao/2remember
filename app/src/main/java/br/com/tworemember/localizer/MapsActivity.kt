package br.com.tworemember.localizer

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_maps.*
import org.json.JSONException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private val qrScan = IntentIntegrator(this)
    private val functions = FirebaseFunctions.getInstance()
    private var loading: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btn_bluetooth.setOnClickListener {
            startActivity(
                Intent(
                    this@MapsActivity,
                    BluetoothListActivity::class.java
                )
            )
        }

        qr_scanner.setOnClickListener { qrScan.initiateScan() }
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun getLocation() {
        if (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        ) {
            askPermission()
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
        setLocationInMap(location)
    }

    private fun setLocationInMap(location: Location) {
        val lat = try {
            location.latitude
        } catch (e: NullPointerException) {
            -1.0
        }
        val lon = try {
            location.longitude
        } catch (e: NullPointerException) {
            -1.0
        }
        val myLocation = LatLng(lat, lon)
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(myLocation).title("You are here!"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f))
    }

    private fun askPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100) {
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

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location?) {
            if (location != null)
                setLocationInMap(location)
            else
                getLastLocation()
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String?) {}

        override fun onProviderDisabled(provider: String?) {
            Toast.makeText(
                this@MapsActivity,
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

        getLocation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_LONG).show()
            } else {
                try {
                    val macaddress = result.contents
                    registerDevice(macaddress)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, result.contents, Toast.LENGTH_LONG).show()
                }

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun registerDevice(macAddress: String) {
        loading = createLoading()
        val user = Preferences(this).getUser()
        if (user == null){
            loading?.dismiss()
            Toast.makeText(this, "Usuário não encontrado, refaça o login por favor", Toast.LENGTH_SHORT).show()
            return
        }

        val req = RegisterRequest(user.uuid, macAddress)
        callRegisterFunction(req)
    }

    fun createLoading() : ProgressDialog {
        val dialog = ProgressDialogProvider.showProgressDialog(this, "Vinculando dispositivo...")
        return dialog
    }

    fun callRegisterFunction(req: RegisterRequest){
        functions.getHttpsCallable("newRegister")
            .call(req)
            .addOnCompleteListener {
                if (!it.isSuccessful){
                    Toast.makeText(this, "Erro ao vincular dispositivo", Toast.LENGTH_SHORT).show()
                    loading?.dismiss()
                    return@addOnCompleteListener
                }

                Toast.makeText(this, "Dispositivo vinculado com sucesso!", Toast.LENGTH_SHORT).show()
                Preferences(this).setMacAddress(req.macaddress)
                callLastLocationFunction(req.macaddress)
            }
    }

    fun callLastLocationFunction(macAddress: String){
        val currentPositionRequest = CurrentPositionRequest(macAddress)

        functions.getHttpsCallable("lastPosition")
            .call(currentPositionRequest)
            .addOnCompleteListener {
                if(!it.isSuccessful){
                    Toast.makeText(this, "Erro ao carregar localização do dispositivo",
                        Toast.LENGTH_SHORT).show()
                    loading?.dismiss()
                    return@addOnCompleteListener
                }

                Toast.makeText(this, "LOcalização atualizada", Toast.LENGTH_SHORT).show()
                Log.d("Position", it.result?.data.toString())
            }
    }
}
