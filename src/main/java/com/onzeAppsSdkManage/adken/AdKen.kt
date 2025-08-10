// AdKen.kt
package com.onzeAppsSdkManage.adken

import android.content.Context
import android.util.Log
import com.onzeAppsSdkManage.adken.internal.AdData
import com.onzeAppsSdkManage.adken.internal.AdNetworkClient

object AdKen {
    private const val TAG = "AdKenSDK"
    private var isInitialized = false
    private var applicationId: String? = null
    internal var applicationContext: Context? = null

    /**
     * Inicializa o SDK com o ID da sua aplicação.
     * Deve ser chamado uma vez no início do ciclo de vida da aplicação.
     * @param context O contexto da aplicação.
     * @param appId O ID da sua aplicação na plataforma AdKen.
     */
    fun initialize(context: Context, appId: String) {
        if (!isInitialized) {
            applicationContext = context.applicationContext
            applicationId = appId
            isInitialized = true
            Log.d(TAG, "SDK AdKen inicializado com sucesso para o app: $applicationId")
            // Inicie a coleta de dados, como o GAID, em uma thread de background, se necessário.
        }
    }

    /**
     * Carrega um anúncio de banner.
     * @param adUnitId O ID do bloco de anúncio.
     * @param adListener O callback para eventos do anúncio.
     */
    fun loadBannerAd(adUnitId: String, adListener: AdListener) {
        if (!isInitialized || applicationContext == null || applicationId == null) {
            Log.e(TAG, "O SDK AdKen não foi inicializado corretamente.")
            adListener.onAdFailedToLoad(AdKenError.SDK_NOT_INITIALIZED)
            return
        }

        val adRequest = AdRequest(adUnitId)
        AdNetworkClient.requestAd(adRequest, adListener)
    }
}