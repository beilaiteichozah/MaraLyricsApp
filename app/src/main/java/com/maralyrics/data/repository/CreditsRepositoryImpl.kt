package com.maralyrics.data.repository

import android.content.Context
import com.maralyrics.domain.model.CreditsData
import com.maralyrics.domain.repository.CreditsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CreditsRepository {

    override fun getCredits(): Flow<CreditsData> = flow {
        val creditsData = try {
            val jsonString = context.assets.open("credits.json").bufferedReader().use { it.readText() }
            Json.decodeFromString<CreditsData>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            CreditsData()
        }
        emit(creditsData)
    }
}
