package com.maralyrics.laitei.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Guards the shape of the migration list, so a database version can't be raised without a step to
 * it. The steps themselves are run against real SQLite by `MaraLyricsMigrationTest` (androidTest).
 */
class MigrationChainTest {

    private val steps = MaraLyricsDatabase.ALL_MIGRATIONS.toList()

    @Test
    fun `there is exactly one step from every version to the next, starting at 1`() {
        val expected = (1..steps.size).map { it to it + 1 }

        assertEquals(expected, steps.map { it.startVersion to it.endVersion })
    }

    @Test
    fun `the chain reaches the newest exported schema`() {
        // Room writes schemas/<db class>/<version>.json at build time for the version in @Database,
        // so the highest file is the version the code is on. Raising it without adding a step here
        // leaves the chain short of it, and this fails instead of the app wiping data at runtime.
        val newest = exportedSchemaVersions().max()

        assertEquals(
            "@Database is at version $newest but the migrations end at ${steps.last().endVersion}",
            newest,
            steps.last().endVersion
        )
    }

    private fun exportedSchemaVersions(): List<Int> {
        val dir = listOf(
            File("schemas/${MaraLyricsDatabase::class.java.name}"),
            File("app/schemas/${MaraLyricsDatabase::class.java.name}")
        ).firstOrNull { it.isDirectory }
            ?: error("Exported Room schemas not found for ${MaraLyricsDatabase::class.java.name}")

        return dir.listFiles { f -> f.extension == "json" }!!
            .mapNotNull { it.nameWithoutExtension.toIntOrNull() }
            .sorted()
    }
}
