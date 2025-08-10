// AdRequest.kt
package com.onzeAppsSdkManage.adken

/**
 * Representa uma solicitação de anúncio para o SDK da AdKen.
 *
 * @property adUnitId O ID do bloco de anúncio.
 * @property adType O tipo de anúncio solicitado (ex: "BANNER").
 * @property keywords Uma lista opcional de palavras-chave para segmentação.
 * @property contentUrl A URL do conteúdo da página onde o anúncio será exibido.
 * @property isTestRequest Flag para indicar se é uma requisição de teste.
 */
data class AdRequest(
    val adUnitId: String,
    val adType: String = "BANNER",
    val keywords: List<String>? = null,
    val contentUrl: String? = null,
    val isTestRequest: Boolean = true
)