// SPDX-License-Identifier: GPL-3.0-or-later

package de.radiowave.core.player.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import androidx.media3.common.util.UnstableApi
import de.radiowave.core.player.PlayerController
import de.radiowave.core.player.PlayerControllerImpl
import de.radiowave.core.player.RadioPlayerManager
import de.radiowave.core.player.StreamQualityResolver
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @UnstableApi
    abstract fun bindPlayerController(
        impl: PlayerControllerImpl,
    ): PlayerController

    companion object {
        @Provides
        @Singleton
        fun provideRadioPlayerManager(
            playerController: PlayerController,
            streamQualityResolver: StreamQualityResolver,
        ): RadioPlayerManager {
            return RadioPlayerManager(
                playerController = playerController,
                streamQualityResolver = streamQualityResolver,
            )
        }
    }
}

