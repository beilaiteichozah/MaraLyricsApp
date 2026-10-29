package com.maralyrics.laitei.di

import com.maralyrics.laitei.BuildConfig
import com.maralyrics.laitei.data.remote.AccountApiService
import com.maralyrics.laitei.data.remote.CalendarApiService
import com.maralyrics.laitei.data.remote.MaraLyricsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            // Bodies carry user-submitted names/emails and, for Mara Idol, ~260 KB of base64
            // images per response — only worth logging while debugging.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(MaraLyricsApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): MaraLyricsApiService {
        return retrofit.create(MaraLyricsApiService::class.java)
    }

    @Provides
    @Singleton
    @Named(CALENDAR_RETROFIT)
    fun provideCalendarRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(CalendarApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideCalendarApiService(@Named(CALENDAR_RETROFIT) retrofit: Retrofit): CalendarApiService {
        return retrofit.create(CalendarApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAccountApiService(okHttpClient: OkHttpClient, json: Json): AccountApiService {
        // A client of its own without the body logger: sign-in requests carry the password and every
        // later one a token, and debug logging would print both.
        val client = okHttpClient.newBuilder()
            .apply { interceptors().removeAll { it is HttpLoggingInterceptor } }
            .build()
        return Retrofit.Builder()
            .baseUrl(MaraLyricsApiService.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AccountApiService::class.java)
    }

    private const val CALENDAR_RETROFIT = "calendar"
}
