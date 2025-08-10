// AdNetworkClient.kt
package com.onzeAppsSdkManage.adken.internal

import android.util.Log
import com.onzeAppsSdkManage.adken.AdRequest
import com.onzeAppsSdkManage.adken.AdListener
import com.onzeAppsSdkManage.adken.AdKenError
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class AdResponse(
    val success: Boolean,
    val ad: AdData?,
    val message: String?
)

data class AdData(
    val id: String,
    val type: String,
    val imageUrl: String?,
    val htmlContent: String?,
    val redirectUrl: String,
    val width: Int,
    val height: Int
)

interface AdService {
    @POST("v1/ad_request")
    fun requestBannerAd(@Body request: AdRequest): Call<AdResponse>
}

object AdNetworkClient {
    private const val BASE_URL = "https://api.adken.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    private val adService: AdService by lazy {
        retrofit.create(AdService::class.java)
    }

    fun requestAd(request: AdRequest, listener: AdListener) {
        Log.d("AdNetworkClient", "Enviando solicitação de anúncio para o backend.")
        adService.requestBannerAd(request).enqueue(object : Callback<AdResponse> {
            override fun onResponse(call: Call<AdResponse>, response: Response<AdResponse>) {
                if (response.isSuccessful) {
                    val adResponse = response.body()
                    if (adResponse?.success == true && adResponse.ad != null) {
                        listener.onAdLoaded(adResponse.ad)
                    } else {
                        Log.e("AdNetworkClient", "Resposta de anúncio sem sucesso ou dados ausentes.")
                        listener.onAdFailedToLoad(AdKenError.NO_FILL)
                    }
                } else {
                    Log.e("AdNetworkClient", "Erro na resposta do servidor: ${response.code()}")
                    listener.onAdFailedToLoad(AdKenError.INVALID_REQUEST)
                }
            }

            override fun onFailure(call: Call<AdResponse>, t: Throwable) {
                Log.e("AdNetworkClient", "Falha na comunicação de rede: ${t.message}")
                listener.onAdFailedToLoad(AdKenError.NETWORK_ERROR)
            }
        })
    }
}