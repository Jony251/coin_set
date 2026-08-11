package com.example.coinset.api

import androidx.appcompat.app.AppCompatDelegate
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val BASE_URL = "https://coinset.bluecat.cc/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val token = TokenManager.getAccessToken()
        val request = if (token != null) {
            // .header (not addHeader) so a retried request from TokenAuthenticator
            // ends up with a single, current Authorization header, not a duplicate.
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    // Appends ?lang=ru|en|he to every request based on the current per-app
    // language (Settings > Language), so catalog text (coin names,
    // denominations, series, ruler/country names - which the backend
    // localizes server-side) comes back already in the right language
    // without every screen/repository having to pass it explicitly.
    private val langInterceptor = Interceptor { chain ->
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val lang = when {
            tag.startsWith("en") -> "en"
            tag.startsWith("he") || tag.startsWith("iw") -> "he"
            else -> "ru"
        }
        val urlWithLang = chain.request().url.newBuilder()
            .setQueryParameter("lang", lang)
            .build()
        chain.proceed(chain.request().newBuilder().url(urlWithLang).build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(langInterceptor)
        .authenticator(TokenAuthenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api: CoinsetApi = retrofit.create(CoinsetApi::class.java)
}
