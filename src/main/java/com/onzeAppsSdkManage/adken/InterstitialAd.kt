// InterstitialAd.kt
package com.onzeAppsSdkManage.adken

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.annotation.SuppressLint
import android.view.Window
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import org.json.JSONObject
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.Player
import com.onzeAppsSdkManage.AdKen.R
import com.onzeAppsSdkManage.adken.internal.AdNetworkClient // Importe o AdNetworkClient

/**
 * Representa um anúncio intersticial (tela cheia).
 * Um anúncio intersticial cobre toda a tela do aplicativo do usuário.
 */
class InterstitialAd(private val activity: Activity, private val adUnitId: String) {

    private val TAG = "AdKen_InterstitialAd"
    var adListener: AdListener? = null
    private var isAdLoaded = false
    private var adData: JSONObject? = null // Armazena os dados do anúncio completo
    private var exoPlayer: SimpleExoPlayer? = null

    /**
     * Carrega um anúncio intersticial.
     * @param adRequest A solicitação de anúncio com critérios de segmentação.
     */
    fun loadAd(adRequest: AdRequest) {
        if (!AdKen.isInitialized()) {
            val errorMessage = "AdKen SDK não inicializado. Chame AdKen.initialize() primeiro."
            Log.e(TAG, errorMessage)
            adListener?.onAdFailedToLoad(-1, errorMessage)
            return
        }
        if (isAdLoaded) {
            Log.w(TAG, "Anúncio intersticial já carregado.")
            adListener?.onAdLoaded(adData) // Notifica que já está pronto
            return
        }

        AdNetworkClient.requestAd(
            adUnitId,
            adRequest,
            object : AdNetworkClient.AdNetworkCallback {
                override fun onSuccess(response: JSONObject) {
                    adData = response
                    isAdLoaded = true
                    Log.d(TAG, "Anúncio intersticial carregado com sucesso.")
                    adListener?.onAdLoaded(adData)
                }

                override fun onFailure(errorCode: Int, errorMessage: String) {
                    Log.e(TAG, "Falha ao carregar anúncio intersticial: $errorCode - $errorMessage")
                    isAdLoaded = false
                    adData = null
                    adListener?.onAdFailedToLoad(errorCode, errorMessage)
                }
            }
        )
    }

    /**
     * Exibe o anúncio intersticial se ele estiver carregado.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun show() {
        if (!isAdLoaded || adData == null) {
            Log.w(TAG, "Anúncio intersticial não carregado ou dados ausentes.")
            adListener?.onAdFailedToLoad(-7, "Anúncio não pronto para exibição.")
            return
        }

        val adCreative = adData!!.optJSONObject("creative")
        val creativeType = adCreative?.optString("type") ?: ""
        val adId = adData!!.optString("ad_id")
        val applicationSdkId = AdKen.getApplicationId()

        if (adCreative == null || applicationSdkId.isNullOrBlank()) {
            Log.e(TAG, "Dados do criativo ou Application ID ausentes.")
            adListener?.onAdFailedToLoad(-4, "Dados do criativo ou Application ID inválidos.")
            return
        }

        val impressionUrl = adCreative.optString("impression_url")
        val clickUrl = adCreative.optString("click_url")
        val assetUrl = adCreative.optString("asset_url")
        val htmlAdContent = adCreative.optString("html_content")
        val videoData = adCreative.optJSONObject("video_data")

        val dialog = Dialog(activity, android.R.style.Theme_NoTitleBar_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.ad_interstitial_layout) // Use o layout padrão
            setCancelable(false) // Intersticiais geralmente não são canceláveis
        }

        val rootLayout = dialog.findViewById<LinearLayout>(R.id.interstitial_ad_root_layout)
        rootLayout.removeAllViews() // Limpa quaisquer views anteriores

        adListener?.onAdOpened() // Notifica que o anúncio foi aberto

        // Report impression immediately when the ad is shown
        if (impressionUrl.isNotBlank() && adId.isNotBlank() && adUnitId.isNotBlank() && applicationSdkId.isNotBlank()) {
            AdNetworkClient.reportImpression(adId, adUnitId, applicationSdkId, impressionUrl)
            adListener?.onAdImpression()
        }

        when (creativeType) {
            "VIDEO" -> {
                if (assetUrl.isNotBlank()) {
                    val playerView = PlayerView(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = true // Mostra controles de vídeo
                    }
                    rootLayout.addView(playerView)

                    exoPlayer = SimpleExoPlayer.Builder(activity).build().also { player ->
                        playerView.player = player
                        val mediaItem = MediaItem.fromUri(Uri.parse(assetUrl))
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.playWhenReady = true

                        player.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    Log.d(TAG, "Vídeo intersticial concluído.")
                                    // Optionally close ad after video ends or show a close button
                                    // dialog.dismiss()
                                }
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                Log.e(TAG, "Erro no ExoPlayer: ${error.message}", error)
                                adListener?.onAdFailedToLoad(-9, "Erro na reprodução do vídeo intersticial.")
                                dialog.dismiss()
                            }
                        })
                    }
                } else {
                    Log.e(TAG, "URL do ativo de vídeo ausente para intersticial.")
                    adListener?.onAdFailedToLoad(-10, "URL do vídeo ausente.")
                    dialog.dismiss()
                }
            }
            "HTML" -> {
                val webView = WebView(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            Log.e(TAG, "WebView Error (Interstitial): ${error?.description}")
                            adListener?.onAdFailedToLoad(-11, "Erro na WebView do intersticial: ${error?.description}")
                            dialog.dismiss()
                        }

                        @Deprecated("Deprecated in API 24")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            return handleUrl(url, adId, applicationSdkId, clickUrl, dialog)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            return handleUrl(request?.url?.toString(), adId, applicationSdkId, clickUrl, dialog)
                        }

                        private fun handleUrl(url: String?, adId: String, applicationSdkId: String, clickUrl: String, dialog: Dialog): Boolean {
                            if (url != null && url.startsWith(clickUrl)) { // Check if it's the specific ad click URL
                                AdNetworkClient.reportClick(adId, adUnitId, applicationSdkId, clickUrl)
                                adListener?.onAdClicked()

                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                try {
                                    activity.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Erro ao abrir URL em navegador externo: ${e.message}", e)
                                }
                                dialog.dismiss() // Fecha o anúncio ao clicar
                                return true
                            } else if (url != null && !url.startsWith("data:text/html") && !url.startsWith("about:blank")) {
                                // Handle other non-ad links within the creative (open in external browser)
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                try {
                                    activity.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Erro ao abrir URL externa no intersticial: ${e.message}", e)
                                }
                                dialog.dismiss() // Optionally close interstitial if external link clicked
                                return true
                            }
                            return false
                        }
                    }
                    loadDataWithBaseURL(null, htmlAdContent, "text/html", "UTF-8", null)
                }
                rootLayout.addView(webView)
            }
            else -> {
                Log.e(TAG, "Tipo de criativo não suportado para intersticial: $creativeType")
                adListener?.onAdFailedToLoad(-8, "Tipo de criativo não suportado: $creativeType")
                dialog.dismiss()
            }
        }

        dialog.setOnDismissListener {
            exoPlayer?.release() // Libera o player quando o diálogo é fechado
            adListener?.onAdClosed() // Notifica que o anúncio foi fechado
            isAdLoaded = false // Redefine para permitir que um novo anúncio seja carregado
            adData = null // Limpa os dados do anúncio
        }
        dialog.show()
    }

    /**
     * Verifica se o anúncio intersticial está carregado e pronto para ser exibido.
     * @return `true` se o anúncio estiver carregado, `false` caso contrário.
     */
    fun isLoaded(): Boolean {
        return isAdLoaded
    }
}