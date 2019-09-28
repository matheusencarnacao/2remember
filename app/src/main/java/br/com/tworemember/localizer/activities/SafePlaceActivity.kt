package br.com.tworemember.localizer.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.tworemember.localizer.R
import br.com.tworemember.localizer.providers.Preferences
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_safe_place.*


class SafePlaceActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager
    private var myPosition : LatLng? = null
    private var circle: Circle? = null
    private var marker: Marker? = null
    private var radiusSafe = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_place)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //TODO: implementar circulo no app

        seekBar.max = 20
        seekBar.visibility = View.GONE
        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val radius = progress * 25
                radiusSafe = radius
                setRadius(radius.toDouble())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

        })

        salvar.setOnClickListener {
            Preferences(this@SafePlaceActivity).setRaio(radiusSafe)
            //TODO: implementar salvar localização e enviar para o bluetooth
        }
    }


    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun askPermission(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, 100)
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

    private fun setRadius(radius: Double){
        if(circle == null){
            circle = mMap.addCircle(
                CircleOptions()
                    .center(myPosition)
                    .radius(radius)
                    .strokeWidth(0f)
                    .fillColor(0x330000FF))
        } else {
            circle?.radius = radius.toDouble()
        }
    }

    private fun setLocationInMap(myLocation: LatLng) {
        myPosition = myLocation
        seekBar.visibility = View.VISIBLE
        if (marker == null){
            marker = mMap.addMarker(MarkerOptions().position(myLocation).title("Device is here!"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16f))
        } else {
            marker?.position = myLocation
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val criteria = Criteria()
        val bestProvider = locationManager.getBestProvider(criteria, false)
        val location = locationManager.getLastKnownLocation(bestProvider)
        setLocationInMap(LatLng(location.altitude, location.longitude))
        //todo: pegar do spinner
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
                setLocationInMap(LatLng(location.latitude, location.longitude))
            else
                getLastLocation()
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String?) {}

        override fun onProviderDisabled(provider: String?) {
            Toast.makeText(
                this@SafePlaceActivity,
                "O recurso de GPS está desabilitado neste aparelho",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        getLocation()
    }
}