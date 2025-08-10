// AdListener.kt
package com.onzeAppsSdkManage.adken

/**
 * Enum class para códigos de erro de anúncios, fornecendo um conjunto de erros bem definidos.
 */
enum class AdKenError {
    NO_FILL,
    NETWORK_ERROR,
    INVALID_REQUEST,
    SDK_NOT_INITIALIZED
}

/**
 * Interface para callbacks de eventos do ciclo de vida dos anúncios.
 */
interface AdListener {
    /**
     * Chamado quando um anúncio é carregado com sucesso.
     * @param adData Os dados do anúncio carregado.
     */
    fun onAdLoaded(adData: com.onzeAppsSdkManage.adken.internal.AdData)

    /**
     * Chamado quando um anúncio falha ao carregar.
     * @param error O código de erro que descreve a falha.
     */
    fun onAdFailedToLoad(error: AdKenError)

    fun onAdImpression()
    fun onAdOpened()
    fun onAdClicked()
    fun onAdClosed()
}