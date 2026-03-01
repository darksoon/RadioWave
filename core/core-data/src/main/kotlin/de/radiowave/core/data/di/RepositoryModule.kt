package de.radiowave.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.radiowave.core.data.repository.CoverArtRepository
import de.radiowave.core.data.repository.CoverArtRepositoryImpl
import de.radiowave.core.data.repository.FavoriteRepository
import de.radiowave.core.data.repository.FavoriteRepositoryImpl
import de.radiowave.core.data.repository.OfflineFirstStationRepository
import de.radiowave.core.data.repository.RecentRepository
import de.radiowave.core.data.repository.RecentRepositoryImpl
import de.radiowave.core.data.repository.StationRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindStationRepository(
        impl: OfflineFirstStationRepository,
    ): StationRepository

    @Binds
    abstract fun bindFavoriteRepository(
        impl: FavoriteRepositoryImpl,
    ): FavoriteRepository

    @Binds
    abstract fun bindRecentRepository(
        impl: RecentRepositoryImpl,
    ): RecentRepository

    @Binds
    abstract fun bindCoverArtRepository(
        impl: CoverArtRepositoryImpl,
    ): CoverArtRepository
}
