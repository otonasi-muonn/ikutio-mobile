package com.example.ikutio_mobile.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.ikutio_mobile.data.local.AppDatabase
import com.example.ikutio_mobile.data.local.dao.LocationDao
import com.example.ikutio_mobile.data.remote.AuthApiService
import com.example.ikutio_mobile.data.remote.AuthInterceptor
import com.example.ikutio_mobile.data.remote.GameApiService
import com.example.ikutio_mobile.data.remote.MapsApiService
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
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val AUTH_BASE_URL = "http://192.168.179.62:50052/"
    private const val GAME_BASE_URL = "http://192.168.179.62:50056/"
    private const val MAPS_BASE_URL = "https://maps.googleapis.com/"

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
    @Named("AuthRetrofit")
    fun provideAuthRetrofit(authInterceptor: AuthInterceptor): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .addInterceptor(authInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(AUTH_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
    }

    @Provides
    @Singleton
    @Named("GameRetrofit")
    fun provideGameRetrofit(authInterceptor: AuthInterceptor): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .addInterceptor(authInterceptor)
            .build()
        return Retrofit.Builder()
            .baseUrl(GAME_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
    }

    @Provides
    @Singleton
    @Named("MapsRetrofit")
    fun provideMapsRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
        return Retrofit.Builder()
            .baseUrl(MAPS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(@Named("AuthRetrofit") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileApiService(@Named("AuthRetrofit") retrofit: Retrofit): ProfileApiService {
        return retrofit.create(ProfileApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGameApiService(@Named("GameRetrofit") retrofit: Retrofit): GameApiService {
        return retrofit.create(GameApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMapsApiService(@Named("MapsRetrofit") retrofit: Retrofit): MapsApiService {
        return retrofit.create(MapsApiService::class.java)
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
        gameApiService: GameApiService
    ): LocationRepository {
        return LocationRepository(locationDao, gameApiService)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}