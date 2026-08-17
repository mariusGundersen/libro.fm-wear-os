package fm.libro.wearos.api

import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.api.models.DownloadManifest
import fm.libro.wearos.api.models.LibraryMetadata
import fm.libro.wearos.api.models.TokenMetadata
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object LibroFmClient {

    private const val BASE_URL = "https://libro.fm/"
    private const val USER_AGENT = "okhttp/5.3.2"
    private const val APP_VERSION = "7.34.8"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("X-LibroFm-AppVer", APP_VERSION)
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val api: LibroFmApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibroFmApi::class.java)
    }

    suspend fun login(username: String, password: String): TokenMetadata {
        return api.login(username = username, password = password)
    }

    suspend fun getLibrary(token: String, page: Int = 1): LibraryMetadata {
        return api.getLibrary(auth = "Bearer $token", page = page)
    }

    suspend fun getAllLibraryBooks(token: String): List<Audiobook> {
        val books = mutableListOf<Audiobook>()
        var page = 1
        var totalPages = 1
        while (page <= totalPages) {
            val response = getLibrary(token, page)
            books.addAll(response.audiobooks)
            totalPages = response.totalPages
            page++
        }
        return books
    }

    suspend fun getDownloadManifest(token: String, isbn: String): DownloadManifest {
        return api.getDownloadManifest(auth = "Bearer $token", isbn = isbn)
    }
}
