package com.project.bachao.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log

import androidx.core.content.ContextCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest as GoogleLocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationHelper(
    private val context: Context
) {

    companion object {
        private const val TAG = "BACHAO_LOCATION"

        private const val LOCATION_TIMEOUT = 15000L
    }

    private val fusedLocationClient:
            FusedLocationProviderClient =
        LocationServices
            .getFusedLocationProviderClient(context)


    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    )


    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        callback: (LocationData?) -> Unit
    ) {

        /*
         * Check location permission.
         */

        val fineGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED


        if (!fineGranted && !coarseGranted) {

            Log.e(
                TAG,
                "Location permission not granted"
            )

            callback(null)

            return
        }


        Log.d(
            TAG,
            "Requesting fresh GPS location..."
        )


        /*
         * First try the current location API.
         *
         * This does NOT depend on a previously cached
         * location.
         */

        val priority =
            if (fineGranted) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }


        val cancellationToken =
            com.google.android.gms.tasks.CancellationTokenSource()


        fusedLocationClient
            .getCurrentLocation(
                priority,
                cancellationToken.token
            )
            .addOnSuccessListener { location ->

                if (location != null) {

                    Log.d(
                        TAG,
                        "Fresh location received"
                    )

                    Log.d(
                        TAG,
                        "Latitude=${location.latitude}"
                    )

                    Log.d(
                        TAG,
                        "Longitude=${location.longitude}"
                    )

                    Log.d(
                        TAG,
                        "Accuracy=${location.accuracy}"
                    )


                    callback(
                        LocationData(
                            latitude =
                                location.latitude,

                            longitude =
                                location.longitude,

                            accuracy =
                                location.accuracy
                        )
                    )

                } else {

                    Log.w(
                        TAG,
                        "getCurrentLocation returned null"
                    )

                    /*
                     * Fallback to an active location update.
                     */

                    requestLocationUpdate(
                        priority,
                        callback
                    )
                }
            }
            .addOnFailureListener { exception ->

                Log.e(
                    TAG,
                    "getCurrentLocation failed",
                    exception
                )

                /*
                 * Fallback.
                 */

                requestLocationUpdate(
                    priority,
                    callback
                )
            }
    }


    @SuppressLint("MissingPermission")
    private fun requestLocationUpdate(
        priority: Int,
        callback: (LocationData?) -> Unit
    ) {

        Log.d(
            TAG,
            "Starting active location update..."
        )


        val request =
            GoogleLocationRequest
                .Builder(
                    priority,
                    1000L
                )
                .setMinUpdateIntervalMillis(
                    500L
                )
                .setMaxUpdateDelayMillis(
                    2000L
                )
                .setWaitForAccurateLocation(
                    false
                )
                .setDurationMillis(
                    LOCATION_TIMEOUT
                )
                .setMaxUpdates(
                    1
                )
                .build()


        var callbackCalled = false


        val locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    if (callbackCalled) {
                        return
                    }

                    callbackCalled = true


                    val location =
                        result.lastLocation


                    if (location != null) {

                        Log.d(
                            TAG,
                            "Active location received"
                        )

                        Log.d(
                            TAG,
                            "Latitude=${location.latitude}"
                        )

                        Log.d(
                            TAG,
                            "Longitude=${location.longitude}"
                        )

                        Log.d(
                            TAG,
                            "Accuracy=${location.accuracy}"
                        )


                        callback(
                            LocationData(
                                latitude =
                                    location.latitude,

                                longitude =
                                    location.longitude,

                                accuracy =
                                    location.accuracy
                            )
                        )

                    } else {

                        Log.e(
                            TAG,
                            "LocationResult contained no location"
                        )

                        callback(null)
                    }


                    fusedLocationClient
                        .removeLocationUpdates(this)
                }
            }


        fusedLocationClient
            .requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
            .addOnFailureListener { exception ->

                if (callbackCalled) {
                    return@addOnFailureListener
                }

                callbackCalled = true


                Log.e(
                    TAG,
                    "requestLocationUpdates failed",
                    exception
                )


                callback(null)
            }
    }
}