package br.com.tworemember.localizer.webservices

import android.app.IntentService
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import br.com.tworemember.localizer.R
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.util.*

class GeocoderService : IntentService(GeocoderService::class.java.simpleName) {

    private var receiver: ResultReceiver?  = null
    private val tag = GeocoderService::class.java.simpleName

    object Constants {
        const val SUCCESS_RESULT = 0
        const val FAILURE_RESULT = 1
        const val PACKAGE_NAME = "br.com.tworemember.localizer"
        const val RECEIVER = "$PACKAGE_NAME.RECEIVER"
        const val RESULT_DATA_KEY = "${PACKAGE_NAME}.RESULT_DATA_KEY"
        const val LOCATION_DATA_EXTRA = "${PACKAGE_NAME}.LOCATION_DATA_EXTRA"
    }

    override fun onHandleIntent(intent: Intent?) {
        intent ?: return

        var errorMessage = ""

        //Get the location passed to this service trough an extra
        val location = intent.getParcelableExtra<LatLng>(Constants.LOCATION_DATA_EXTRA)
        receiver = intent.getParcelableExtra(Constants.RECEIVER)

        val geocoder = Geocoder(this, Locale.getDefault())

        var addresses: List<Address> = emptyList()

        try {
            addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        } catch (ioException: IOException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available)
            Log.e(tag, errorMessage, ioException)
        } catch (illegalArgumentException: IllegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used)
            Log.e(tag, "$errorMessage. Latitude = $location.latitude , " +
                    "Longitude =  $location.longitude", illegalArgumentException)
        }

        // Handle case where no address was found.
        if (addresses.isEmpty()) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found)
                Log.e(tag, errorMessage)
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage)
        } else {
            val address = addresses[0]
            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            val addressFragments = with(address) {
                (0..maxAddressLineIndex).map { getAddressLine(it) }
            }
            Log.i(tag, getString(R.string.address_found))
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                addressFragments.joinToString(separator = "\n"))
        }

    }

    private fun deliverResultToReceiver(resultCode: Int, message: String){
        val bundle = Bundle().apply { putString(Constants.RESULT_DATA_KEY, message) }
        receiver?.send(resultCode, bundle)
    }
}