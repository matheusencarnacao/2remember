@file:Suppress("DEPRECATION")

package br.com.tworemember.localizer.activities

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
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
import com.facebook.login.LoginManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.content_device_status.*
import kotlinx.android.synthetic.main.content_fab_home.*
import kotlinx.android.synthetic.main.content_no_device_bonded.*
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
    private lateinit var resultReceiver: AddressResultReceiver
    private var marker: Marker? = null
    private var circle: Circle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

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
        sair.setOnClickListener { logout() }

        fab_area_segura.setOnClickListener { goToSafePlace() }
        fab_logout.setOnClickListener { showLogoutDialog() }
        fab_settings.setOnClickListener { goToSettings() }
        fab_last_location.setOnClickListener { zoomToLastLocation() }

        LocationScheduler.startService(this)
    }

    override fun onResume() {
        super.onResume()
        verifyRegister()
        verifyZoomButton()
        verifyRadius()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun modalConfig() {
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
        startActivity(
            Intent(this, ConfiguracoesActivity::class.java)
        )
    }

    private fun goToSafePlace() {
        val intent = Intent(this@HomeActivity, SafePlaceActivity::class.java)
        startActivity(intent)
    }

    private fun showLogoutDialog() {
        DialogProvider.showAlertDialog(this,
            "Deseja realizar logout?", "Logout",
            DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
                logout()
            }).show()
    }

    fun logout() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut()
        val faceAuth = LoginManager.getInstance()
        faceAuth.logOut()

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

    private fun verifyZoomButton(){
        fab_last_location.visibility = if (lastLocation == null) {
            View.GONE
        } else {
            View.VISIBLE
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


    private fun zoomToLastLocation() {
        lastLocation?.let { mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f)) }
    }

    private fun setLocationInMap(location: LatLng) {
        lastLocation = location
        val icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_person_pin_circle)
        mMap?.clear()

        val prefs = Preferences(this)
        val nome = if (prefs.getNome().isEmpty()) {
            "Pulseira"
        } else {
            prefs.getNome()
        }

        verifyRadius()
        verifyZoomButton()

        marker = mMap?.addMarker(
            MarkerOptions()
                .position(location)
                .title(nome)
                .icon(icon)
        )
        zoomToLastLocation()
    }

    private fun verifyRadius(){
        val prefs = Preferences(this)
        if(prefs.getRaio() > 0 && prefs.getSafePosition() != null){
            if(circle == null){
                circle = mMap?.addCircle(
                    CircleOptions()
                        .center(prefs.getSafePosition())
                        .radius(prefs.getRaio().toDouble())
                        .strokeWidth(0f)
                        .fillColor(0x330000FF))
            } else {
                circle?.radius = prefs.getRaio().toDouble()
            }
        }
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

        mMap?.let {


            it.setInfoWindowAdapter(object: GoogleMap.InfoWindowAdapter {
                override fun getInfoContents(p0: Marker?): View {
                    val info = LinearLayout(this@HomeActivity)
                    info.setOrientation(LinearLayout.VERTICAL)

                    //TODO: melhorar esse window

                    val title = TextView(this@HomeActivity).also { t ->
                        t.setTextColor(Color.BLACK)
                        t.gravity = Gravity.CENTER
                        t.setTypeface(null, Typeface.BOLD)
                        t.text = marker?.title
                    }


                    val snippet = TextView(this@HomeActivity).also {s ->
                        s.setTextColor(Color.GRAY)
                        s.text = marker?.snippet
                    }

                    with(info) {
                        addView(title)
                        addView(snippet)
                    }

                    return info
                }

                override fun getInfoWindow(p0: Marker?): View? {
                    return null
                }

            })

            try {
                val success = it.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.maps_style
                    )
                )

                if (!success) {
                    Log.e("HomeActivity", "Style parsing failed.")
                } else {
                    Log.d("HomeActivity", "Map stylef")
                }
            } catch (e: Resources.NotFoundException) {
                Log.e("HomeActivity", "Can't find style. Error: ", e)
            }
        }
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

    private fun macValidate(mac: String): Boolean {
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
        return DialogProvider.showProgressDialog(this, message)
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
                loading?.dismiss()
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
        val location = LatLng(position.lat, position.lng)
        setLocationInMap(location)
        getLoaltionDetails(location)
        loading?.dismiss()
        verifyZoomButton()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLOcationFailed(event: LastLocationFailureEvent) {
        Log.d("${event.javaClass.simpleName}: ", event.errorMessage)
        if (event.errorMessage == "404") {
            Toast.makeText(this@HomeActivity, "Nenhuma localização encontrada", Toast.LENGTH_LONG)
                .show()
        } else {
            Toast.makeText(
                this@HomeActivity, "Erro ao carregar localização do dispositivo",
                Toast.LENGTH_SHORT
            ).show()
        }
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

    private fun getLoaltionDetails(location: LatLng) {
        resultReceiver = AddressResultReceiver(Handler())
        val intent = Intent(this, GeocoderService::class.java).apply {
            putExtra(GeocoderService.Constants.RECEIVER, resultReceiver)
            putExtra(GeocoderService.Constants.LOCATION_DATA_EXTRA, location)
        }
        startService(intent)
    }

    internal inner class AddressResultReceiver(handler: Handler) : ResultReceiver(handler) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            val addressOutput =
                resultData?.getString(GeocoderService.Constants.RESULT_DATA_KEY) ?: ""
            marker?.snippet = addressOutput
        }
    }
}
