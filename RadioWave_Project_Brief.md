📻 RadioWave – Project Brief & Architecture (v2.1)

    Projekt: RadioWave – Freie Internet-Radio App für Android
    Erstellt: 2026-02-23
    Ziel: Übergabe an Multi-Agenten-System (OpenClaw) zur modularen Umsetzung

1. Vision & Grundprinzipien

RadioWave ist eine Internet-Radio-App für Android, die sich durch folgende Prinzipien von TuneIn, Radioplayer & Co. abhebt:

    Kein Account – Null Registrierung, alle Daten lokal auf dem Gerät

    Keine Werbung – Komplett werbefrei, kein Tracking, kein Analytics

    Kein Abo – Vollständig kostenlos, Open Source (MIT License)

    Privacy First – Keine Datenerhebung, kein Netzwerk außer Streaming-URLs

    Stylisch & Intuitiv – Material You Design, smooth Animationen, Dark/Light

2. Tech-Stack (Festgelegt)
Komponente	Technologie	Begründung
Sprache	Kotlin	Industriestandard für Android
UI Framework	Jetpack Compose + Material 3	Modern, deklarativ, Material You Support
Audio Player	AndroidX Media3 (ExoPlayer)	Industriestandard für Streaming, HLS/DASH/ICY Support
Background & Auto	MediaLibraryService (Media3)	Foreground Service + MediaSession + Android Auto Browse-Tree
Chromecast	Media3 Cast Extension	Native Cast-Integration für Smart Speaker
Lokale DB	Room (SQLite)	Offline-fähig, reaktiv mit Flow
DI	Hilt (Dagger)	Standard für Android DI
Navigation	Compose Navigation	Single-Activity-Architecture
Async	Kotlin Coroutines + Flow	Reaktive Datenströme
Build	Gradle (Kotlin DSL)	Convention Plugins für Module
Min SDK	26 (Android 8.0)	~97% Abdeckung, MediaSession Support
Target SDK	35 (Android 15)	Aktuellste API
3. Architektur-Übersicht

┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │  Home    │  │  Browse  │  │ Favorites │  │ Player  │ │
│  │  Screen  │  │  Screen  │  │  Screen   │  │  Sheet  │ │
│  └────┬─────┘  └────┬─────┘  └────┬──────┘  └────┬────┘ │
│       └──────────────┼───────────┬─┘              │      │
│               ┌──────┴───────┐   │                │      │
│               │  ViewModels  │   │                │      │
│               └──────┬───────┘   │                │      │
├──────────────────────┼───────────┼────────────────┼──────┤
│                    Domain Layer   │                │      │
│  ┌────────────────┐  ┌──────────┴──────┐  ┌──────┴────┐ │
│  │  Use Cases     │  │  Repositories   │  │  Player   │ │
│  │  (Interactors) │  │  (Interfaces)   │  │  Manager  │ │
│  └────────┬───────┘  └────────┬────────┘  └──────┬────┘ │
├───────────┼───────────────────┼───────────────────┼──────┤
│                     Data Layer                    │      │
│  ┌────────┴───────┐  ┌───────┴────────┐  ┌───────┴────┐ │
│  │  Room DB       │  │  Radio API     │  │  Media3    │ │
│  │  (Stations,    │  │  (Community    │  │  Library  │ │
│  │   Favorites,   │  │   Radio DB)    │  │  Service   │ │
│  │   History)     │  │                │  │            │ │
│  └────────────────┘  └────────────────┘  └────────────┘ │
│                                                          │
│  ┌──────────────────────────────────────────────────────┐│
│  │       Android Auto (Browse) & Chromecast (Cast)       ││
│  └──────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────┘

Architektur-Muster: Clean Architecture mit MVVM (Model-View-ViewModel)
Single Activity: Ja – eine MainActivity, alles über Compose Navigation
4. Modul-Struktur (Gradle Multi-Module)

radiowave/
├── app/                          # Modul 0: App Shell & DI
├── build-logic/                  # Modul 1: Convention Plugins (MUSS ZUERST GEBAUT WERDEN)
├── core/
│   ├── core-model/               # Modul 2: Datenmodelle
│   ├── core-database/            # Modul 3: Room DB
│   ├── core-network/             # Modul 4: Radio-Browser API Client
│   ├── core-player/              # Modul 5: Media3 Player & Library Service
│   ├── core-cast/                # Modul 6: Chromecast Integration
│   ├── core-data/                # Modul 7: Repositories
│   └── core-ui/                  # Modul 8: Shared Compose Components
├── feature/
│   ├── feature-home/             # Modul 9: Home Screen
│   ├── feature-browse/           # Modul 10: Browse & Search
│   ├── feature-favorites/        # Modul 11: Favorites
│   ├── feature-player/           # Modul 12: Player UI (Bottom Sheet + Fullscreen)
│   ├── feature-custom-stations/  # Modul 13: Custom Stations hinzufügen
│   ├── feature-alarm/            # Modul 14: Wecker / AlarmManager
│   └── feature-settings/         # Modul 15: Einstellungen
└── auto/                         # Modul 16: Android Auto Integration

5. Module im Detail
MODUL 0: app – App Shell

Verantwortung: Application-Klasse, Hilt Setup, Navigation Graph, MainActivity

Dateien:

app/src/main/
├── java/de/radiowave/
│   ├── RadioWaveApp.kt          # @HiltAndroidApp
│   ├── MainActivity.kt          # Single Activity, setzt ComposeContent
│   └── navigation/
│       └── RadioWaveNavHost.kt  # NavHost mit allen Feature-Routes
├── res/
│   └── values/themes.xml        # Material You Theme
└── AndroidManifest.xml

Dependencies: Alle Feature-Module, core-ui, core-player, Hilt
MODUL 1: build-logic – Convention Plugins ⭐ STARTPUNKT

Verantwortung: Zentrale Build-Konfiguration, um Gradle-Boilerplate in den Modulen zu vermeiden.
Dieses Modul muss funktionsfähig sein, bevor irgendein Feature-Code geschrieben wird.

    AndroidApplicationConventionPlugin

    AndroidLibraryComposeConventionPlugin

    HiltConventionPlugin

MODUL 2: core-model – Datenmodelle

Verantwortung: Reine Kotlin-Datenklassen, keine Android-Dependencies
Kotlin

// Station.kt
data class Station(
    val uuid: String,              // Unique ID (von API oder lokal generiert)
    val name: String,
    val streamUrl: String,         // Primäre Stream-URL
    val homepageUrl: String?,
    val faviconUrl: String?,       // Sender-Logo
    val country: String?,
    val countryCode: String?,
    val language: String?,
    val tags: List<String>,        // Genre-Tags
    val codec: String?,            // MP3, AAC, OGG, FLAC etc.
    val bitrate: Int?,             // kbps
    val isCustom: Boolean = false, // Vom User hinzugefügt?
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val addedAt: Long = System.currentTimeMillis()
)

// PlayerState.kt
data class PlayerState(
    val currentStation: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoading: Boolean = false,
    val error: PlayerError? = null,
    val metadata: StreamMetadata? = null  // ICY Metadata (Song-Titel etc.)
)

// PlayerError.kt (Sealed Class für exaktes Error-Handling)
sealed class PlayerError {
    object NetworkError : PlayerError()
    object StreamBroken : PlayerError() // 404 oder Timeout
    data class Unknown(val message: String) : PlayerError()
}

// StreamMetadata.kt
data class StreamMetadata(
    val title: String?,            // Aktueller Song/Show
    val artist: String?,
    val albumArtUrl: String?
)

// Genre.kt
data class Genre(
    val name: String,
    val stationCount: Int
)

// Country.kt  
data class Country(
    val name: String,
    val code: String,
    val stationCount: Int
)

MODUL 3: core-database – Room Database

Verantwortung: Lokale Persistenz für Favoriten, Custom Stations, History, Cache
Kotlin

// RadioWaveDatabase.kt
@Database(
    entities = [
        StationEntity::class,
        FavoriteEntity::class,
        RecentEntity::class,
        CustomStationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RadioWaveDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun customStationDao(): CustomStationDao
}

// StationEntity.kt – Cached Stations von der API
@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val uuid: String,
    val name: String,
    val streamUrl: String,
    val homepageUrl: String?,
    val faviconUrl: String?,
    val country: String?,
    val countryCode: String?,
    val language: String?,
    val tags: String?,              // Komma-separiert
    val codec: String?,
    val bitrate: Int?,
    val cachedAt: Long              // Wann gecached
)

// FavoriteEntity.kt
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val stationUuid: String,
    val addedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0          // Für manuelle Sortierung
)

// RecentEntity.kt – Zuletzt gehört
@Entity(tableName = "recent_stations")
data class RecentEntity(
    @PrimaryKey val stationUuid: String,
    val lastPlayedAt: Long,
    val playCount: Int = 1
)

// CustomStationEntity.kt – Vom User manuell hinzugefügt
@Entity(tableName = "custom_stations")
data class CustomStationEntity(
    @PrimaryKey val uuid: String,   // Lokal generierte UUID
    val name: String,
    val streamUrl: String,
    val homepageUrl: String?,
    val faviconUrl: String?,
    val genre: String?,
    val country: String?,
    val addedAt: Long = System.currentTimeMillis()
)

// DAOs
@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY sortOrder ASC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun removeFavorite(favorite: FavoriteEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE stationUuid = :uuid)")
    fun isFavorite(uuid: String): Flow<Boolean>
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recent_stations ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 50): Flow<List<RecentEntity>>

    @Upsert
    suspend fun upsertRecent(recent: RecentEntity)

    @Query("DELETE FROM recent_stations")
    suspend fun clearHistory()
}

MODUL 4: core-network – Radio-Browser API Client

Verantwortung: Anbindung an die Community-API radio-browser.info

Wichtig: radio-browser.info ist eine freie, community-betriebene API mit ~45.000 Sendern. Kein API-Key nötig!
Kotlin

// RadioBrowserApi.kt (Retrofit)
interface RadioBrowserApi {

    // Sender nach Name suchen
    @GET("json/stations/byname/{name}")
    suspend fun searchByName(
        @Path("name") name: String,
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<RadioBrowserStation>

    // Sender nach Tag/Genre
    @GET("json/stations/bytag/{tag}")
    suspend fun searchByTag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int = 50,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioBrowserStation>

    // Sender nach Land
    @GET("json/stations/bycountrycodeexact/{code}")
    suspend fun searchByCountry(
        @Path("code") countryCode: String,
        @Query("limit") limit: Int = 100,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioBrowserStation>

    // Top-Sender (beliebteste)
    @GET("json/stations/topclick/{count}")
    suspend fun getTopStations(
        @Path("count") count: Int = 100
    ): List<RadioBrowserStation>

    // Alle Genres/Tags
    @GET("json/tags")
    suspend fun getTags(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 100
    ): List<RadioBrowserTag>

    // Alle Länder
    @GET("json/countries")
    suspend fun getCountries(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioBrowserCountry>

    // Click zählen (fair use – zeigt der Community dass der Sender genutzt wird)
    @POST("json/url/{stationuuid}")
    suspend fun registerClick(@Path("stationuuid") uuid: String)

    // Defekten Stream an die Community melden (Optional, nutzt vote endpoint)
    @POST("json/vote/station/{stationuuid}")
    suspend fun reportBrokenStream(@Path("stationuuid") uuid: String)
}

// RadioBrowserStation.kt (API Response DTO)
@Serializable
data class RadioBrowserStation(
    @SerialName("stationuuid") val uuid: String,
    val name: String,
    @SerialName("url_resolved") val urlResolved: String,  // Aufgelöste Stream-URL
    val url: String,
    val homepage: String?,
    val favicon: String?,
    val country: String?,
    @SerialName("countrycode") val countryCode: String?,
    val language: String?,
    val tags: String?,              // Komma-separiert
    val codec: String?,
    val bitrate: Int?,
    @SerialName("clickcount") val clickCount: Int?,
    val votes: Int?
)

MODUL 5: core-player – Media3 Player & Service ⭐ KERNMODUL

Verantwortung: Audio-Playback, Foreground Service, Lock Screen, Broken-Link-Fallback. Nutzt zwingend MediaLibraryService für Android Auto Kompatibilität.
Kotlin

// RadioPlaybackService.kt
@AndroidEntryPoint
class RadioPlaybackService : MediaLibraryService() {

    private var mediaLibrarySession: MediaLibrarySession? = null
    @Inject lateinit var playerManager: PlayerManager

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true) // Pause bei Kopfhörer-Abziehen
            .build()

        // Callback übernimmt den Android Auto Browse Tree
        mediaLibrarySession = MediaLibrarySession.Builder(this, player, RadioMediaSessionCallback())
            .build()

        playerManager.initialize(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}

// PlayerManager.kt
@Singleton
class PlayerManager @Inject constructor(
    private val stationRepository: StationRepository,
    private val recentDao: RecentDao
) {
    private var _player: ExoPlayer? = null
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun initialize(player: ExoPlayer) {
        _player = player
        
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(state: Int) {
                _playerState.update {
                    it.copy(
                        isBuffering = state == Player.STATE_BUFFERING,
                        isLoading = state == Player.STATE_BUFFERING && it.currentStation != null
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val playerError = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> PlayerError.NetworkError
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> PlayerError.StreamBroken
                    else -> PlayerError.Unknown(error.message ?: "Unknown Error")
                }
                
                _playerState.update { it.copy(error = playerError, isPlaying = false, isBuffering = false) }
            }

            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                // ICY Metadata (Song-Titel aus dem Stream)
                _playerState.update {
                    it.copy(
                        metadata = StreamMetadata(
                            title = metadata.title?.toString(),
                            artist = metadata.artist?.toString(),
                            albumArtUrl = metadata.artworkUri?.toString()
                        )
                    )
                }
            }
        })
    }

    suspend fun playStation(station: Station) {
        _playerState.update { it.copy(currentStation = station, error = null) }
        
        val mediaItem = MediaItem.Builder()
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtworkUri(station.faviconUrl?.toUri())
                    .build()
            )
            .build()

        _player?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }

        // In History speichern
        recentDao.upsertRecent(
            RecentEntity(
                stationUuid = station.uuid,
                lastPlayedAt = System.currentTimeMillis()
            )
        )
    }

    fun togglePlayPause() {
        _player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun stop() {
        _player?.stop()
        _playerState.update { PlayerState() }
    }
}

// AndroidManifest.xml Ergänzungen:
// <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
// <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
// <uses-permission android:name="android.permission.INTERNET" />
//
// <service
//     android:name=".player.RadioPlaybackService"
//     android:foregroundServiceType="mediaPlayback"
//     android:exported="true">
//     <intent-filter>
//         <action android:name="androidx.media3.session.MediaLibraryService" />
//         <action android:name="android.media.browse.MediaBrowserService" />
//     </intent-filter>
// </service>

MODUL 6: core-cast – Chromecast Integration

Verantwortung: Nahtlose Übergabe des Streams an Google Cast fähige Geräte via androidx.media3:media3-cast. Stellt den State für den Cast-Button in der UI bereit.
MODUL 7: core-data – Repositories

Verantwortung: Koordiniert Datenquellen (API + Lokale DB), Single Source of Truth
Kotlin

// StationRepository.kt
interface StationRepository {
    fun searchStations(query: String): Flow<List<Station>>
    fun getTopStations(): Flow<List<Station>>
    fun getStationsByCountry(countryCode: String): Flow<List<Station>>
    fun getStationsByTag(tag: String): Flow<List<Station>>
    fun getTags(): Flow<List<Genre>>
    fun getCountries(): Flow<List<Country>>
    suspend fun registerClick(stationUuid: String)
    suspend fun reportBrokenStream(stationUuid: String)
}

// FavoriteRepository.kt
interface FavoriteRepository {
    fun getFavorites(): Flow<List<Station>>
    fun isFavorite(uuid: String): Flow<Boolean>
    suspend fun toggleFavorite(station: Station)
    suspend fun reorderFavorites(stationIds: List<String>)
}

// RecentRepository.kt
interface RecentRepository {
    fun getRecentStations(limit: Int = 50): Flow<List<Station>>
    suspend fun clearHistory()
}

// CustomStationRepository.kt
interface CustomStationRepository {
    fun getCustomStations(): Flow<List<Station>>
    suspend fun addStation(name: String, streamUrl: String, genre: String?, country: String?)
    suspend fun updateStation(station: Station)
    suspend fun deleteStation(uuid: String)
    suspend fun validateStreamUrl(url: String): Boolean
}

MODUL 8: core-ui – Shared Compose Components

Verantwortung: Wiederverwendbare UI-Bausteine

core-ui/
├── components/
│   ├── StationCard.kt              # Sender-Karte mit Logo, Name, Genre, Fav-Button
│   ├── StationListItem.kt          # Kompakte Listen-Ansicht
│   ├── MiniPlayer.kt               # Bottom-Bar Mini-Player (immer sichtbar wenn was läuft)
│   ├── PlayButton.kt               # Animierter Play/Pause Button
│   ├── CastButton.kt               # Chromecast Button
│   ├── EqualizerAnimation.kt       # Animierte Equalizer-Bars (Playing-Indikator)
│   ├── GenreChip.kt                # Tag/Genre Chip
│   ├── CountryFlag.kt              # Länderflagge Emoji/Icon
│   ├── SearchBar.kt                # Suchleiste mit Debounce
│   ├── EmptyState.kt               # Leere-Ansicht Placeholder
│   ├── ErrorState.kt               # Fehler-Anzeige mit Retry / Stream broken report
│   ├── LoadingState.kt             # Shimmer Loading Skeleton
│   └── StreamQualityBadge.kt       # Bitrate/Codec Badge
├── theme/
│   ├── RadioWaveTheme.kt           # Material You Dynamic Colors
│   ├── Color.kt                    # Fallback Farbpalette
│   ├── Typography.kt               # Schriftarten (Inter oder Product Sans)
│   └── Shape.kt                    # Abgerundete Ecken etc.
└── util/
    ├── ImageLoader.kt              # Coil AsyncImage Wrapper für Station-Logos
    └── Modifiers.kt                # Custom Modifiers (Glassmorphism, etc.)

MODUL 9: feature-home – Home Screen

Verantwortung: Landing Page, Schnellzugriff

Inhalt:

    "Zuletzt gehört" – Horizontale Liste

    "Deine Favoriten" – Quick Access Grid (max 6, "Alle anzeigen" Link)

    "Beliebte Sender" – Top Stations aus der API

    "Nach Genre entdecken" – Chip-Row mit beliebten Genres

    "Deutsche Sender" – Automatisch basierend auf System-Locale

MODUL 10: feature-browse – Browse & Suche

Verantwortung: Sender entdecken und suchen

Inhalt:

    Suchleiste (oben, sticky) mit Debounce (300ms)

    Tabs: "Genres" | "Länder" | "Sprachen" | "Beliebt"

    Genre-Screen: Grid mit Genre-Cards → Sender-Liste

    Länder-Screen: Alphabetisch mit Flaggen → Sender-Liste

    Sender-Liste: LazyColumn mit StationListItem, Pull-to-Refresh

    Filter: Codec, Mindest-Bitrate

MODUL 11: feature-favorites – Favoriten

Verantwortung: Verwaltung der Lieblingssender

Inhalt:

    Favoriten-Liste mit Drag-to-Reorder (manuelle Sortierung)

    Long-Press für Context-Menü (Entfernen, Teilen, Details)

    Export/Import von Favoriten als JSON-Datei (Backup)

    Leere-Ansicht mit Hinweis zum Hinzufügen

    Swipe-to-Remove mit Undo-Snackbar

MODUL 12: feature-player – Player UI ⭐

Verantwortung: Hauptplayer-Oberfläche

Zwei Zustände:

A) Mini-Player (Bottom Bar)

    Immer sichtbar wenn ein Sender läuft

    Station-Logo, Name, aktueller Song (ICY Metadata)

    Play/Pause Button

    Tap → expandiert zu Fullscreen

B) Fullscreen Player (Bottom Sheet expanded)

    Großes Station-Logo (mit Blur-Background des Logos)

    Station-Name und aktuelle Metadata

    Play/Pause, Stop Buttons, Cast-Button (Toolbar)

    Favorit-Toggle-Button (Herz)

    Stream-Info: Bitrate, Codec

    Sleep Timer Button

    Share Button (Stream-URL teilen)

    Visualisierung: Animierte Equalizer-Bars oder Waveform

    Swipe-Down zum Minimieren

MODUL 13: feature-custom-stations – Eigene Sender

Verantwortung: Manuell Sender hinzufügen

Inhalt:

    "Station hinzufügen" Screen

        Name (Pflicht)

        Stream-URL (Pflicht) mit URL-Validierung

        Genre (Optional, Autocomplete aus existierenden Tags)

        Land (Optional, Dropdown)

        Logo-URL (Optional)

    "Test Stream" Button – prüft ob URL abspielbar ist

    Liste eigener Sender mit Edit/Delete

    Import via M3U/PLS Playlist-Dateien

MODUL 14: feature-alarm – Wecker-Funktion

Verantwortung: Erlaubt es, das Gerät zu einer bestimmten Uhrzeit mit einem ausgewählten Radiosender zu wecken.

    Nutzt AlarmManager (Berechtigung: SCHEDULE_EXACT_ALARM).

    Erfordert einen eigenen BroadcastReceiver, der den RadioPlaybackService startet.

    UI für Wecker-Verwaltung (Tage, Uhrzeit, Sender-Auswahl, Lautstärke-Fade-In).

MODUL 15: feature-settings – Einstellungen

Verantwortung: App-Konfiguration

Inhalt:

    Erscheinungsbild: Theme (System/Dark/Light), Dynamic Colors An/Aus

    Audio: Standard-Qualität (Niedrig/Mittel/Hoch wenn Sender mehrere Streams hat)

    Streaming: Mobile Daten erlauben (Ja/Nein), Buffer-Größe

    Sleep Timer: Standard-Dauer

    Daten: Cache leeren, History löschen, Favoriten exportieren/importieren

    Info: Version, Open Source Lizenzen, Link zu GitHub

    DataStore (Preferences) für Settings-Persistenz

MODUL 16: auto – Android Auto Integration ⭐

Verantwortung: Vollständige Android Auto Unterstützung via MediaLibraryService
Kotlin

// In RadioPlaybackService implementiert der RadioMediaSessionCallback das Interface MediaLibrarySession.Callback

class RadioMediaSessionCallback(
    // Repositories injecten (z.B. via EntryPointAccessors da Callback nicht direkt injectbar ist)
) : MediaLibrarySession.Callback {

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        // Root-Item zurückgeben
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        // Kinder basierend auf parentId laden
        // "favorites" → Favoriten-Sender
        // "recent" → Zuletzt gehört
        // "genre_rock" → Rock-Sender
        // etc.
    }

    // ... onAddPlaylistItem / onPlayMediaItem ...
}

// AndroidManifest.xml für Android Auto:
// <meta-data
//     android:name="com.google.android.gms.car.application"
//     android:resource="@xml/automotive_app_desc" />

// res/xml/automotive_app_desc.xml:
// <automotiveApp>
//     <uses name="media" />
// </automotiveApp>

6. Abhängigkeiten (Version Catalog Ergänzungen)
Ini, TOML

# gradle/libs.versions.toml
[versions]
kotlin = "2.1.0"
agp = "8.7.3"
compose-bom = "2025.01.00"
media3 = "1.5.1"
room = "2.6.1"
hilt = "2.53.1"
retrofit = "2.11.0"
kotlinx-serialization = "1.7.3"
coil = "2.7.0"
navigation-compose = "2.8.5"
datastore = "1.1.1"

[libraries]
# ... [Originale Abhängigkeiten aus v1] ...
# Neu hinzugefügt für v2.1:
media3-cast = { group = "androidx.media3", name = "media3-cast", version.ref = "media3" }

7. Entwicklungsreihenfolge (Strikte Vorgabe für Agenten)

Die Module müssen in dieser Reihenfolge entwickelt werden:

Phase 0 – Build Setup ⭐ KRITISCH
════════════════════════════════
0. build-logic         (Convention Plugins einrichten. Danach kompilieren!)

Phase 1 – Fundament
═══════════════════
1. core-model          (keine Dependencies, reines Kotlin)
2. core-database       (braucht: core-model)
3. core-network        (braucht: core-model)
4. core-data           (braucht: core-model, core-database, core-network)

Phase 2 – Player Engine & Cast
══════════════════════════════
5. core-player         (braucht: core-model, core-data. MediaLibraryService implementieren!)
6. core-cast           (braucht: core-player)

Phase 3 – UI & Features
═══════════════════════
7. core-ui             (braucht: core-model)
8. feature-player      (braucht: core-ui, core-player, core-cast)
9. feature-home        (braucht: core-ui, core-data, core-player)
10. feature-browse     (braucht: core-ui, core-data, core-player)
11. feature-favorites  (braucht: core-ui, core-data, core-player)
12. feature-custom-stations (braucht: core-ui, core-data)
13. feature-settings   (braucht: core-ui)
14. feature-alarm      (braucht: core-ui, core-data, core-player)

Phase 4 – Integration
═════════════════════
15. app                (verbindet alles, Hilt Graph schließen)
16. auto               (Android Auto Browse-Tree finalisieren)

8. Besondere technische Hinweise
Stream-Formate die unterstützt werden müssen

    MP3 (häufigster Codec)

    AAC/AAC+ (HE-AAC für niedrige Bitraten)

    OGG Vorbis

    FLAC (seltener)

    HLS Streams (.m3u8)

    ICY Protocol (SHOUTcast/Icecast Metadata)

Media3/ExoPlayer unterstützt das alles out-of-the-box!
ICY Metadata

Viele Internet-Radio-Streams senden Metadata (aktueller Song) via ICY Protocol. Media3 parsed das automatisch und liefert es über onMediaMetadataChanged.
Netzwerk-Handling

    Reconnect bei Verbindungsabbruch (ExoPlayer hat eingebaute Retry-Logik)

    Wechsel WiFi ↔ Mobile Data ohne Unterbrechung

    Offline-Graceful: "Kein Internet" State mit Retry-Button

ProGuard/R8

    Keep-Rules für Room Entities, Retrofit Interfaces

    Keep-Rules für Media3 Service

Testing

    Unit Tests: Repositories, ViewModels mit Turbine (Flow Testing)

    Instrumented Tests: Room DAO Tests

    UI Tests: Compose Testing mit composeTestRule

9. Agent-Anweisungen für OpenClaw (System Prompts)
Allgemeine Architektur-Regeln

    Zuerst Build-Logic: Generiere und validiere zwingend zuerst das build-logic Verzeichnis. Gehe erst zu Modul 2 über, wenn Gradle fehlerfrei synct.

    Jedes Modul ist ein eigenes Gradle-Modul – kein Monolith! Nutze die Convention Plugins aus build-logic in den build.gradle.kts der Module.

    Kotlin-First – kein Java, kein XML für Layouts (nur AndroidManifest und Vector Drawables).

    Compose Preview – jede UI-Komponente braucht eine @Preview Funktion, idealerweise mit Fake-Daten.

    StateFlow überall – kein LiveData, kein RxJava.

    Immutable State – alle State-Klassen sind data class mit copy().

    Error Handling – Nutze sealed class für Errors, kein Exception-Swallowing. Reagiere auf HttpDataSourceException im Player.

    Code-Dokumentation – KDoc für public APIs.

Modul-Workflow

    Erstelle das Gradle Submodule mit korrektem build.gradle.kts.

    Definiere das public API (was exportiert wird, Rest internal).

    Schreibe Unit Tests (mindestens Happy Path + Error Case). ViewModels mit Turbine testen.

    Dokumentiere Dependencies zum nächsten Modul.