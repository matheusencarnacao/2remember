package br.com.tworemember.localizer.providers

import android.content.Context
import android.content.res.Resources
import android.util.Log
import br.com.tworemember.localizer.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions

class MapsUtils(private val map: GoogleMap) {

    fun styleMap(context: Context){
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context, R.raw.maps_style
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