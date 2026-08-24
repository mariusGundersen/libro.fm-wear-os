package fm.libro.wearos.di

import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.okhttp.NetworkAwareCallFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fm.libro.wearos.api.LibroFmApi
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://libro.fm/"
    private const val USER_AGENT = "okhttp/5.3.2"
    private const val APP_VERSION = "7.34.8"

    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
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

    @Provides
    @Singleton
    fun callFactory(
        okHttpClient: OkHttpClient,
    ): Call.Factory = okHttpClient

    @Provides
    @Singleton
    fun retrofit(
        callFactory: Call.Factory,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .callFactory(NetworkAwareCallFactory(callFactory, RequestType.ApiRequest))
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun libroFmApi(
        retrofit: Retrofit,
    ): LibroFmApi = retrofit.create(LibroFmApi::class.java)
}
