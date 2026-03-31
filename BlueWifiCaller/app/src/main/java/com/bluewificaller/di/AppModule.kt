package com.bluewificaller.di

import android.content.Context
import com.bluewificaller.service.AudioEngine
import com.bluewificaller.service.BluetoothManager
import com.bluewificaller.service.WifiDirectManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context): BluetoothManager =
        BluetoothManager(context)

    @Provides
    @Singleton
    fun provideWifiDirectManager(@ApplicationContext context: Context): WifiDirectManager =
        WifiDirectManager(context)

    @Provides
    @Singleton
    fun provideAudioEngine(): AudioEngine = AudioEngine()
}
