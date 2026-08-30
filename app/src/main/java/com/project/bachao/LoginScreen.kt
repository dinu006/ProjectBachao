// In LoginScreen.kt or directly in MainActivity.kt
package com.project.bachao

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.bachao.data.UserPreferences
import com.project.bachao.network.ApiClient
import com.project.bachao.network.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RegisterScreen(onRegistrationSuccess: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userPrefs = remember { UserPreferences(context) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Modern Palette
    val deepNavy = Color(0xFF0F172A)
    val cardBackground = Color(0xFF1E293B)
    val inputBackground = Color(0xFF334155)
    val primaryAccent = Color(0xFF6366F1)
    val secondaryAccent = Color(0xFF8B5CF6)
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(deepNavy)
    ) {
        // Decorative background glow elements
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .clip(CircleShape)
                .background(primaryAccent.copy(alpha = 0.22f))
                .blur(70.dp)
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape)
                .background(secondaryAccent.copy(alpha = 0.18f))
                .blur(70.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Gradient Icon Badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(primaryAccent, secondaryAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Shield Logo",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Bachao",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Emergency protection & immediate response",
                fontSize = 14.sp,
                color = textSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Glassmorphic Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground.copy(alpha = 0.88f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Create Account",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Enter your verified details below",
                        fontSize = 13.sp,
                        color = textSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Full Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Alex Morgan", color = textSecondary.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Name Icon",
                                tint = primaryAccent
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = inputBackground.copy(alpha = 0.5f),
                            unfocusedContainerColor = inputBackground.copy(alpha = 0.35f),
                            focusedBorderColor = primaryAccent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = primaryAccent,
                            unfocusedLabelColor = textSecondary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone Number Input
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. 9876543210", color = textSecondary.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone Icon",
                                tint = primaryAccent
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = inputBackground.copy(alpha = 0.5f),
                            unfocusedContainerColor = inputBackground.copy(alpha = 0.35f),
                            focusedBorderColor = primaryAccent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = primaryAccent,
                            unfocusedLabelColor = textSecondary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g. name@example.com", color = textSecondary.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon",
                                tint = primaryAccent
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedContainerColor = inputBackground.copy(alpha = 0.5f),
                            unfocusedContainerColor = inputBackground.copy(alpha = 0.35f),
                            focusedBorderColor = primaryAccent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedLabelColor = primaryAccent,
                            unfocusedLabelColor = textSecondary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Gradient Submit Button
                    Button(
                        onClick = {
                            if (name.trim().length < 2 || phone.trim().length < 10 || email.trim().isEmpty()) {
                                Toast.makeText(context, "Please fill in valid details.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val req = RegisterRequest(
                                        name = name.trim(),
                                        phone = phone.trim(),
                                        email = email.trim()
                                    )
                                    val response = ApiClient.api.registerUser(req)

                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        if (response.isSuccessful && response.body()?.user != null) {
                                            val user = response.body()!!.user!!
                                            // Save registered user locally
                                            userPrefs.saveUser(
                                                userId = user.id,
                                                name = user.name,
                                                phone = user.phone,
                                                email = user.email
                                            )
                                            Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                            onRegistrationSuccess()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                response.body()?.message ?: "Registration failed",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        Toast.makeText(
                                            context,
                                            "Error: ${e.localizedMessage ?: "Could not connect to server"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = primaryAccent.copy(alpha = 0.4f)
                        ),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(
                                brush = if (!isLoading) {
                                    Brush.horizontalGradient(listOf(primaryAccent, secondaryAccent))
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            primaryAccent.copy(alpha = 0.5f),
                                            secondaryAccent.copy(alpha = 0.5f)
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Creating Account...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Register",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
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