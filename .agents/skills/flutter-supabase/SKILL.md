# SKILL: Flutter + Supabase

## pubspec.yaml – Dependencies

```yaml
dependencies:
  flutter:
    sdk: flutter

  # Supabase
  supabase_flutter: ^2.3.4

  # State Management
  flutter_riverpod: ^2.5.1

  # Navigation
  go_router: ^13.2.0

  # Offline Cache
  hive_flutter: ^1.1.0

  # Utils
  uuid: ^4.3.3
  intl: ^0.19.0
  share_plus: ^7.2.2
  flutter_dotenv: ^5.1.0

  # UI
  flutter_slidable: ^3.0.1
```

## Setup

```dart
// main.dart
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:supabase_flutter/supabase_flutter.dart';

Future<void> main() async {
  await dotenv.load();
  await Supabase.initialize(
    url: dotenv.env['SUPABASE_URL']!,
    anonKey: dotenv.env['SUPABASE_ANON_KEY']!,
  );
  runApp(const ProviderScope(child: App()));
}

// Shortcut
final supabase = Supabase.instance.client;
```

## .env Template

```env
# .env (NIEMALS in Git committen!)
SUPABASE_URL=https://xxxxx.supabase.co
SUPABASE_ANON_KEY=eyJ...

# Self-Hosted (Unraid)
# SUPABASE_URL=http://192.168.x.x:8000
# SUPABASE_ANON_KEY=<dein-anon-key>
```

## Auth

```dart
class AuthRepository {
  final _auth = supabase.auth;

  Future<void> signIn(String email, String password) async {
    await _auth.signInWithPassword(email: email, password: password);
  }

  Future<void> register(String email, String password, String name) async {
    final res = await _auth.signUp(email: email, password: password);
    if (res.user != null) {
      await supabase.from('profiles').insert({
        'id': res.user!.id,
        'email': email,
        'name': name,
      });
    }
  }

  Future<void> signOut() => _auth.signOut();

  Stream<AuthState> get authStateChanges => _auth.onAuthStateChange;
}

// Provider
final authStateProvider = StreamProvider<AuthState>((ref) {
  return supabase.auth.onAuthStateChange;
});

final currentUserProvider = Provider<User?>((ref) {
  return ref.watch(authStateProvider).value?.session?.user;
});
```

## Realtime – Items beobachten

```dart
class ItemsRepository {
  Stream<List<ShoppingItem>> watchItems(String listId) {
    return supabase
        .from('items')
        .stream(primaryKey: ['id'])
        .eq('list_id', listId)
        .order('checked')
        .order('created_at', ascending: false)
        .map((rows) => rows.map(ShoppingItem.fromMap).toList());
  }

  Future<void> addItem(String listId, String name, String userId) async {
    await supabase.from('items').insert({
      'list_id': listId,
      'name': name,
      'added_by': userId,
    });
  }

  Future<void> toggleItem(String itemId, bool checked) async {
    await supabase.from('items').update({'checked': checked}).eq('id', itemId);
  }

  Future<void> deleteItem(String itemId) async {
    await supabase.from('items').delete().eq('id', itemId);
  }
}
```

## Model Pattern (fromMap / toMap)

```dart
class ShoppingItem {
  final String id;
  final String listId;
  final String name;
  final int quantity;
  final bool checked;
  final double? price;
  final String? shop;
  final String addedBy;
  final DateTime createdAt;

  const ShoppingItem({
    required this.id,
    required this.listId,
    required this.name,
    this.quantity = 1,
    this.checked = false,
    this.price,
    this.shop,
    required this.addedBy,
    required this.createdAt,
  });

  factory ShoppingItem.fromMap(Map<String, dynamic> map) {
    return ShoppingItem(
      id: map['id'] as String,
      listId: map['list_id'] as String,
      name: map['name'] as String,
      quantity: (map['quantity'] ?? 1) as int,
      checked: (map['checked'] ?? false) as bool,
      price: (map['price'] as num?)?.toDouble(),
      shop: map['shop'] as String?,
      addedBy: map['added_by'] as String,
      createdAt: DateTime.parse(map['created_at'] as String),
    );
  }

  Map<String, dynamic> toMap() => {
    'list_id': listId,
    'name': name,
    'quantity': quantity,
    'checked': checked,
    if (price != null) 'price': price,
    if (shop != null) 'shop': shop,
    'added_by': addedBy,
  };
}
```

## Offline Cache Pattern (Hive)

```dart
// Wenn Supabase nicht erreichbar → aus Hive laden
class CachedItemsRepository {
  final ItemsRepository _remote;
  final Box<String> _cache;

  CachedItemsRepository(this._remote, this._cache);

  Stream<List<ShoppingItem>> watchItems(String listId) {
    return _remote.watchItems(listId).handleError((e) {
      final cached = _cache.get('items_$listId');
      if (cached != null) {
        final list = (jsonDecode(cached) as List)
            .map((m) => ShoppingItem.fromMap(m as Map<String, dynamic>))
            .toList();
        return Stream.value(list);
      }
      throw e;
    });
  }
}
```

## Einladungs-System

```dart
// Invite erstellen
Future<String> createInvite(String listId) async {
  final userId = supabase.auth.currentUser!.id;
  final res = await supabase.from('invites').insert({
    'list_id': listId,
    'created_by': userId,
  }).select('token').single();
  return 'packedmanager://join/${res['token']}';
}

// Invite einlösen
Future<void> joinViaToken(String token) async {
  final userId = supabase.auth.currentUser!.id;

  // Invite laden und prüfen
  final invite = await supabase
      .from('invites')
      .select()
      .eq('token', token)
      .single();

  if (invite['used'] == true) throw Exception('Link bereits verwendet');
  if (DateTime.parse(invite['expires_at']).isBefore(DateTime.now())) {
    throw Exception('Link abgelaufen');
  }

  // Mitglied hinzufügen + Invite als used markieren
  await Future.wait([
    supabase.from('list_members').insert({
      'list_id': invite['list_id'],
      'user_id': userId,
      'role': 'member',
    }),
    supabase.from('invites').update({'used': true}).eq('token', token),
  ]);
}
```

## Self-Hosted auf Unraid – Quick Setup

```bash
# 1. Supabase CLI installieren
npm install -g supabase

# 2. Projekt initialisieren
supabase init

# 3. Lokale Instanz starten (für Development)
supabase start

# 4. Migrations anwenden
supabase db push

# 5. Für Unraid: docker-compose.yml aus Agent 14 verwenden
# Volumes nach /mnt/user/appdata/supabase/
```

## Supabase Studio (Self-Hosted)
- URL: `http://<unraid-ip>:3000`
- Table Editor, SQL Editor, Auth Management
- RLS Policies direkt im Browser bearbeiten

## Häufige Fehler & Fixes

| Problem | Lösung |
|---------|--------|
| 401 Unauthorized | RLS Policy fehlt oder falsch |
| Realtime funktioniert nicht | Realtime für Tabelle in Studio aktivieren |
| Self-Hosted: Connection refused | Kong API Gateway Port 8000 prüfen |
| JWT expired | Session refresh: `supabase.auth.refreshSession()` |
| CORS Error | Supabase Dashboard → API Settings → Allowed Origins |
