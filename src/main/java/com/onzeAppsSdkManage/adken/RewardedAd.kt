// RewardedAd.kt
package com.onzeAppsSdkManage.adken

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import org.json.JSONObject
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.Player
import com.onzeAppsSdkManage.AdKen.R
import com.onzeAppsSdkManage.adken.internal.AdNetworkClient

/**
 * Interface a ser implementada pelo desenvolvedor para receber notificações quando o usuário
 * ganha uma recompensa por interagir com um anúncio recompensado.
 */
fun interface OnUserEarnedRewardListener {
    /**
     * Chamado quando o usuário conclui a interação com o anúncio e ganha uma recompensa.
     * @param rewardItem Um objeto que descreve a recompensa ganha.
     */
    fun onUserEarnedReward(rewardItem: RewardItem)
}

/**
 * Representa o item de recompensa que o usuário ganhou.
 * @param amount A quantidade da recompensa.
 * @param type O tipo da recompensa (e.g., "coins", "lives").
 */
data class RewardItem(val amount: Int, val type: String)

/**
 * Representa um anúncio recompensado (rewarded ad).
 * Anúncios recompensados permitem que os usuários ganhem recompensas no aplicativo
 * por interagir com o anúncio, geralmente assistindo a um vídeo.
 */
class RewardedAd(private val activity: Activity, private val adUnitId: String) {

    private val TAG = "AdKen_RewardedAd"
    var adListener: AdListener? = null
    var onUserEarnedRewardListener: OnUserEarnedRewardListener? = null

    private var isAdLoaded = false
    private var adData: JSONObject? = null // Armazena os dados do anúncio completo
    private var exoPlayer: SimpleExoPlayer? = null
    private var userEarnedReward = false // Flag para controlar se a recompensa foi concedida

    /**
     * Carrega um anúncio recompensado.
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
            Log.w(TAG, "Anúncio recompensado já carregado.")
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
                    Log.d(TAG, "Anúncio recompensado carregado com sucesso.")
                    adListener?.onAdLoaded(adData)
                }

                override fun onFailure(errorCode: Int, errorMessage: String) {
                    Log.e(TAG, "Falha ao carregar anúncio recompensado: $errorCode - $errorMessage")
                    isAdLoaded = false
                    adData = null
                    adListener?.onAdFailedToLoad(errorCode, errorMessage)
                }
            }
        )
    }

    /**
     * Exibe o anúncio recompensado se ele estiver carregado.
     */
    @SuppressLint("SetJavaScriptEnabled", "InflateParams")
    fun show() {
        if (!isAdLoaded || adData == null) {
            Log.w(TAG, "Anúncio recompensado não carregado ou dados ausentes.")
            adListener?.onAdFailedToLoad(-7, "Anúncio não pronto para exibição.")
            return
        }

        val adCreative = adData!!.optJSONObject("creative")
        val creativeType = adCreative?.optString("type") ?: "" // Changed from "creative_type" to "type"
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
        val rewardCallbackUrl = adCreative.optString("reward_callback_url")

        val dialog = Dialog(activity, android.R.style.Theme_NoTitleBar_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.ad_rewarded_layout)
            setCancelable(false) // Recompensados geralmente não são canceláveis até o fim do vídeo/interação
        }

        val rootLayout = dialog.findViewById<LinearLayout>(R.id.rewarded_ad_root_layout)
        rootLayout.removeAllViews() // Limpa quaisquer views anteriores

        adListener?.onAdOpened() // Notifica que o anúncio foi aberto

        // Report impression immediately when the ad is shown
        if (impressionUrl.isNotBlank() && adId.isNotBlank() && adUnitId.isNotBlank() && applicationSdkId.isNotBlank()) {
            AdNetworkClient.reportImpression(adId, adUnitId, applicationSdkId, impressionUrl)
            adListener?.onAdImpression()
        }

        val closeButton = Button(activity, null, android.R.attr.buttonStyleSmall).apply { // Estilo padrão do Android
            text = "X"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.END
                setMargins(0, 16, 16, 0)
            }
            // Para anúncios recompensados, o botão de fechar só deve aparecer após a recompensa ou um tempo.
            // Para HTML, a recompensa é concedida na onPageFinished, então podemos mostrá-lo.
            // Para vídeo, ele é mostrado após o STATE_ENDED.
            visibility = View.GONE // Initially hidden
            setOnClickListener {
                dialog.dismiss()
            }
        }

        when (creativeType) {
            "VIDEO" -> {
                if (assetUrl.isNotBlank()) {
                    val playerView = PlayerView(activity).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false // Geralmente vídeos recompensados não têm controles para forçar a visualização
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
                                    Log.d(TAG, "Vídeo recompensado concluído. Concedendo recompensa.")
                                    userEarnedReward = true
                                    // Report reward to backend
                                    if (rewardCallbackUrl.isNotBlank() && adId.isNotBlank() && adUnitId.isNotBlank() && applicationSdkId.isNotBlank()) {
                                        val rewardAmount = videoData?.optInt("reward_amount", 1) ?: 1
                                        val rewardType = videoData?.optString("reward_type", "coins") ?: "coins"
                                        AdNetworkClient.reportRewardGranted(adId, adUnitId, applicationSdkId, rewardCallbackUrl, rewardAmount, rewardType)
                                        onUserEarnedRewardListener?.onUserEarnedReward(RewardItem(rewardAmount, rewardType))
                                    }
                                    activity.runOnUiThread {
                                        closeButton.visibility = View.VISIBLE // Mostra botão de fechar
                                    }
                                }
                            }

                            override fun onPlayerError(error: PlaybackException) {
                                Log.e(TAG, "Erro no ExoPlayer (Recompensado): ${error.message}", error)
                                adListener?.onAdFailedToLoad(-9, "Erro na reprodução do vídeo recompensado.")
                                dialog.dismiss()
                            }
                        })
                    }
                    rootLayout.addView(closeButton) // Add close button after player
                } else {
                    Log.e(TAG, "URL do ativo de vídeo ausente para recompensado.")
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
                            Log.e(TAG, "WebView Error (Rewarded): ${error?.description}")
                            adListener?.onAdFailedToLoad(-11, "Erro na WebView do recompensado: ${error?.description}")
                            dialog.dismiss()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d(TAG, "WebView onPageFinished for Rewarded HTML.")
                            // For HTML rewarded ads, reward might be granted on page finish or specific interaction
                            // For this example, let's grant reward on page finish (simulating an interaction)
                            userEarnedReward = true
                            if (rewardCallbackUrl.isNotBlank() && adId.isNotBlank() && adUnitId.isNotBlank() && applicationSdkId.isNotBlank()) {
                                val rewardAmount = videoData?.optInt("reward_amount", 1) ?: 1
                                val rewardType = videoData?.optString("reward_type", "coins") ?: "coins"
                                AdNetworkClient.reportRewardGranted(adId, adUnitId, applicationSdkId, rewardCallbackUrl, rewardAmount, rewardType)
                                onUserEarnedRewardListener?.onUserEarnedReward(RewardItem(rewardAmount, rewardType))
                            }
                            activity.runOnUiThread {
                                closeButton.visibility = View.VISIBLE // Show close button after "reward"
                            }
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
                                    Log.e(TAG, "Erro ao abrir URL externa no recompensado: ${e.message}", e)
                                }
                                dialog.dismiss() // Optionally close rewarded ad if external link clicked
                                return true
                            }
                            return false
                        }
                    }
                    loadDataWithBaseURL(null, htmlAdContent, "text/html", "UTF-8", null)
                }
                rootLayout.addView(webView)
                rootLayout.addView(closeButton) // Add close button for HTML
            }
            else -> {
                Log.e(TAG, "Tipo de criativo não suportado para recompensado: $creativeType")
                adListener?.onAdFailedToLoad(-8, "Tipo de criativo não suportado: $creativeType")
                dialog.dismiss()
            }
        }

        dialog.setOnDismissListener {
            exoPlayer?.release() // Libera o player quando o diálogo é fechado
            adListener?.onAdClosed() // Notifica que o anúncio foi fechado
            isAdLoaded = false // Redefine para permitir que um novo anúncio seja carregado
            adData = null // Limpa os dados do anúncio
            userEarnedReward = false // Reseta a flag
        }
        dialog.show()
    }

    /**
     * Verifica se o anúncio recompensado está carregado e pronto para ser exibido.
     * @return `true` se o anúncio estiver carregado, `false` caso contrário.
     */
    fun isLoaded(): Boolean {
        return isAdLoaded
    }
}