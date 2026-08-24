package com.maralyrics.laitei.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialLinksSerializerTest {

    @Test
    fun `test parsing proper JSON object`() {
        val json = """{"facebook": "https://facebook.com/artist", "twitter": "https://twitter.com/artist"}"""
        val result = Json.decodeFromString(SocialLinksSerializer, json)
        assertEquals("https://facebook.com/artist", result?.get("facebook"))
        assertEquals("https://twitter.com/artist", result?.get("twitter"))
    }

    @Test
    fun `test parsing JSON array of URLs`() {
        val json = """["https://facebook.com/artist", "https://instagram.com/artist"]"""
        val result = Json.decodeFromString(SocialLinksSerializer, json)
        assertEquals("https://facebook.com/artist", result?.get("facebook"))
        assertEquals("https://instagram.com/artist", result?.get("instagram"))
    }

    @Test
    fun `test parsing double-encoded JSON array string`() {
        val json = """"[\"https://facebook.com/artist\"]""""
        val result = Json.decodeFromString(SocialLinksSerializer, json)
        assertEquals("https://facebook.com/artist", result?.get("facebook"))
    }

    @Test
    fun `test parsing single URL string`() {
        val json = """"https://youtube.com/artist""""
        val result = Json.decodeFromString(SocialLinksSerializer, json)
        assertEquals("https://youtube.com/artist", result?.get("youtube"))
    }

    @Test
    fun `test parsing unknown platform URL`() {
        val json = """"https://example.com/artist""""
        val result = Json.decodeFromString(SocialLinksSerializer, json)
        assertEquals("https://example.com/artist", result?.get("website"))
    }

    @Test
    fun `test parsing empty string`() {
        val json = """" """"
        val result = Json.decodeFromString(SocialLinksSerializer, json)
        assertEquals(null, result)
    }

    @Test
    fun `test parsing string with literal brackets`() {
        val json = """"[\"https://facebook.com/artist\"]""""
        val result = Json.decodeFromString(SocialLinksSerializer, json)
        assertEquals("https://facebook.com/artist", result?.get("facebook"))
    }

    @Test
    fun `test ArtistDto with various social_links formats`() {
        val jsonObject = """{"id": 1, "slug": "artist", "name": "Artist", "social_links": {"facebook": "fb"}}"""
        val artist1 = Json.decodeFromString<ArtistDto>(jsonObject)
        assertEquals("fb", artist1.socialLinks?.get("facebook"))

        val jsonArray = """{"id": 1, "slug": "artist", "name": "Artist", "social_links": ["https://twitter.com/t"]}"""
        val artist2 = Json.decodeFromString<ArtistDto>(jsonArray)
        assertEquals("https://twitter.com/t", artist2.socialLinks?.get("twitter"))

        val jsonString = """{"id": 1, "slug": "artist", "name": "Artist", "social_links": "https://instagram.com/i"}"""
        val artist3 = Json.decodeFromString<ArtistDto>(jsonString)
        assertEquals("https://instagram.com/i", artist3.socialLinks?.get("instagram"))
    }
}
