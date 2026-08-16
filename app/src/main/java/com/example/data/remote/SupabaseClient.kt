package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * SupabaseClient provides initialization and access to the Supabase backend services,
 * configured with the project URL and publishable key for database and storage operations.
 */
object SupabaseClient {

    /**
     * Supabase Project URL provided in the PRD.
     */
    const val SUPABASE_URL = "https://wbieadcwhteohabvrphh.supabase.co"

    /**
     * Supabase Publishable / Anonymous Key provided in the PRD.
     */
    const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_PoFac7Jv1vw4E4epfaxzsQ_rs4JLizz"

    /**
     * Storage Bucket Names
     */
    object StorageBuckets {
        const val PDFS = "pdfs"
        const val STUDY_MATERIALS = "study-materials"
        const val PDF_DOCS = "pdf-documents"
        const val BOOK_COVERS = "book-covers"
        const val USER_AVATARS = "user-avatars"
    }

    /**
     * Base URL ensured with trailing slash for Retrofit compatibility.
     */
    private val normalizedBaseUrl: String = if (SUPABASE_URL.endsWith("/")) SUPABASE_URL else "$SUPABASE_URL/"

    /**
     * Common authentication & headers interceptor for Supabase requests.
     */
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("apikey", SUPABASE_PUBLISHABLE_KEY)
            .header("Authorization", "Bearer $SUPABASE_PUBLISHABLE_KEY")

        if (original.header("Content-Type") == null) {
            requestBuilder.header("Content-Type", "application/json")
        }

        chain.proceed(requestBuilder.build())
    }

    /**
     * Logging interceptor for network debugging.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    /**
     * Shared OkHttpClient instance configured with authorization and timeouts.
     */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Moshi JSON serializer instance with Kotlin reflection adapter.
     */
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Retrofit REST API interface for database queries and updates.
     */
    val api: SupabaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApi::class.java)
    }

    // ------------------------------------------------------------------------
    // Supabase Storage Utilities
    // ------------------------------------------------------------------------

    /**
     * Constructs a public URL for a file stored in a Supabase Storage bucket.
     *
     * Example: https://wbieadcwhteohabvrphh.supabase.co/storage/v1/object/public/pdfs/class12/history_ch1.pdf
     */
    fun getStoragePublicUrl(bucket: String, path: String): String {
        val cleanPath = path.trimStart('/')
        val base = SUPABASE_URL.trimEnd('/')
        return "$base/storage/v1/object/public/$bucket/$cleanPath"
    }

    /**
     * Constructs an authenticated Storage REST endpoint for object operations.
     */
    fun getStorageObjectUrl(bucket: String, path: String): String {
        val cleanPath = path.trimStart('/')
        val base = SUPABASE_URL.trimEnd('/')
        return "$base/storage/v1/object/$bucket/$cleanPath"
    }

    /**
     * Generates a signed URL for a private file in a Supabase Storage bucket.
     */
    suspend fun getStorageSignedUrl(bucket: String, path: String, expiresIn: Int = 3600): String? {
        val cleanPath = path.trimStart('/')
        val base = SUPABASE_URL.trimEnd('/')
        val url = "$base/storage/v1/object/sign/$bucket/$cleanPath"
        
        return try {
            val json = """{"expiresIn": $expiresIn}"""
            val body = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: ""
                val signedUrlPath = org.json.JSONObject(respBody).optString("signedURL")
                if (signedUrlPath.isNotEmpty()) {
                    "$base$signedUrlPath"
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Constructs a direct REST query endpoint for database tables.
     */
    fun getDatabaseTableUrl(tableName: String): String {
        val base = SUPABASE_URL.trimEnd('/')
        return "$base/rest/v1/$tableName"
    }

    /**
     * Performs a direct GET request to download a file from Supabase Storage or CDN.
     */
    suspend fun downloadStorageFile(fileUrl: String): Response {
        val request = Request.Builder()
            .url(fileUrl)
            .get()
            .build()
        return okHttpClient.newCall(request).execute()
    }

    /**
     * Uploads bytes to a Supabase Storage bucket path with upsert enabled.
     */
    suspend fun uploadStorageFile(
        bucket: String,
        path: String,
        byteArray: ByteArray,
        contentType: String = "application/pdf"
    ): Response {
        val cleanPath = path.trimStart('/')
        val url = getStorageObjectUrl(bucket, cleanPath)
        val body = byteArray.toRequestBody(contentType.toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .header("x-upsert", "true")
            .header("Content-Type", contentType)
            .post(body)
            .build()
        return okHttpClient.newCall(request).execute()
    }

    /**
     * Checks if a file exists in Supabase storage and is readable.
     */
    suspend fun checkStorageFileExists(bucket: String, path: String): Boolean {
        val publicUrl = getStoragePublicUrl(bucket, path)
        return try {
            val req = Request.Builder()
                .url(publicUrl)
                .head()
                .build()
            val resp = okHttpClient.newCall(req).execute()
            resp.isSuccessful || resp.code in listOf(200, 206, 304)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes a file from Supabase Storage.
     */
    suspend fun deleteStorageFile(bucket: String, path: String): Boolean {
        val cleanPath = path.trimStart('/')
        val url = getStorageObjectUrl(bucket, cleanPath)
        return try {
            val request = Request.Builder()
                .url(url)
                .delete()
                .build()
            val resp = okHttpClient.newCall(request).execute()
            resp.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
