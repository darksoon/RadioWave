// SPDX-License-Identifier: GPL-3.0-or-later
package de.darksoon.radiowave.core.data.repository

import androidx.test.core.app.ApplicationProvider
import de.darksoon.radiowave.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositorySnapshotTest {

    @Test fun `warm-up fills the snapshot cache so the blocking read is not repeated`() = runTest {
        val repository = CountingSettings(AppSettingsState(themeMode = AppSettings.THEME_DARK, allowMobileData = false))

        repository.warmUpInitialSnapshot()
        val first = repository.initialSnapshotBlocking()
        val second = repository.initialSnapshotBlocking()

        assertEquals(1, repository.reads)
        assertSame(first, second)
        assertEquals(AppSettings.THEME_DARK, first.themeMode)
        assertEquals(false, first.allowMobileData)
    }

    @Test fun `warm-up after a blocking read keeps the existing snapshot`() = runTest {
        val repository = CountingSettings(AppSettingsState(confirmRemoveFavorite = true))

        val blocking = repository.initialSnapshotBlocking()
        repository.warmUpInitialSnapshot()

        assertEquals(1, repository.reads)
        assertSame(blocking, repository.initialSnapshotBlocking())
    }
}

private class CountingSettings(
    private val state: AppSettingsState,
) : SettingsRepository(ApplicationProvider.getApplicationContext()) {
    var reads = 0
    override val data: Flow<AppSettingsState> = flow {
        reads++
        emit(state)
    }
}
