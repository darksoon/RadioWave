# 🤖 Agent Instructions — RadioWave Android App

## Project Overview

**RadioWave** — A free, open-source Internet Radio app for Android built with Kotlin and Jetpack Compose.

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture with MVVM
- **Build System**: Gradle (Kotlin DSL)
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)

## Build Commands

### Essential Commands

```bash
# Build the entire project
./gradlew build

# Build specific module
./gradlew :app:build
./gradlew :core:core-model:build

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :core:core-model:test
./gradlew :feature:feature-home:test

# Run a single test class
./gradlew :core:core-model:test --tests "StationRepositoryTest"

# Run a single test method
./gradlew :core:core-model:test --tests "StationRepositoryTest.testSearchStations"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run instrumented test for specific module
./gradlew :core:core-database:connectedAndroidTest

# Clean build
./gradlew clean

# Assemble debug APK
./gradlew assembleDebug

# Assemble release APK
./gradlew assembleRelease

# Install debug APK to connected device
./gradlew installDebug

# Run lint checks
./gradlew lint

# Generate lint report
./gradlew lintDebug

# Check code style (ktlint if configured)
./gradlew ktlintCheck

# Auto-format code (ktlint if configured)
./gradlew ktlintFormat
```

### Module-Specific Commands

```bash
# Build build-logic first (critical!)
./gradlew :build-logic:build

# Test core modules
./gradlew :core:core-database:test
./gradlew :core:core-network:test
./gradlew :core:core-data:test
./gradlew :core:core-player:test

# Test feature modules
./gradlew :feature:feature-home:test
./gradlew :feature:feature-player:test
./gradlew :feature:feature-favorites:test
```

## Code Style Guidelines

### Kotlin Style

- **Language**: Kotlin 2.1.0+
- **Style Guide**: [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Max line length**: 120 characters
- **Indentation**: 4 spaces (no tabs)
- **Trailing commas**: Required

### Naming Conventions

```kotlin
// Classes & Interfaces: PascalCase
class StationRepositoryImpl
interface StationRepository

// Functions & Variables: camelCase
fun searchStations(query: String)
val stationList: List<Station>

// Constants: UPPER_SNAKE_CASE (top-level or companion object)
const val MAX_RETRY_COUNT = 3

// Compose Functions: PascalCase
@Composable
fun StationCard(station: Station)

// ViewModels: Suffix with ViewModel
class HomeViewModel @Inject constructor(
    private val getTopStationsUseCase: GetTopStationsUseCase
) : ViewModel()

// Use Cases: Suffix with UseCase
class GetTopStationsUseCase @Inject constructor(
    private val repository: StationRepository
)
```

### Imports

```kotlin
// Group imports: Kotlin stdlib, Android, Third-party, Project
import kotlin.coroutines.CoroutineContext
import android.content.Context
import androidx.compose.runtime.Composable
import javax.inject.Inject
import de.radiowave.core.model.Station

// No wildcard imports (except Compose)
import androidx.compose.material3.*  // OK for Compose
```

### Architecture Patterns

```kotlin
// MVVM: ViewModel exposes StateFlow
class HomeViewModel @Inject constructor(
    private val getTopStationsUseCase: GetTopStationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadStations() {
        viewModelScope.launch {
            getTopStationsUseCase()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { error -> 
                    _uiState.update { it.copy(error = error.toUiError(), isLoading = false) }
                }
                .collect { stations ->
                    _uiState.update { it.copy(stations = stations, isLoading = false) }
                }
        }
    }
}

// Immutable State
data class HomeUiState(
    val stations: List<Station> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiError? = null
)

// Sealed class for Errors
sealed class PlayerError {
    object NetworkError : PlayerError()
    object StreamBroken : PlayerError()
    data class Unknown(val message: String) : PlayerError()
}
```

### Compose Guidelines

```kotlin
// Preview required for every UI component
@Preview(showBackground = true)
@Composable
private fun StationCardPreview() {
    RadioWaveTheme {
        StationCard(
            station = Station(
                uuid = "test-uuid",
                name = "Test Radio",
                streamUrl = "https://example.com/stream"
            )
        )
    }
}

// State hoisting
@Composable
fun StationCard(
    station: Station,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
)

// Use Modifier as last parameter with default
@Composable
fun StationList(
    stations: List<Station>,
    modifier: Modifier = Modifier
)
```

### Error Handling

```kotlin
// Use Result type or sealed classes, never swallow exceptions
suspend fun getStations(): Result<List<Station>> = try {
    val stations = api.getTopStations()
    Result.success(stations.map { it.toDomain() })
} catch (e: IOException) {
    Result.failure(PlayerError.NetworkError)
} catch (e: Exception) {
    Result.failure(PlayerError.Unknown(e.message ?: "Unknown error"))
}

// Flow error handling
fun searchStations(query: String): Flow<List<Station>> = flow {
    emit(api.searchByName(query).map { it.toDomain() })
}.catch { error ->
    emit(emptyList())
    // Log error but don't crash
}
```

### Testing

```kotlin
// Unit tests with Turbine for Flow testing
@Test
fun `search stations emits loading then success`() = runTest {
    val viewModel = HomeViewModel(getTopStationsUseCase)
    
    viewModel.uiState.test {
        assertEquals(HomeUiState(isLoading = true), awaitItem())
        assertEquals(HomeUiState(stations = fakeStations), awaitItem())
    }
}

// Repository tests
@Test
fun `get favorites returns flow of stations`() = runTest {
    val favorites = repository.getFavorites().first()
    assertEquals(2, favorites.size)
}

// DAO tests (instrumented)
@Test
fun insertAndRetrieveFavorite() = runBlocking {
    val favorite = FavoriteEntity(stationUuid = "test-uuid")
    dao.addFavorite(favorite)
    
    val result = dao.getAllFavorites().first()
    assertEquals(1, result.size)
}
```

### Documentation

```kotlin
/**
 * Repository for managing radio stations.
 * 
 * @property api The Radio Browser API client
 * @property dao The local database DAO
 */
interface StationRepository {
    /**
     * Searches for stations by name.
     * 
     * @param query The search query
     * @return Flow of matching stations
     */
    fun searchStations(query: String): Flow<List<Station>>
}
```

## Module Dependencies

Follow the strict build order:

1. **build-logic** (Convention Plugins - MUST BUILD FIRST)
2. **core-model** (No dependencies)
3. **core-database** (core-model)
4. **core-network** (core-model)
5. **core-data** (core-model, core-database, core-network)
6. **core-player** (core-model, core-data)
7. **core-cast** (core-player)
8. **core-ui** (core-model)
9. **feature-*** (core-ui, core-data, core-player)
10. **app** (All modules)
11. **auto** (Android Auto)

## Security Rules

1. **No hardcoded secrets** — Use BuildConfig fields from environment
2. **Input validation** — Validate all URLs and user inputs
3. **HTTPS only** — All API calls must use HTTPS
4. **No logging in release** — Use BuildConfig.DEBUG checks
5. **ProGuard rules** — Keep Room entities and Retrofit interfaces

## Pre-commit Checklist

Before committing code:

1. Run `./gradlew :build-logic:build` (if build-logic changed)
2. Run `./gradlew ktlintCheck` (formatting)
3. Run `./gradlew lint` (Android lint)
4. Run `./gradlew test` (unit tests)
5. Run affected module tests: `./gradlew :<module>:test`
6. Verify Compose Previews render correctly
