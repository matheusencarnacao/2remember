package br.com.tworemember.localizer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btn_bluetooth.setOnClickListener { startActivity(Intent(this@MapsActivity, BluetoothListActivity::class.java)) }
    }

    private fun checkPermission(permission: String) : Boolean{
        return ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
    }

    private fun getLocation(){
        if (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            askPermission()
            return
        }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, locationListener)
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(){
        val criteria = Criteria()
        val bestProvider = locationManager.getBestProvider(criteria, false)
        val location = locationManager.getLastKnownLocation(bestProvider)
        setLocationInMap(location)
    }

    private fun setLocationInMap(location: Location){
        val lat = try{ location.latitude } catch (e: NullPointerException) { -1.0 }
        val lon = try { location.longitude } catch (e: NullPointerException) { -1.0 }
        val myLocation = LatLng(lat, lon)
        mMap.clear()
        mMap.addMarker(MarkerOptions().position(myLocation).title("You are here!"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 15f))
    }

    private fun askPermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            100)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                getLocation()
            else
                Toast.makeText(this, getString(R.string.msg_location_required), Toast.LENGTH_SHORT).show()
        }
    }

    private val locationListener = object: LocationListener {
        override fun onLocationChanged(location: Location?) {
            if(location != null)
                setLocationInMap(location)
            else
                getLastLocation()
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String?) {}

        override fun onProviderDisabled(provider: String?) {
            Toast.makeText(this@MapsActivity, "O recurso de GPS está desabilitado neste aparelho", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        getLocation()
    }
}
