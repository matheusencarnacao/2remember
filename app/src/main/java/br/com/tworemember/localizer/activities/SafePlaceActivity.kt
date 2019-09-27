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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.tworemember.localizer.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class SafePlaceActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_place)

        //TODO: implementar circulo no app
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000,
            5f,
            locationListener
        )
    }
    private fun setLocationInMap(myLocation: LatLng) {
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(myLocation).title("Device is here!"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f))
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        val criteria = Criteria()
        val bestProvider = locationManager.getBestProvider(criteria, false)
        val location = locationManager.getLastKnownLocation(bestProvider)
        setLocationInMap(LatLng(location.altitude, location.longitude))
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