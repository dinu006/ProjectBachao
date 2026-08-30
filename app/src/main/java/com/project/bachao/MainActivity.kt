package com.project.bachao

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat

import com.project.bachao.data.UserPreferences
import com.project.bachao.network.ApiClient
import com.project.bachao.network.RegisterRequest
import com.project.bachao.service.EmergencyService

import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "BACHAO_MAIN"
    }

    private lateinit var userPreferences: UserPreferences

    private var protectionStartRequested = false

    /*
     * Permission launcher
     */
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            Log.d(TAG, "Permission result: $permissions")

            val locationGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

            val notificationGranted =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

            if (locationGranted && notificationGranted) {

                Log.d(
                    TAG,
                    "Required permissions granted"
                )

                if (protectionStartRequested) {
                    startProtectionService()
                }

            } else {

                Log.e(
                    TAG,
                    "Required permissions were not granted"
                )
            }

            protectionStartRequested = false
        }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        userPreferences =
            UserPreferences(this)

        Log.d(
            TAG,
            "MainActivity started"
        )

        Log.d(
            TAG,
            "Stored user ID = ${userPreferences.getUserId()}"
        )

        Log.d(
            TAG,
            "Stored user name = ${userPreferences.getName()}"
        )

        Log.d(
            TAG,
            "Stored user phone = ${userPreferences.getPhone()}"
        )

        Log.d(
            TAG,
            "Stored user email = ${userPreferences.getEmail()}"
        )

        setContent {

            MaterialTheme {

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {

                    BachaoScreen()
                }
            }
        }
    }


    @Composable
    private fun BachaoScreen() {

        var name by remember {
            mutableStateOf("")
        }

        var phone by remember {
            mutableStateOf("")
        }

        var email by remember {
            mutableStateOf("")
        }

        var message by remember {
            mutableStateOf("")
        }

        var loading by remember {
            mutableStateOf(false)
        }

        var protectionActive by remember {
            mutableStateOf(false)
        }

        val scope =
            rememberCoroutineScope()


        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),

            verticalArrangement =
                Arrangement.Center

        ) {

            /*
             * APP TITLE
             */

            Text(
                text = "Bachao",

                style =
                    MaterialTheme
                        .typography
                        .headlineLarge
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                text =
                    "Women Safety Application"
            )

            Spacer(
                Modifier.height(24.dp)
            )


            /*
             * REGISTRATION SCREEN
             */

            if (!userPreferences.isRegistered()) {

                /*
                 * NAME
                 */

                OutlinedTextField(

                    value = name,

                    onValueChange = {
                        name = it
                    },

                    label = {
                        Text("Name")
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true
                )


                Spacer(
                    Modifier.height(12.dp)
                )


                /*
                 * PHONE
                 */

                OutlinedTextField(

                    value = phone,

                    onValueChange = {
                        phone = it
                    },

                    label = {
                        Text("Phone")
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true
                )


                Spacer(
                    Modifier.height(12.dp)
                )


                /*
                 * EMAIL
                 */

                OutlinedTextField(

                    value = email,

                    onValueChange = {
                        email = it
                    },

                    label = {
                        Text("Email")
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true
                )


                Spacer(
                    Modifier.height(20.dp)
                )


                /*
                 * REGISTER BUTTON
                 */

                Button(

                    enabled = !loading,

                    onClick = {

                        if (
                            name.isBlank() ||
                            phone.isBlank() ||
                            email.isBlank()
                        ) {

                            message =
                                "Please fill all fields."

                            return@Button
                        }


                        loading = true

                        message = ""


                        scope.launch {

                            try {

                                Log.d(
                                    TAG,
                                    "Registering user..."
                                )


                                val response =
                                    ApiClient
                                        .api
                                        .registerUser(

                                            RegisterRequest(

                                                name =
                                                    name.trim(),

                                                phone =
                                                    phone.trim(),

                                                email =
                                                    email.trim()
                                            )
                                        )


                                Log.d(
                                    TAG,
                                    "Registration HTTP code = ${response.code()}"
                                )


                                if (response.isSuccessful) {

                                    val body =
                                        response.body()


                                    if (
                                        body != null &&
                                        body.success &&
                                        body.user != null
                                    ) {

                                        /*
                                         * SAVE USER
                                         */

                                        userPreferences.saveUser(

                                            userId =
                                                body.user.id,

                                            name =
                                                body.user.name,

                                            phone =
                                                body.user.phone,

                                            email =
                                                body.user.email
                                        )


                                        /*
                                         * VERIFY STORAGE
                                         */

                                        Log.d(
                                            TAG,
                                            "================================"
                                        )

                                        Log.d(
                                            TAG,
                                            "USER SAVED SUCCESSFULLY"
                                        )

                                        Log.d(
                                            TAG,
                                            "User ID = ${userPreferences.getUserId()}"
                                        )

                                        Log.d(
                                            TAG,
                                            "Name = ${userPreferences.getName()}"
                                        )

                                        Log.d(
                                            TAG,
                                            "Phone = ${userPreferences.getPhone()}"
                                        )

                                        Log.d(
                                            TAG,
                                            "Email = ${userPreferences.getEmail()}"
                                        )

                                        Log.d(
                                            TAG,
                                            "================================"
                                        )


                                        message =
                                            "Registration successful."

                                    } else {

                                        message =
                                            body?.message
                                                ?: "Registration failed."
                                    }

                                } else {

                                    val error =
                                        response
                                            .errorBody()
                                            ?.string()

                                    message =
                                        error
                                            ?: "HTTP ${response.code()}"
                                }

                            } catch (e: Exception) {

                                Log.e(
                                    TAG,
                                    "Registration error",
                                    e
                                )

                                message =
                                    "Connection error: ${e.message}"

                            } finally {

                                loading = false
                            }
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Text(
                        if (loading)
                            "Registering..."
                        else
                            "Register"
                    )
                }


            } else {


                /*
                 * USER SCREEN
                 */

                Text(

                    text =
                        "Welcome, ${userPreferences.getName()}",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge
                )


                Spacer(
                    Modifier.height(12.dp)
                )


                Text(
                    text =
                        "Phone: ${userPreferences.getPhone()}"
                )


                Spacer(
                    Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Email: ${userPreferences.getEmail()}"
                )


                Spacer(
                    Modifier.height(24.dp)
                )


                /*
                 * PROTECTION BUTTON
                 */

                Button(

                    onClick = {

                        if (protectionActive) {

                            /*
                             * STOP
                             */

                            Log.d(
                                TAG,
                                "Stopping protection"
                            )

                            stopProtectionService()

                            protectionActive =
                                false

                            message =
                                "Protection stopped."

                        } else {

                            /*
                             * START
                             */

                            Log.d(
                                TAG,
                                "Starting protection request"
                            )

                            protectionStartRequested =
                                true

                            requestPermissionsAndStart()
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Text(

                        if (protectionActive)

                            "STOP PROTECTION"

                        else

                            "ACTIVATE PROTECTION"
                    )
                }
            }


            Spacer(
                Modifier.height(20.dp)
            )


            /*
             * STATUS MESSAGE
             */

            if (message.isNotBlank()) {

                Text(
                    text = message
                )
            }
        }
    }


    /*
     * REQUEST REQUIRED PERMISSIONS
     */

    private fun requestPermissionsAndStart() {

        val permissions =
            mutableListOf<String>()


        /*
         * FINE LOCATION
         */

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            permissions.add(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }


        /*
         * COARSE LOCATION
         */

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            permissions.add(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }


        /*
         * ANDROID 13+ NOTIFICATION
         */

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                permissions.add(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }


        /*
         * ALL PERMISSIONS ALREADY GRANTED
         */

        if (permissions.isEmpty()) {

            Log.d(
                TAG,
                "All permissions already granted"
            )

            startProtectionService()

        } else {

            Log.d(
                TAG,
                "Requesting permissions: $permissions"
            )

            permissionLauncher.launch(
                permissions.toTypedArray()
            )
        }
    }


    /*
     * START EMERGENCY SERVICE
     */

    private fun startProtectionService() {

        /*
         * VERY IMPORTANT:
         * Verify user exists before starting service.
         */

        val userId =
            userPreferences.getUserId()


        if (userId == -1) {

            Log.e(
                TAG,
                "Cannot start protection: no user ID"
            )

            return
        }


        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "STARTING EMERGENCY SERVICE"
        )

        Log.d(
            TAG,
            "User ID = $userId"
        )

        Log.d(
            TAG,
            "User name = ${userPreferences.getName()}"
        )

        Log.d(
            TAG,
            "User phone = ${userPreferences.getPhone()}"
        )

        Log.d(
            TAG,
            "User email = ${userPreferences.getEmail()}"
        )

        Log.d(
            TAG,
            "================================"
        )


        val intent =
            Intent(
                this,
                EmergencyService::class.java
            )


        try {

            ContextCompat.startForegroundService(
                this,
                intent
            )

            Log.d(
                TAG,
                "Foreground service start requested"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Failed to start EmergencyService",
                e
            )

            return
        }
    }


    /*
     * STOP EMERGENCY SERVICE
     */

    private fun stopProtectionService() {

        Log.d(
            TAG,
            "Stopping EmergencyService"
        )


        val intent =
            Intent(
                this,
                EmergencyService::class.java
            )


        stopService(intent)
    }


    override fun onDestroy() {

        Log.d(
            TAG,
            "MainActivity destroyed"
        )

        super.onDestroy()
    }
}