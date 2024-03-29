package br.com.tworemember.localizer.activities

import android.Manifest
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.tworemember.localizer.R
import br.com.tworemember.localizer.providers.DialogProvider
import br.com.tworemember.localizer.providers.MapsUtils
import br.com.tworemember.localizer.providers.Preferences
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.activity_safe_place.*
import kotlinx.android.synthetic.main.custom_toolbar.*


class SafePlaceActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private var myPosition : LatLng? = null
    private var circle: Circle? = null
    private var marker: Marker? = null
    private var radiusSafe = 0
    private var item: MenuItem? = null
    private var dialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_place)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white)
        }
        title = "Area segura"

        dialog = DialogProvider.showProgressDialog(this, "Carregando, aguarde...")

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tv_title.visibility = View.GONE
        seekBar.visibility = View.GONE
        seekBar.onSeekChangeListener = object: OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams?) {
                seekParams?.let {
                    val radius = it.progress
                    radiusSafe = radius
                    setRadius(radius.toDouble())
                }
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar?) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar?) {}
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.let {
            item = it.findItem(R.id.item_salvar)
            updateMenuItem()
        }
        return super.onPrepareOptionsMenu(menu)
    }

    private fun updateMenuItem(){ item?.isVisible = myPosition != null }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_safe_position, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let {
            when {
                it.itemId == R.id.item_salvar -> saveConfig()
                it.itemId == android.R.id.home -> onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveConfig(){
        DialogProvider.showAlertDialog(this,
            "Confirmar as informações carregadas",
            "Confirmar",
            DialogInterface.OnClickListener { dialog, _ ->
                myPosition?.let {
                    val prefs = Preferences(this@SafePlaceActivity)
                    prefs.setRaio(radiusSafe)
                    prefs.setSafePosition(it)
                    startActivity(Intent(this@SafePlaceActivity,
                        BluetoothListActivity::class.java))
                    finish()
                }
            },
            DialogInterface.OnClickListener { dialog, _ -> dialog.dismiss() })
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun askPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ), 100)
    }

    private fun getLocation() {
        if (checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            askPermission()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                location?.let {
                    setLocationInMap(LatLng(it.latitude, it.longitude))
                }
            }
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
            circle?.radius = radius
        }
    }

    private fun setLocationInMap(myLocation: LatLng) {
        myPosition = myLocation
        updateMenuItem()

        dialog?.dismiss()
        seekBar.visibility = View.VISIBLE
        tv_title.visibility = View.VISIBLE
        if (marker == null){
            marker = mMap.addMarker(MarkerOptions()
                .position(myLocation)
                .title("Você esta aqui!")
                .snippet("Esta será o centro da area segura."))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16f))
        } else {
            marker?.position = myLocation
        }
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

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        mMap.uiSettings.isMapToolbarEnabled = false
        getLocation()
        val utils = MapsUtils(mMap)
        utils.styleMap(this)
    }
}