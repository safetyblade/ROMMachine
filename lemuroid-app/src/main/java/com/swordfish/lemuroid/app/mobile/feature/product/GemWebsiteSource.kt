package com.swordfish.lemuroid.app.mobile.feature.product

import java.net.URI
import java.net.URLEncoder
import java.util.UUID

/** User-provided browser destinations. No site data is fetched by CARTDECK. */
internal data class GemWebsiteSource(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val home: String,
    val category: String,
    val template: String,
    val enabled: Boolean = true,
) {
    fun normalized(): GemWebsiteSource = copy(home = normalizeAddress(home), template = normalizeAddress(template))

    fun validationError(): String? =
        when {
            name.isBlank() || name.length > 80 -> "Enter a name of up to 80 characters."
            category.length > 60 -> "Use a category of up to 60 characters."
            validWebsite(home) == null -> "Enter a public HTTPS website address."
            template.isNotBlank() && searchUri("example") == null ->
                "Use the same HTTPS website and one {query} in a search parameter."
            else -> null
        }

    fun searchUri(query: String): URI? {
        val homeUri = validWebsite(home) ?: return null
        if (template.isBlank()) return homeUri
        if (query.isBlank() || query.length > 500) return null
        val slot = "{query}"
        val marker = "cartdeckqueryplaceholder193"
        if (template.indexOf(slot) < 0 || template.indexOf(slot) != template.lastIndexOf(slot)) return null
        if (template.contains(marker)) return null
        val parsed = validWebsite(template.replace(slot, marker)) ?: return null
        if (!parsed.host.equals(homeUri.host, ignoreCase = true)) return null
        if (effectivePort(parsed) != effectivePort(homeUri)) return null
        val parameters = parsed.rawQuery?.split("&").orEmpty()
        if (parameters.count { it.substringAfter("=", "").contains(marker) } != 1) return null
        if (parameters.any { it.substringBefore("=").contains(marker) }) return null
        if (parsed.rawPath.orEmpty().contains(marker) || parsed.rawAuthority.contains(marker)) return null
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        return validWebsite(template.replace(slot, encoded))
    }

    fun hostname(): String = validWebsite(home)?.host.orEmpty()

    companion object {
        private fun normalizeAddress(value: String): String {
            val trimmed = value.trim()
            if (trimmed.isEmpty() || trimmed.contains("://")) return trimmed
            if (trimmed.startsWith("//")) return "https:$trimmed"
            return "https://$trimmed"
        }

        private fun effectivePort(uri: URI) = if (uri.port == -1) 443 else uri.port

        private fun validWebsite(value: String): URI? {
            val address = normalizeAddress(value)
            if (address.length !in 1..2000 || address.any { it.isISOControl() || it.isWhitespace() }) return null
            val uri = runCatching { URI(address) }.getOrNull() ?: return null
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null || uri.fragment != null) {
                return null
            }
            val host = uri.host?.lowercase() ?: return null
            if (!host.contains('.') || host.endsWith('.') || host.all { it.isDigit() || it == '.' }) return null
            if (
                host.endsWith(".local") || host.endsWith(".localhost") || host.endsWith(".internal") ||
                host.endsWith(".home") || host.endsWith(".lan")
            ) {
                return null
            }
            if (uri.port != -1 && uri.port !in 1..65535) return null
            return uri
        }
    }
}
