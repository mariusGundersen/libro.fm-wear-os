package fm.libro.wearos.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SuppressSpeakerPlayback

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsEmulator