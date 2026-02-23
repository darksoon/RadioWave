---
name: godot-gdscript
description: Konventionen und Best Practices für Godot 4.x mit GDScript
version: 1.0.0
triggers: ["*.gd", "*.tscn", "*.tres", "project.godot"]
---

# Godot 4.x / GDScript Skill

## Engine-Version
Godot 4.3+ mit GDScript (NICHT C#, NICHT Godot 3.x)

## Code-Regeln

### Typisierung
- Immer statische Typen verwenden: `var speed: float = 200.0`
- Return-Typen auf allen Funktionen: `func get_health() -> int:`
- Arrays typisieren: `var enemies: Array[Enemy] = []`
- Dictionaries typisieren (Godot 4.4+): `var scores: Dictionary[String, int] = {}`

### Signal-Konventionen
- Signals als erstes im Script deklarieren
- Benannt in past tense oder als Event: `signal died`, `signal health_changed(new_value: int)`
- Verbindung bevorzugt über Editor oder `connect()` in `_ready()`
- NICHT `emit_signal("name")` → verwende `signal_name.emit()`

### Node-Referenzen
- `@onready` statt `get_node()` in `_ready()`
- Unique Name (`%NodeName`) für wichtige Nodes: `@onready var hud := %HUD`
- Relative Pfade nur für direkte Kinder

### Export-Variablen
- `@export` für im Editor konfigurierbare Werte
- `@export_category`, `@export_group` für Organisation
- `@export_range(0, 100, 0.5)` für numerische Grenzen

### Verbotene Patterns
- ❌ `get_tree().get_nodes_in_group()` in `_process` → Cache das Ergebnis
- ❌ `load()` zur Laufzeit für große Assets → `preload()` oder async ResourceLoader
- ❌ Direkte Node-Referenzen über den Scene Tree → Signals oder Groups
- ❌ `yield()` → verwende `await`
- ❌ String-basierte Methodenaufrufe → direkte Referenzen oder Callable

### Performance
- `_physics_process` nur für Physik-relevantes
- `set_process(false)` für inaktive Nodes
- Object Pooling für häufiges Spawning (Projektile, Partikel)
- `StringName` statt `String` für häufige Vergleiche
- Vermeide `_process` wo Timer oder Signals reichen

## Projektstruktur

```
res://
├── autoload/           # AutoLoad Singletons
├── scenes/             # Alle Szenen (.tscn + .gd)
│   ├── main/
│   ├── levels/
│   ├── entities/
│   ├── ui/
│   └── components/     # Wiederverwendbare Sub-Scenes
├── resources/          # Custom Resources (.tres)
├── assets/             # Grafiken, Audio, Fonts
│   ├── sprites/
│   ├── audio/
│   └── fonts/
├── shaders/            # .gdshader Dateien
└── addons/             # Third-Party Plugins
```

## Nützliche Patterns
- State Machine für Spieler/Gegner-Zustände
- Component Pattern: HealthComponent, HitboxComponent als Sub-Scenes
- Event Bus (AutoLoad) für globale Kommunikation
- Resource-Klassen für Daten (EnemyStats, WeaponData)
