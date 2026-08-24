package fm.libro.wearos.di

import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {
    @Singleton
    @Provides
    @IsEmulator
    fun isEmulator() = listOf(Build.PRODUCT, Build.MODEL).any { it.startsWith("sdk_gwear") }

}