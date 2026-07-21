package com.apppair.di

import android.content.Context
import com.apppair.data.preferences.AppPairPreferences
import com.apppair.data.repository.AppRepository
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
    fun provideAppPairPreferences(
        @ApplicationContext context: Context
    ): AppPairPreferences {
        return AppPairPreferences(context)
    }

    @Provides
    @Singleton
    fun provideAppRepository(
        @ApplicationContext context: Context,
        preferences: AppPairPreferences
    ): AppRepository {
        return AppRepository(context, preferences)
    }
}
