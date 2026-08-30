package com.project.bachao.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log

import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

import com.project.bachao.MainActivity
import com.project.bachao.R

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import java.net.HttpURLConnection
import java.net.URL


class EmergencyService : Service() {

    companion object {

        private const val TAG = "BACHAO_SERVICE"

        private const val CHANNEL_ID =
            "bachao_protection"

        private const val NOTIFICATION_ID =
            1001

        /*
         * IMPORTANT:
         *
         * This must be your PC's IPv4 address.
         *
         * Example:
         *
         * http://192.168.31.200:3000
         *
         * Do NOT use localhost.
         */
        private const val SERVER_URL =
            "http://192.168.31.200:3000"

        private const val ALERT_ENDPOINT =
            "$SERVER_URL/api/alerts"
    }


    /*
     * Coroutine scope for network operations.
     */
    private val serviceScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.IO
        )


    /*
     * Sensors
     */
    private lateinit var sensorManager: SensorManager

    private lateinit var shakeDetector: ShakeDetector


    /*
     * Location
     */
    private lateinit var fusedLocationClient:
            FusedLocationProviderClient


    /*
     * Prevent duplicate requests.
     */
    @Volatile
    private var alertInProgress = false


    /*
     * Prevent multiple active emergencies.
     */
    @Volatile
    private var emergencyActive = false


    override fun onCreate() {

        super.onCreate()

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "EmergencyService CREATED"
        )

        Log.d(
            TAG,
            "================================"
        )


        /*
         * Notification channel.
         */
        createNotificationChannel()


        /*
         * Sensor manager.
         */
        sensorManager =
            getSystemService(
                Context.SENSOR_SERVICE
            ) as SensorManager


        /*
         * Fused Location Provider.
         *
         * This is the important change.
         */
        fusedLocationClient =
            LocationServices
                .getFusedLocationProviderClient(
                    this
                )


        /*
         * Shake detector.
         */
        shakeDetector =
            ShakeDetector(
                sensorManager
            ) {

                Log.d(
                    TAG,
                    "SHAKE CALLBACK RECEIVED"
                )

                handleShake()
            }
    }


    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        Log.d(
            TAG,
            "EmergencyService STARTED"
        )


        /*
         * Start foreground notification.
         */
        startProtectionNotification()


        /*
         * Start shake detector.
         */
        shakeDetector.start()


        /*
         * If Android kills the service,
         * request it to recreate it.
         */
        return START_STICKY
    }


    /*
     * ========================================
     * SHAKE HANDLER
     * ========================================
     */

    private fun handleShake() {

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "HANDLE SHAKE"
        )

        Log.d(
            TAG,
            "alertInProgress=$alertInProgress"
        )

        Log.d(
            TAG,
            "emergencyActive=$emergencyActive"
        )

        Log.d(
            TAG,
            "================================"
        )


        /*
         * Don't send another request while
         * an alert is already being processed.
         */
        if (alertInProgress) {

            Log.d(
                TAG,
                "Alert request already in progress"
            )

            return
        }


        /*
         * Don't create duplicate alerts.
         */
        if (emergencyActive) {

            Log.d(
                TAG,
                "Emergency already active"
            )

            return
        }


        /*
         * Lock immediately.
         */
        alertInProgress = true


        /*
         * ====================================
         * READ USER DATA
         * ====================================
         *
         * These keys must match the keys used
         * when MainActivity saves the user.
         */
        val preferences =
            getSharedPreferences(
                "bachao_prefs",
                Context.MODE_PRIVATE
            )


        val userId =
            preferences.getInt(
                "user_id",
                -1
            )


        val name =
            preferences.getString(
                "user_name",
                ""
            ) ?: ""


        val phone =
            preferences.getString(
                "user_phone",
                ""
            ) ?: ""


        val email =
            preferences.getString(
                "user_email",
                ""
            ) ?: ""


        Log.d(
            TAG,
            "User ID=$userId"
        )

        Log.d(
            TAG,
            "User name=$name"
        )

        Log.d(
            TAG,
            "User phone=$phone"
        )

        Log.d(
            TAG,
            "User email=$email"
        )


        /*
         * User doesn't exist locally.
         */
        if (userId == -1) {

            Log.e(
                TAG,
                "NO USER ID FOUND"
            )

            alertInProgress = false

            return
        }


        /*
         * ====================================
         * GET FRESH LOCATION
         * ====================================
         */
        getCurrentLocation(

            onSuccess = { location ->

                Log.d(
                    TAG,
                    "================================"
                )

                Log.d(
                    TAG,
                    "LOCATION RECEIVED"
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

                Log.d(
                    TAG,
                    "================================"
                )


                /*
                 * Send network request on
                 * background thread.
                 */
                serviceScope.launch {

                    sendEmergencyAlert(

                        userId =
                            userId,

                        name =
                            name,

                        phone =
                            phone,

                        email =
                            email,

                        latitude =
                            location.latitude,

                        longitude =
                            location.longitude,

                        accuracy =
                            location.accuracy
                    )
                }
            },

            onFailure = {

                Log.e(
                    TAG,
                    "Could not get current location"
                )

                /*
                 * Allow another shake attempt.
                 */
                alertInProgress = false
            }
        )
    }


    /*
     * ========================================
     * GET CURRENT LOCATION
     * ========================================
     *
     * IMPORTANT:
     *
     * We do NOT use:
     *
     * getLastKnownLocation()
     *
     * because it can return null.
     *
     * Instead we request a fresh location.
     */

    private fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onFailure: () -> Unit
    ) {

        /*
         * Check permission.
         */
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&

            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Log.e(
                TAG,
                "Location permission not granted"
            )

            onFailure()

            return
        }


        Log.d(
            TAG,
            "Requesting fresh location..."
        )


        /*
         * Determine accuracy.
         */
        val priority =

            if (
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                Priority.PRIORITY_HIGH_ACCURACY

            } else {

                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }


        /*
         * First try getCurrentLocation().
         *
         * Unlike lastLocation, this asks Google
         * Play Services for a current location.
         */
        fusedLocationClient
            .getCurrentLocation(
                priority,
                null
            )
            .addOnSuccessListener { location ->

                if (location != null) {

                    Log.d(
                        TAG,
                        "Fresh location received"
                    )

                    onSuccess(location)

                } else {

                    Log.w(
                        TAG,
                        "getCurrentLocation returned NULL"
                    )

                    /*
                     * Try active location updates.
                     */
                    requestActiveLocation(
                        priority,
                        onSuccess,
                        onFailure
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
                 * Try active location updates.
                 */
                requestActiveLocation(
                    priority,
                    onSuccess,
                    onFailure
                )
            }
    }


    /*
     * ========================================
     * ACTIVE LOCATION FALLBACK
     * ========================================
     */

    private fun requestActiveLocation(
        priority: Int,
        onSuccess: (Location) -> Unit,
        onFailure: () -> Unit
    ) {

        Log.d(
            TAG,
            "Starting active location updates..."
        )


        /*
         * Request location every second.
         */
        val locationRequest =
            LocationRequest.Builder(
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
                .setMaxUpdates(
                    1
                )
                .build()


        var callbackCalled =
            false


        val locationCallback =
            object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    if (callbackCalled) {
                        return
                    }


                    callbackCalled =
                        true


                    val location =
                        result.lastLocation


                    /*
                     * Stop requesting more locations.
                     */
                    fusedLocationClient
                        .removeLocationUpdates(
                            this
                        )


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


                        onSuccess(location)

                    } else {

                        Log.e(
                            TAG,
                            "LocationResult has no location"
                        )

                        onFailure()
                    }
                }
            }


        fusedLocationClient
            .requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            .addOnFailureListener { exception ->

                if (callbackCalled) {
                    return@addOnFailureListener
                }


                callbackCalled =
                    true


                Log.e(
                    TAG,
                    "requestLocationUpdates failed",
                    exception
                )


                onFailure()
            }
    }


    /*
     * ========================================
     * SEND ALERT TO NODE.JS
     * ========================================
     */

    private fun sendEmergencyAlert(
        userId: Int,
        name: String,
        phone: String,
        email: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float
    ) {

        var connection:
                HttpURLConnection? = null


        try {

            Log.d(
                TAG,
                "================================"
            )

            Log.d(
                TAG,
                "SENDING EMERGENCY ALERT"
            )

            Log.d(
                TAG,
                "URL=$ALERT_ENDPOINT"
            )

            Log.d(
                TAG,
                "================================"
            )


            /*
             * Create URL.
             */
            val url =
                URL(ALERT_ENDPOINT)


            /*
             * Open HTTP connection.
             */
            connection =
                url.openConnection()
                        as HttpURLConnection


            /*
             * POST.
             */
            connection.requestMethod =
                "POST"


            /*
             * Timeouts.
             */
            connection.connectTimeout =
                10000

            connection.readTimeout =
                10000


            /*
             * We are sending data.
             */
            connection.doOutput =
                true


            /*
             * JSON content.
             */
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )


            connection.setRequestProperty(
                "Accept",
                "application/json"
            )


            /*
             * =================================
             * JSON
             * =================================
             */
            val json =
                """
                {
                    "user_id": $userId,
                    "name": "${escapeJson(name)}",
                    "phone": "${escapeJson(phone)}",
                    "email": "${escapeJson(email)}",
                    "triggerType": "SHAKE",
                    "latitude": $latitude,
                    "longitude": $longitude,
                    "accuracy": $accuracy
                }
                """.trimIndent()


            Log.d(
                TAG,
                "Request JSON=$json"
            )


            /*
             * Send JSON.
             */
            connection
                .outputStream
                .use { output ->

                    output.write(
                        json.toByteArray(
                            Charsets.UTF_8
                        )
                    )

                    output.flush()
                }


            /*
             * Get HTTP response.
             */
            val responseCode =
                connection.responseCode


            Log.d(
                TAG,
                "Server response code=$responseCode"
            )


            /*
             * Read server response.
             */
            val responseText =

                try {

                    if (
                        responseCode in
                        200..299
                    ) {

                        connection
                            .inputStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }

                    } else {

                        connection
                            .errorStream
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }
                            ?: ""
                    }

                } catch (
                    e: Exception
                ) {

                    Log.e(
                        TAG,
                        "Could not read server response",
                        e
                    )

                    ""
                }


            Log.d(
                TAG,
                "Server response=$responseText"
            )


            /*
             * =================================
             * SUCCESS
             * =================================
             */
            if (
                responseCode in
                200..299
            ) {

                emergencyActive =
                    true

                alertInProgress =
                    false


                Log.d(
                    TAG,
                    "================================"
                )

                Log.d(
                    TAG,
                    "EMERGENCY ALERT SENT SUCCESSFULLY"
                )

                Log.d(
                    TAG,
                    "================================"
                )

            } else {

                Log.e(
                    TAG,
                    "SERVER REJECTED ALERT"
                )


                /*
                 * Allow another attempt.
                 */
                alertInProgress =
                    false
            }


        } catch (e: Exception) {

            Log.e(
                TAG,
                "FAILED TO SEND EMERGENCY ALERT",
                e
            )


            /*
             * Allow another attempt.
             */
            alertInProgress =
                false

        } finally {

            connection?.disconnect()
        }
    }


    /*
     * ========================================
     * ESCAPE JSON
     * ========================================
     */

    private fun escapeJson(
        value: String
    ): String {

        return value
            .replace(
                "\\",
                "\\\\"
            )
            .replace(
                "\"",
                "\\\""
            )
            .replace(
                "\n",
                "\\n"
            )
            .replace(
                "\r",
                "\\r"
            )
    }


    /*
     * ========================================
     * FOREGROUND NOTIFICATION
     * ========================================
     */

    private fun startProtectionNotification() {

        val notification =
            createNotification()


        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            ServiceCompat.startForeground(

                this,

                NOTIFICATION_ID,

                notification,

                android.content.pm.ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_LOCATION
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }


        Log.d(
            TAG,
            "Foreground service started successfully"
        )
    }


    /*
     * ========================================
     * CREATE NOTIFICATION
     * ========================================
     */

    private fun createNotification():
            Notification {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )


        val pendingIntent =
            PendingIntent.getActivity(

                this,

                0,

                intent,

                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        return NotificationCompat
            .Builder(
                this,
                CHANNEL_ID
            )
            .setContentTitle(
                "Bachao Protection Active"
            )
            .setContentText(
                "Shake your phone to send an emergency alert"
            )
            .setSmallIcon(
                R.drawable.ic_launcher_foreground
            )
            .setContentIntent(
                pendingIntent
            )
            .setOngoing(true)
            .setPriority(
                NotificationCompat.PRIORITY_HIGH
            )
            .build()
    }


    /*
     * ========================================
     * NOTIFICATION CHANNEL
     * ========================================
     */

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(

                    CHANNEL_ID,

                    "Bachao Protection",

                    NotificationManager
                        .IMPORTANCE_LOW
                )


            channel.description =
                "Bachao emergency protection service"


            val manager =
                getSystemService(
                    NotificationManager::class.java
                )


            manager.createNotificationChannel(
                channel
            )
        }
    }


    /*
     * ========================================
     * RESET EMERGENCY
     * ========================================
     */

    fun resetEmergencyState() {

        emergencyActive =
            false

        alertInProgress =
            false


        Log.d(
            TAG,
            "Emergency state reset"
        )
    }


    /*
     * ========================================
     * SERVICE DESTROYED
     * ========================================
     */

    override fun onDestroy() {

        Log.d(
            TAG,
            "EmergencyService DESTROYED"
        )


        /*
         * Stop shake detector.
         */
        shakeDetector.stop()


        /*
         * Cancel coroutines.
         */
        serviceScope.cancel()


        super.onDestroy()
    }


    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}