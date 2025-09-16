package com.example.ikutio_mobile.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
// import androidx.security.crypto.MasterKey // ← 不要になるのでコメントアウトまたは削除
import androidx.security.crypto.MasterKeys // ★ こちらをインポート
import com.example.ikutio_mobile.data.local.AppDatabase
import com.example.ikutio_mobile.data.local.dao.LocationDao
import com.example.ikutio_mobile.data.remote.AuthApiService
import com.example.ikutio_mobile.data.remote.AuthInterceptor
import com.example.ikutio_mobile.data.remote.ProfileApiService
import com.example.ikutio_mobile.data.repository.LocationRepository
import com.example.ikutio_mobile.data.security.TokenManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import com.example.ikutio_mobile.data.remote.GameApiService

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val BASE_URL = "http://192.168.179.62:50052/"


    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        return EncryptedSharedPreferences.create(
            "secret_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideTokenManager(prefs: SharedPreferences): TokenManager {
        return TokenManager(prefs)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideRetrofit(authInterceptor: AuthInterceptor): Retrofit {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .addInterceptor(authInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileApiService(retrofit: Retrofit): ProfileApiService {
        return retrofit.create(ProfileApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ikutio_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocationDao(db: AppDatabase): LocationDao {
        return db.locationDao()
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        locationDao: LocationDao,
        gameApiService: GameApiService // 引数にgameApiServiceを追加
    ): LocationRepository {
        return LocationRepository(locationDao, gameApiService) // 両方を渡す
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideGameApiService(retrofit: Retrofit): GameApiService {
        return retrofit.create(GameApiService::class.java)
    }
}
