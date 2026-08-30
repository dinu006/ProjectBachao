package com.project.bachao.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /*
     * IMPORTANT:
     *
     * Physical phone:
     * http://YOUR_PC_IP:3000/
     *
     * Example:
     * http://192.168.1.100:3000/
     */

    private const val BASE_URL =
        "http://192.168.31.200:3000/"


    private val client =
        OkHttpClient.Builder()

            .connectTimeout(
                15,
                TimeUnit.SECONDS
            )

            .readTimeout(
                30,
                TimeUnit.SECONDS
            )

            .writeTimeout(
                30,
                TimeUnit.SECONDS
            )

            .build()


    val api: ApiService by lazy {

        Retrofit.Builder()

            .baseUrl(BASE_URL)

            .client(client)

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(ApiService::class.java)
    }
}