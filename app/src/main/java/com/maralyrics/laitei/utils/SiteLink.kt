package com.maralyrics.laitei.utils

import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder

/**
 * Links to maralyrics.com that the app can open itself. Deliberately narrow — only song pages — so
 * the rest of the site (articles, terms, ...) keeps opening in the browser. The manifest's intent
 * filter and this parser have to agree; the filter decides which links reach the app at all.
 */
object SiteLink {

    private const val CANONICAL_HOST = "maralyrics.com"
    private val HOSTS = setOf(CANONICAL_HOST, "www.maralyrics.com")
    private val SCHEMES = setOf("https", "http")

    /**
     * The website page for a song — what Share sends. Opens in the app for people who have it
     * (see the manifest's intent filter) and on the website for everyone else.
     */
    fun songUrl(slug: String): String =
        // URI's multi-argument constructor percent-escapes whatever isn't legal in a path
        // (spaces, non-ASCII), which is exactly what a slug with such characters needs.
        URI("https", CANONICAL_HOST, "/song/$slug", null).toASCIIString()

    /** The slug in `https://maralyrics.com/song/{slug}`, or null for any other URL. */
    fun songSlug(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = try {
            URI(url.trim())
        } catch (e: URISyntaxException) {
            return null
        }
        if (uri.scheme?.lowercase() !in SCHEMES) return null
        if (uri.host?.lowercase() !in HOSTS) return null

        // rawPath, so an encoded "/" inside a slug can't be mistaken for a path separator.
        val segments = uri.rawPath.orEmpty().trim('/').split('/')
        if (segments.size != 2 || segments[0] != "song") return null

        // '+' is a literal plus in a path (only form data turns it into a space).
        val slug = try {
            URLDecoder.decode(segments[1].replace("+", "%2B"), "UTF-8")
        } catch (e: IllegalArgumentException) {
            return null
        }
        return slug.takeIf { it.isNotBlank() && '/' !in it }
    }
}
