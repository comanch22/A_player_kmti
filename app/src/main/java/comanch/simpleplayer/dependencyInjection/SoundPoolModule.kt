package comanch.simpleplayer.dependencyInjection

import android.media.AudioAttributes
import android.media.SoundPool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.scopes.FragmentScoped

@Module
@InstallIn(FragmentComponent::class)
class SoundPoolModule {

    @FragmentScoped
    @Provides
    fun provideSoundPool(): SoundPool {
        return SoundPool.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).setMaxStreams(1)
            .build()
    }
}