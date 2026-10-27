package com.maralyrics.laitei.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SiteLinkTest {

    @Test
    fun `a song page link yields its slug`() {
        assertEquals("he-he-eima-rah", SiteLink.songSlug("https://maralyrics.com/song/he-he-eima-rah"))
    }

    @Test
    fun `www host, http scheme, trailing slash and host case are all accepted`() {
        assertEquals("a-b", SiteLink.songSlug("https://www.maralyrics.com/song/a-b"))
        assertEquals("a-b", SiteLink.songSlug("http://maralyrics.com/song/a-b"))
        assertEquals("a-b", SiteLink.songSlug("https://maralyrics.com/song/a-b/"))
        assertEquals("a-b", SiteLink.songSlug("https://MaraLyrics.COM/song/a-b"))
    }

    @Test
    fun `query string and fragment are not part of the slug`() {
        assertEquals("a-b", SiteLink.songSlug("https://maralyrics.com/song/a-b?utm_source=share"))
        assertEquals("a-b", SiteLink.songSlug("https://maralyrics.com/song/a-b#lyrics"))
        assertEquals("a-b", SiteLink.songSlug("  https://maralyrics.com/song/a-b  "))
    }

    @Test
    fun `percent-encoded slugs are decoded and a plus stays a plus`() {
        assertEquals("kô thiehpa", SiteLink.songSlug("https://maralyrics.com/song/k%C3%B4%20thiehpa"))
        assertEquals("a+b", SiteLink.songSlug("https://maralyrics.com/song/a+b"))
    }

    @Test
    fun `other pages on the site are left to the browser`() {
        assertNull(SiteLink.songSlug("https://maralyrics.com/"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/articles"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/article/updated-terms-privacy-policy"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/artist/hc-teresa-ngiano"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/terms"))
    }

    @Test
    fun `paths that only resemble a song page are rejected`() {
        assertNull(SiteLink.songSlug("https://maralyrics.com/song"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/song/"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/songs/a-b"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/song/a-b/extra"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/x/song/a-b"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/song/a%2Fb"))
    }

    @Test
    fun `look-alike hosts are rejected`() {
        assertNull(SiteLink.songSlug("https://maralyrics.com.evil.example/song/a-b"))
        assertNull(SiteLink.songSlug("https://evilmaralyrics.com/song/a-b"))
        assertNull(SiteLink.songSlug("https://maralyrics.co/song/a-b"))
        assertNull(SiteLink.songSlug("https://api.maralyrics.com/song/a-b"))
        assertNull(SiteLink.songSlug("https://example.com/maralyrics.com/song/a-b"))
        assertNull(SiteLink.songSlug("https://maralyrics.com@evil.example/song/a-b"))
    }

    @Test
    fun `share link is the canonical song page`() {
        assertEquals("https://maralyrics.com/song/he-he-eima-rah", SiteLink.songUrl("he-he-eima-rah"))
    }

    @Test
    fun `awkward slugs are escaped in the share link and read back unchanged`() {
        val slugs = listOf(
            "he-he-eima-rah",
            "kô thiehpa noh",        // space and non-ASCII
            "râh-lia",               // circumflex, common in Mara
            "a+b",                   // a plus is a plus, not a space
            "100%-song",             // a literal percent sign
            "what?-is-this",         // would otherwise start a query string
            "a#b",                   // would otherwise start a fragment
            "ei-cha-reithai-lymâ-aw"
        )
        for (slug in slugs) {
            val url = SiteLink.songUrl(slug)
            assertEquals("round trip of \"$slug\" via $url", slug, SiteLink.songSlug(url))
        }
    }

    @Test
    fun `share link stays plain ASCII so every messaging app keeps it clickable`() {
        val url = SiteLink.songUrl("kô thiehpa noh")

        assertTrue("not ASCII: $url", url.all { it.code < 128 })
        assertTrue("contains a raw space: $url", ' ' !in url)
    }

    @Test
    fun `non-web schemes and junk are rejected without throwing`() {
        assertNull(SiteLink.songSlug(null))
        assertNull(SiteLink.songSlug(""))
        assertNull(SiteLink.songSlug("   "))
        assertNull(SiteLink.songSlug("not a url"))
        assertNull(SiteLink.songSlug("ftp://maralyrics.com/song/a-b"))
        assertNull(SiteLink.songSlug("file:///song/a-b"))
        assertNull(SiteLink.songSlug("maralyrics.com/song/a-b"))
        assertNull(SiteLink.songSlug("https://maralyrics.com/song/%ZZ"))
    }
}
