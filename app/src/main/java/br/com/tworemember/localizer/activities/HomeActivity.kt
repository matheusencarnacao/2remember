@file:Suppress("DEPRECATION")

package br.com.tworemember.localizer.activities

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.tworemember.localizer.R
import br.com.tworemember.localizer.providers.DialogProvider
import br.com.tworemember.localizer.providers.Preferences
import br.com.tworemember.localizer.webservices.*
import br.com.tworemember.localizer.webservices.model.RegisterRequest
import com.google.android.gms.location.places.GeoDataClient
import com.google.android.gms.location.places.PlaceDetectionClient
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_device_status.*
import kotlinx.android.synthetic.main.content_fab_home.*
import kotlinx.android.synthetic.main.content_no_device_bonded.*
import kotlinx.android.synthetic.main.custom_info_contents.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern


class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var loading: ProgressDialog? = null
    private var lastLocation: LatLng? = null
    private lateinit var geoDataClient: GeoDataClient
    private lateinit var placeDetectionClient: PlaceDetectionClient

    private val M_MAX_ENTRIES = 5
//    private String[] mLikelyPlaceNames;
//    private String[] mLikelyPlaceAddresses;
//    private String[] mLikelyPlaceAttributions;
//    private LatLng[] mLikelyPlaceLatLngs;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        geoDataClient = Places.getGeoDataClient(this, null)
        placeDetectionClient = Places.getPlaceDetectionClient(this, null)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        window.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                statusBarColor = Color.TRANSPARENT
            }
        }

        modalConfig()

        qr_scanner.setOnClickListener { scanQrCode() }

        fab_area_segura.setOnClickListener { goToSafePlace() }
        fab_logout.setOnClickListener { logout() }
        fab_settings.setOnClickListener { goToSettings() }
        fab_last_location.setOnClickListener { zoomToLastLocation() }

        LocationScheduler.startService(this)
    }

    override fun onResume() {
        super.onResume()
        verifyRegister()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    fun modalConfig() {
        val prefs = Preferences(this)
        modal_panic_btn.visibility = if (prefs.isPanicButtonOn()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        modal_disconnected.visibility = if (prefs.isDisconnectedBand()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        modal_low_battery.visibility = if (prefs.isBatteryLow()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        close_modal_panic_btn.setOnClickListener {
            prefs.setPanicButtonOn(false)
            modal_panic_btn.visibility = View.GONE
        }
        close_modal_disconnected.setOnClickListener {
            prefs.setDisconnectedBand(false)
            modal_disconnected.visibility = View.GONE
        }
        close_modal_low_battery.setOnClickListener {
            prefs.setBatteryLow(false)
            modal_low_battery.visibility = View.GONE
        }
    }

    private fun goToSettings() {
        //TODO: Criar activity de configuração
        Toast.makeText(this, "Em breve...", Toast.LENGTH_SHORT).show()
    }

    fun goToSafePlace() {
        val intent = Intent(this@HomeActivity, SafePlaceActivity::class.java)
        startActivity(intent)
    }

    fun logout() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut()

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun verifyRegister() {
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

    private fun showCurrentPlace() {
        val placeResult = placeDetectionClient.getCurrentPlace(null)
        placeResult.addOnCompleteListener {
            if (it.isSuccessful && it.result != null) {
                val likelyPlaces = it.result as PlaceLikelihoodBufferResponse

                val count = if (likelyPlaces.count < M_MAX_ENTRIES) {
                    likelyPlaces.count
                } else {
                    M_MAX_ENTRIES
                }

                val likelyPlacesName = ArrayList<String>(count)
                val likelyPlacesAddresses = ArrayList<String>(count)
                val likelyPlacesAttributtions = ArrayList<String?>(count)
                val likelyPlacesLatLngs = ArrayList<LatLng>(count)

                for (likelyPlace in likelyPlaces) {
                    likelyPlacesName.add(likelyPlace.place.name.toString())
                    likelyPlacesAddresses.add(likelyPlace.place.address.toString())
                    likelyPlacesAttributtions.add(likelyPlace.place.attributions.toString())
                    likelyPlacesLatLngs.add(likelyPlace.place.latLng)
                }

                likelyPlaces.release()

                //TODO: fazer alguma coisa com os lugares
            }
        }
    }

    private fun zoomToLastLocation() {
        lastLocation?.let { mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f)) }
    }

    private fun setLocationInMap(location: LatLng) {
        lastLocation = location
        val icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_blue)
        mMap?.clear()
        mMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title("Device is here!")
                .snippet("Olha o veio aqui")
                .icon(icon)
        )
        zoomToLastLocation()
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

        mMap?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoContents(marker: Marker?): View {
                val infoWindow = layoutInflater.inflate(
                    R.layout.custom_info_contents,
                    findViewById(R.id.map),
                    false
                )
                val title = infoWindow.findViewById<TextView>(R.id.title)
                title.text = marker?.title

                val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
                snippet.text = marker?.snippet

                return infoWindow
            }

            override fun getInfoWindow(p0: Marker?): View? {
                return null
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 4 && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, "Erro ao ler QR Code.", Toast.LENGTH_LONG).show()
            } else {
                val macaddress = data.getStringExtra("value")
                if (macValidate(macaddress)) {
                    registerDevice(macaddress)
                } else {
                    Toast.makeText(this, "Formato de MacAddress inválido", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun macValidate(mac: String): Boolean {
        val p = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
        val m = p.matcher(mac)
        return m.find()
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
        FunctionTrigger.callLastPositionFunction(macAddress)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocationSucess(event: LastLocationSucessEvent) {
        Toast.makeText(this@HomeActivity, "LOocalização atualizada", Toast.LENGTH_SHORT)
            .show()
        val position = event.location
        setLocationInMap(LatLng(position.lat, position.lng))
        loading?.dismiss()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLOcationFailed(event: LastLocationFailureEvent) {
        Log.d("${event.javaClass.simpleName}: ", event.errorMessage)
        Toast.makeText(
            this@HomeActivity, "Erro ao carregar localização do dispositivo",
            Toast.LENGTH_SHORT
        ).show()
        loading?.dismiss()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocationLoading(event: LastLocationLoadingEvent) {
        progress_bar.visibility = if (event.loading) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
}
