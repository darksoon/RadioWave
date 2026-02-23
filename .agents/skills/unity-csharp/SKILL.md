---
name: unity-csharp
description: Konventionen und Best Practices für Unity mit C#
version: 1.0.0
triggers: ["*.cs", "*.unity", "*.asmdef", "*.prefab"]
---

# Unity / C# Skill

## Engine
Unity 2022 LTS+ mit C# 10+

## Code-Regeln

### Namenskonventionen
```csharp
public class PlayerController { }        // PascalCase: Klassen
public void TakeDamage(int amount) { }   // PascalCase: Methoden
public int Health { get; private set; }   // PascalCase: Properties
public event Action OnDied;              // PascalCase: Events

private float _moveSpeed;               // _camelCase: private Felder
private void handleInput() { }          // camelCase: private Methoden (optional)
local variable = 5;                     // camelCase: Lokale Variablen

private const int MAX_HEALTH = 100;     // UPPER_SNAKE: Konstanten
public interface IDamageable { }         // I-Prefix: Interfaces
```

### Inspector-Variablen
```csharp
// ✅ GUT
[Header("Movement")]
[SerializeField] private float _moveSpeed = 5f;
[SerializeField, Range(0f, 1f)] private float _friction = 0.3f;

[Header("Combat")]
[SerializeField] private WeaponData _weaponData;
[SerializeField] private LayerMask _enemyLayer;

// ❌ SCHLECHT
public float moveSpeed = 5f;
```

### Component-Zugriff
```csharp
// ✅ Cache in Awake
private Rigidbody _rb;
private void Awake() => _rb = GetComponent<Rigidbody>();

// ✅ TryGetComponent
if (other.TryGetComponent<IDamageable>(out var target))
    target.TakeDamage(10);

// ❌ NIEMALS in Update
void Update() {
    GetComponent<Rigidbody>().velocity = ...;  // VERBOTEN
}
```

### Events statt direkte Referenzen
```csharp
// Definieren
public static event Action<int> OnScoreChanged;
public static event Action<Player, Enemy> OnEnemyKilled;

// Auslösen
OnScoreChanged?.Invoke(newScore);

// Abonnieren / Abbestellen
void OnEnable() => GameManager.OnScoreChanged += UpdateUI;
void OnDisable() => GameManager.OnScoreChanged -= UpdateUI;
```

### ScriptableObjects für Daten
```csharp
[CreateAssetMenu(fileName = "New Enemy", menuName = "Game/Enemy Data")]
public class EnemyData : ScriptableObject
{
    [field: SerializeField] public string Name { get; private set; }
    [field: SerializeField] public int Health { get; private set; }
    [field: SerializeField] public float Speed { get; private set; }
    [field: SerializeField] public GameObject Prefab { get; private set; }
}
```

### State Machine
```csharp
public interface IState
{
    void Enter();
    void Update();
    void Exit();
}

// Zustandswechsel über StateMachine-Klasse
// NICHT über bool-Flags: if (isRunning && !isJumping && isGrounded)
```

## Performance-Regeln
- Cache alles was in Update gebraucht wird
- Object Pooling für Spawn/Destroy (min. 10x pro Sekunde)
- `StringBuilder` für String-Konkatenation in Loops
- `struct` für kleine Daten-Container (< 16 Bytes ideal)
- Keine Allokationen in Update (LINQ, new List, String+)
- `CompareTag()` statt `tag ==`
- Profiler nutzen, nicht raten

## Verbotene Patterns
- ❌ `public` Felder für Inspector
- ❌ `Find()` / `FindObjectOfType()` in Update
- ❌ `GameObject.Find("Name")`
- ❌ Magic Numbers: `health -= 10;` → `health -= _weaponData.Damage;`
- ❌ God-Classes (>300 Zeilen = refactoren)
- ❌ `Resources.Load` als Standard-Loading
- ❌ `new List<T>()` in Update/FixedUpdate
- ❌ `Camera.main` in Update (cached bis Unity 2020, trotzdem cachen)
- ❌ Leere Unity-Callbacks (leeres Update() kostet Performance)
