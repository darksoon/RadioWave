package de.radiowave.core.player.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.radiowave.core.player.PlayerController
import de.radiowave.core.player.PlayerControllerImpl
import de.radiowave.core.player.RadioPlayerManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    abstract fun bindPlayerController(
        impl: PlayerControllerImpl,
    ): PlayerController

    companion object {
        @Provides
        @Singleton
        fun provideRadioPlayerManager(
            playerController: PlayerController,
        ): RadioPlayerManager {
            return RadioPlayerManager(playerController)
        }
    }
}
