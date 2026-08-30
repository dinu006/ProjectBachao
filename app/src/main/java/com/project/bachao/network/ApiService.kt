package com.project.bachao.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    // =========================================================
    // REGISTER
    // =========================================================

    @POST("api/auth/register")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>


    // =========================================================
    // GET USER
    // =========================================================

    @GET("api/users/{id}")
    suspend fun getUser(
        @Path("id") userId: Int
    ): Response<UserResponse>


    // =========================================================
    // CREATE EMERGENCY ALERT
    // =========================================================

    @POST("api/alerts")
    suspend fun createAlert(
        @Body request: AlertRequest
    ): Response<AlertResponse>


    // =========================================================
    // SEND LIVE LOCATION
    // =========================================================

    @POST("api/alerts/{id}/location")
    suspend fun sendLocation(
        @Path("id") alertId: Int,
        @Body request: LocationRequest
    ): Response<BasicResponse>


    // =========================================================
    // RESOLVE ALERT
    // =========================================================

    @PUT("api/alerts/{id}/resolve")
    suspend fun resolveAlert(
        @Path("id") alertId: Int
    ): Response<BasicResponse>
}


// =============================================================
// REGISTER REQUEST
// =============================================================

data class RegisterRequest(

    @SerializedName("name")
    val name: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("email")
    val email: String
)


// =============================================================
// REGISTER RESPONSE
// =============================================================

data class RegisterResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("user")
    val user: User?
)


// =============================================================
// USER
// =============================================================

data class User(

    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("email")
    val email: String
)


// =============================================================
// USER RESPONSE
// =============================================================

data class UserResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("user")
    val user: User?
)


// =============================================================
// ALERT REQUEST
// =============================================================

data class AlertRequest(

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("trigger_type")
    val triggerType: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("accuracy")
    val accuracy: Float
)


// =============================================================
// ALERT RESPONSE
// =============================================================

data class AlertResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("alert")
    val alert: Alert?
)


// =============================================================
// ALERT
// =============================================================

data class Alert(

    @SerializedName("id")
    val id: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("trigger_type")
    val triggerType: String?,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("accuracy")
    val accuracy: Float?
)


// =============================================================
// LOCATION REQUEST
// =============================================================

data class LocationRequest(

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("accuracy")
    val accuracy: Float
)


// =============================================================
// BASIC RESPONSE
// =============================================================

data class BasicResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?
)