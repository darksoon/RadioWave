// SPDX-License-Identifier: GPL-3.0-or-later

package de.darksoon.radiowave.core.data.repository

import de.darksoon.radiowave.core.model.Station
import kotlinx.coroutines.flow.Flow

interface CustomStationRepository {
    fun getCustomStations(): Flow<List<Station>>
    suspend fun addCustomStation(station: Station)
    suspend fun deleteCustomStation(uuid: String)
}
