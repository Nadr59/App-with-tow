package com.apppair.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // لا حاجة لدوال Provides هنا
    // لأن كلاFromClassين يستخدم @Inject constructor
    // Hilt سيكتشفهما تلقائياً
}
