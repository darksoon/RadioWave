---
name: roblox-luau
description: Konventionen und Best Practices für Roblox Luau Entwicklung
version: 1.0.0
triggers: ["*.lua", "*.luau", "*.rbxlx"]
---

# Roblox / Luau Skill

## Sprache
Luau (Roblox's Lua-Fork, NICHT Standard Lua 5.x)

## Code-Regeln

### Strict Mode + Typen
- `--!strict` als erste Zeile in jeder Datei
- Alle Funktionen typisiert:
  ```lua
  local function heal(target: Player, amount: number): boolean
  ```
- Custom Types mit `type`:
  ```lua
  type WeaponConfig = {
      name: string,
      damage: number,
      cooldown: number,
      range: number,
  }
  ```
- Alles `local` — keine globalen Variablen

### Deprecated API vermeiden
```lua
-- ❌ ALT                    -- ✅ NEU
wait(1)                      task.wait(1)
spawn(fn)                    task.spawn(fn)
delay(1, fn)                 task.delay(1, fn)
Instance.new("Part", parent) local p = Instance.new("Part")
                             p.Parent = parent  -- Parent zuletzt!
```

### Services abrufen
```lua
local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local RunService = game:GetService("RunService")
-- Services IMMER über GetService, nie über game.ServiceName
```

### Remote-Kommunikation
```lua
-- Server erstellt Remotes in ReplicatedStorage/Remotes/
-- Naming: Verb + Noun
-- RemoteEvent: FireServer/FireClient (one-way)
-- RemoteFunction: InvokeServer (two-way, NICHT InvokeClient!)

-- Server-seitige Validierung IMMER:
remote.OnServerEvent:Connect(function(player: Player, action: string)
    if typeof(action) ~= "string" then return end  -- Type Check
    if #action > 50 then return end                  -- Length Check
    -- Rate Limit Check
    -- Sanity Check (Entfernung, Cooldown, etc.)
end)
```

### Memory Management
- `Debris:AddItem(instance, lifetime)` für temporäre Instanzen
- `:Destroy()` aufrufen wenn Instanzen nicht mehr gebraucht werden
- Connections in Variablen speichern und `:Disconnect()` aufrufen
- Maid/Trove Pattern für Cleanup:
  ```lua
  local connections = {}
  table.insert(connections, event:Connect(handler))
  -- Cleanup:
  for _, conn in connections do conn:Disconnect() end
  ```

### DataStore
- Immer `pcall()` um DataStore-Aufrufe
- Retry mit Exponential Backoff (max 5 Versuche)
- Session Locking gegen Duplikation
- Auto-Save Interval: 60 Sekunden
- Daten-Schema mit Version-Number für Migrationen

## Verbotene Patterns
- ❌ `wait()`, `spawn()`, `delay()`
- ❌ `loadstring()` — Sicherheitsrisiko
- ❌ `script.Parent.Parent.Parent` Chains
- ❌ `:WaitForChild()` ohne Timeout-Parameter
- ❌ `Instance.new("Part", workspace)` — Parent als letztes setzen
- ❌ Client-seitige Gameplay-Entscheidungen
- ❌ `while true do wait() end` → RunService Events
- ❌ Globale Variablen
- ❌ `string.find()` für Gameplay-Logik → Enums/Tables
