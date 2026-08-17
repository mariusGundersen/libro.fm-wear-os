package fm.libro.wearos.api

import fm.libro.wearos.api.models.Audiobook
import fm.libro.wearos.api.models.DownloadManifest
import fm.libro.wearos.api.models.LibraryMetadata
import fm.libro.wearos.api.models.TokenMetadata
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface LibroFmApi {

    @FormUrlEncoded
    @POST("oauth/token")
    suspend fun login(
        @Field("grant_type") grantType: String = "password",
        @Field("username") username: String,
        @Field("password") password: String,
    ): TokenMetadata

    @GET("api/v7/library")
    suspend fun getLibrary(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
    ): LibraryMetadata

    @GET("api/v9/download-manifest")
    suspend fun getDownloadManifest(
        @Header("Authorization") auth: String,
        @Query("isbn") isbn: String,
    ): DownloadManifest
}
