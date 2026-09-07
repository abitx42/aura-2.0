package com.aura.personalos.api

import android.content.Context
import com.aura.personalos.auth.AuraSessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object AuraApiClient {

    // Default development base URL (10.0.2.2 points to host machine from Android Emulator)
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/api/v1/"

    @Volatile
    private var apiService: AuraApiService? = null

    fun getService(context: Context, baseUrl: String = DEFAULT_BASE_URL): AuraApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(context.applicationContext, baseUrl).create(AuraApiService::class.java).also {
                apiService = it
            }
        }
    }

    private fun buildRetrofit(context: Context, baseUrl: String): Retrofit {
        val sessionManager = AuraSessionManager.getInstance(context)

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            val token = sessionManager.accessToken
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            requestBuilder.header("Content-Type", "application/json")
            requestBuilder.header("X-Client-Version", "2.0.0")

            chain.proceed(requestBuilder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
