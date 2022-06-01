package my.webnovels

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import my.webnovels.data.Repository
import my.webnovels.data.database.AppDatabase
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule
{
    const val mainDatabaseName = "bookEntry"

    @Provides
    @Singleton
    fun provideRepository(database: AppDatabase, @ApplicationContext context: Context): Repository
    {
        return Repository(database, context, mainDatabaseName)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase
    {
        return AppDatabase.createRoom(context, mainDatabaseName)
    }

    @Provides
    @Singleton
    fun provideAppPreferencies(@ApplicationContext context: Context): AppPreferences
    {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideAppCoroutineScope(): CoroutineScope
    {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("App"))
    }
}