package com.sage.control.di

import android.content.Context
import androidx.room.Room
import com.sage.control.data.api.OpenClawApi
import com.sage.control.data.db.SageDatabase
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SageDatabase {
        return Room.databaseBuilder(
            context,
            SageDatabase::class.java,
            "sage_control.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideOpenClawApi(
        @ApplicationContext context: Context
    ): OpenClawApi {
        return OpenClawApi(context)
    }
}