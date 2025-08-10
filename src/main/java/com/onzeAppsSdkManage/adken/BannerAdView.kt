// BannerAdView.kt
package com.onzeAppsSdkManage.adken

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.onzeAppsSdkManage.adken.internal.AdData

/**
 * Uma View personalizada para exibir anúncios em formato de banner.
 * Esta View implementa AdListener para receber os callbacks do SDK.
 */
class BannerAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), AdListener {

    private val TAG = "AdKen_BannerAdView"
    private var adUnitId: String? = null
    var adListener: AdListener? = null

    private val webView: WebView
    private val progressBar: ProgressBar

    init {
        // Criando a WebView e a ProgressBar programaticamente
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = object : WebViewClient() {
                // Pode adicionar lógica para monitorar impressões, cliques, etc.
            }
            visibility = View.GONE
        }

        progressBar = ProgressBar(context).apply {
            visibility = View.GONE
        }

        addView(webView)
        addView(progressBar)
    }

    /**
     * Define o ID do bloco de anúncio.
     * @param adUnitId O ID do bloco de anúncio.
     */
    fun setAdUnitId(adUnitId: String) {
        this.adUnitId = adUnitId
    }

    /**
     * Solicita o carregamento de um anúncio de banner.
     */
    fun loadAd() {
        adUnitId?.let {
            visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            webView.visibility = View.GONE
            AdKen.loadBannerAd(it, this) // Chama o SDK para carregar o anúncio
        } ?: run {
            Log.e(TAG, "adUnitId não foi definido. Chame setAdUnitId() antes de loadAd().")
            adListener?.onAdFailedToLoad(AdKenError.INVALID_REQUEST)
        }
    }

    override fun onAdLoaded(adData: AdData) {
        post {
            progressBar.visibility = View.GONE
            webView.visibility = View.VISIBLE
            if (adData.htmlContent != null) {
                // Carrega o conteúdo HTML do anúncio na WebView
                webView.loadDataWithBaseURL(null, adData.htmlContent, "text/html", "UTF-8", null)
            } else {
                // Lidar com anúncios baseados em imagem, se necessário
                // Exemplo: usar uma ImageView e uma biblioteca como Glide
            }
            adListener?.onAdLoaded(adData)
        }
    }



    override fun onAdFailedToLoad(error: AdKenError) {
        post {
            progressBar.visibility = View.GONE
            visibility = View.GONE
            Log.e(TAG, "Falha ao carregar anúncio: $error")
            adListener?.onAdFailedToLoad(error)
        }
    }

    override fun onAdClicked() {
        adListener?.onAdClicked()
        // Adicionar lógica de redirecionamento, se o URL for fornecido
    }

    override fun onAdImpression() {
        // ...
    }

    override fun onAdOpened() {
        // ...
    }

    override fun onAdClosed() {
        // ...
    }
}