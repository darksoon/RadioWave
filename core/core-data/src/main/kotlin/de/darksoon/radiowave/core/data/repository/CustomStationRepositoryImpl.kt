// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.data.mapper.toDomain
import de.darksoon.radiowave.core.data.mapper.toCustomEntity
import de.darksoon.radiowave.core.database.dao.CustomStationDao
import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomStationRepositoryImpl @Inject constructor(
    private val customStationDao: CustomStationDao,
) : CustomStationRepository {

    override fun getCustomStations(): Flow<List<Station>> =
        customStationDao.getAllCustomStations().map { list -> list.map { it.toDomain() } }

    override suspend fun addCustomStation(station: Station) {
        customStationDao.insertCustomStation(station.toCustomEntity())
    }

    override suspend fun deleteCustomStation(uuid: String) {
        customStationDao.deleteByUuid(uuid)
    }
}
