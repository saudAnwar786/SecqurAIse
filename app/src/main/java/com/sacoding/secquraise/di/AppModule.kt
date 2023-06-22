package com.sacoding.secquraise.di

import android.app.Application
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.sacoding.secquraise.location.LocationTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(app: Application): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(app)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Provides
    @Singleton
    fun provideLocationTracker(
        context: Application,
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationTracker {
        return LocationTracker(context, fusedLocationProviderClient)
    }


}