package com.project.bachao

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.project.bachao.data.UserPreferences
import com.project.bachao.network.ApiClient
import com.project.bachao.service.EmergencyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    // Check if user is already registered
    var isUserLoggedIn by remember { mutableStateOf(userPreferences.isRegistered()) }

    if (isUserLoggedIn) {
        MainProtectionScreen(
            onLogout = {
                userPreferences.clearUser()
                isUserLoggedIn = false
            }
        )
    } else {
        RegisterScreen(
            onRegistrationSuccess = {
                isUserLoggedIn = true
            }
        )
    }
}

@Composable
fun MainProtectionScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }

    val activeAlertId by userPreferences.activeAlertIdFlow.collectAsState()
    var isCancelling by remember { mutableStateOf(false) }

    val isAlertActive = activeAlertId != -1

    // Theme Palette
    val deepNavy = Color(0xFF0F172A)
    val cardBackground = Color(0xFF1E293B)
    val primaryAccent = Color(0xFF6366F1)
    val secondaryAccent = Color(0xFF8B5CF6)
    val safeGreen = Color(0xFF10B981)
    val safeGreenDark = Color(0xFF059669)
    val alertRed = Color(0xFFEF4444)
    val alertRedDark = Color(0xFFDC2626)
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)

    val permissionsToRequest = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            val serviceIntent = Intent(context, EmergencyService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            Toast.makeText(context, "Location permissions are required for safety alerts.", Toast.LENGTH_LONG).show()
        }
    }

    // Start protection service when user is logged in
    LaunchedEffect(Unit) {
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            val serviceIntent = Intent(context, EmergencyService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepNavy)
    ) {
        // Dynamic decorative background glow based on alert status
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(
                    if (isAlertActive) alertRed.copy(alpha = 0.25f)
                    else safeGreen.copy(alpha = 0.20f)
                )
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape)
                .background(
                    if (isAlertActive) alertRedDark.copy(alpha = 0.20f)
                    else primaryAccent.copy(alpha = 0.18f)
                )
                .blur(80.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground.copy(alpha = 0.90f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isAlertActive) {
                        // Alert Active State Icon
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(alertRed.copy(alpha = 0.15f))
                                .border(2.dp, alertRed.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency Alert",
                                tint = alertRed,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "EMERGENCY ALERT ACTIVE",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = alertRed,
                            letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = alertRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Alert ID: #$activeAlertId",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = alertRed,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Live emergency responders and your chosen contacts are notified.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                isCancelling = true

                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val response = ApiClient.api.resolveAlert(activeAlertId)

                                        withContext(Dispatchers.Main) {
                                            isCancelling = false

                                            if (response.isSuccessful && response.body()?.success == true) {
                                                userPreferences.clearActiveAlertId()

                                                val resetIntent = Intent(context, EmergencyService::class.java).apply {
                                                    action = EmergencyService.ACTION_RESET_EMERGENCY
                                                }
                                                context.startService(resetIntent)

                                                Toast.makeText(
                                                    context,
                                                    "Alert successfully cancelled!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Failed to cancel: ${response.message()}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isCancelling = false
                                            Toast.makeText(
                                                context,
                                                "Network error: ${e.localizedMessage ?: "Unable to connect"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isCancelling,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = alertRedDark.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .background(
                                    brush = Brush.horizontalGradient(listOf(alertRed, alertRedDark)),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            if (isCancelling) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Cancelling Alert...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Cancel Alert",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // Protected / Normal State Icon
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(safeGreen.copy(alpha = 0.15f))
                                .border(2.dp, safeGreen.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Protection",
                                tint = safeGreen,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Bachao Protection Active",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // User profile pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF334155).copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User",
                                    tint = primaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = userPreferences.getName().ifEmpty { "Protected User" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Shake your phone vigorously in case of an emergency to trigger an instant alert.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // Modern Outlined Logout Button
                        OutlinedButton(
                            onClick = {
                                val stopServiceIntent = Intent(context, EmergencyService::class.java)
                                context.stopService(stopServiceIntent)
                                onLogout()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = textSecondary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(
                                    listOf(Color(0xFF475569), Color(0xFF334155))
                                )
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                modifier = Modifier.size(18.dp),
                                tint = textSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Logout & Stop Protection",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer note
            Text(
                text = "Secured by Bachao Safety System",
                fontSize = 12.sp,
                color = textSecondary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}