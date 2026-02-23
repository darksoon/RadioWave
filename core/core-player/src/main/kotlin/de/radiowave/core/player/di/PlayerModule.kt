package de.radiowave.core.player.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.radiowave.core.player.PlayerController
import de.radiowave.core.player.PlayerControllerImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    abstract fun bindPlayerController(
        impl: PlayerControllerImpl,
    ): PlayerController
}
