# SKILL: Flutter + Firebase (Packed Manager Stack)

## pubspec.yaml – Dependencies

```yaml
dependencies:
  flutter:
    sdk: flutter

  # Firebase Core
  firebase_core: ^2.27.0
  firebase_auth: ^4.17.0
  cloud_firestore: ^4.15.0
  firebase_messaging: ^14.7.0        # Push Notifications

  # State Management
  flutter_riverpod: ^2.5.1
  riverpod_annotation: ^2.3.4

  # Navigation
  go_router: ^13.2.0

  # Lokaler Cache / Offline
  hive_flutter: ^1.1.0

  # Utils
  uuid: ^4.3.3                       # für Invite Tokens
  intl: ^0.19.0                      # Datumsformatierung
  share_plus: ^7.2.2                 # Share Sheet für Einladungslinks
  flutter_dotenv: ^5.1.0             # .env für Konfiguration

  # UI
  flutter_slidable: ^3.0.1           # Swipe-to-delete
  cached_network_image: ^3.3.1

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0
  build_runner: ^2.4.8
  riverpod_generator: ^2.3.9
```

## FirestorePaths – Zentrale Pfad-Verwaltung

```dart
// lib/shared/constants/firestore_paths.dart
class FirestorePaths {
  // Collections
  static const users = 'users';
  static const lists = 'lists';
  static const listMembers = 'listMembers';
  static const invites = 'invites';

  // Sub-Collections
  static String items(String listId) => 'lists/$listId/items';
  static String archive(String listId) => 'lists/$listId/archive';
  static String favorites(String userId) => 'users/$userId/favorites';

  // Documents
  static String user(String uid) => 'users/$uid';
  static String list(String listId) => 'lists/$listId';
  static String membership(String listId, String userId) =>
      'listMembers/${listId}_$userId';
  static String invite(String token) => 'invites/$token';
}
```

## Model Pattern (fromDoc / toMap)

```dart
// lib/features/items/domain/item_model.dart
import 'package:cloud_firestore/cloud_firestore.dart';

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

  factory ShoppingItem.fromDoc(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return ShoppingItem(
      id: doc.id,
      listId: data['listId'] ?? '',
      name: data['name'] ?? '',
      quantity: (data['quantity'] ?? 1) as int,
      checked: (data['checked'] ?? false) as bool,
      price: (data['price'] as num?)?.toDouble(),
      shop: data['shop'] as String?,
      addedBy: data['addedBy'] ?? '',
      createdAt: (data['createdAt'] as Timestamp?)?.toDate() ?? DateTime.now(),
    );
  }

  Map<String, dynamic> toMap() => {
    'listId': listId,
    'name': name,
    'quantity': quantity,
    'checked': checked,
    if (price != null) 'price': price,
    if (shop != null) 'shop': shop,
    'addedBy': addedBy,
    'createdAt': FieldValue.serverTimestamp(),
  };

  ShoppingItem copyWith({bool? checked, int? quantity, String? name}) {
    return ShoppingItem(
      id: id,
      listId: listId,
      name: name ?? this.name,
      quantity: quantity ?? this.quantity,
      checked: checked ?? this.checked,
      price: price,
      shop: shop,
      addedBy: addedBy,
      createdAt: createdAt,
    );
  }
}
```

## Repository Pattern

```dart
// lib/features/items/data/items_repository.dart
class ItemsRepository {
  final FirebaseFirestore _db;
  ItemsRepository(this._db);

  // Echtzeit Stream – Offene oben, erledigte unten
  Stream<List<ShoppingItem>> watchItems(String listId) {
    return _db
        .collection(FirestorePaths.items(listId))
        .orderBy('checked')
        .orderBy('createdAt', descending: true)
        .snapshots()
        .map((s) => s.docs.map(ShoppingItem.fromDoc).toList());
  }

  Future<void> addItem(String listId, String name, String userId) {
    return _db.collection(FirestorePaths.items(listId)).add(
      ShoppingItem(
        id: '',
        listId: listId,
        name: name,
        addedBy: userId,
        createdAt: DateTime.now(),
      ).toMap(),
    );
  }

  Future<void> toggleItem(String listId, String itemId, bool checked) {
    return _db
        .collection(FirestorePaths.items(listId))
        .doc(itemId)
        .update({'checked': checked});
  }

  Future<void> deleteItem(String listId, String itemId) {
    return _db
        .collection(FirestorePaths.items(listId))
        .doc(itemId)
        .delete();
  }
}

// Provider
final itemsRepositoryProvider = Provider<ItemsRepository>((ref) {
  return ItemsRepository(FirebaseFirestore.instance);
});
```

## Riverpod – Auth Provider

```dart
// lib/features/auth/providers/auth_provider.dart
final authStateProvider = StreamProvider<User?>((ref) {
  return FirebaseAuth.instance.authStateChanges();
});

final currentUserProvider = Provider<User?>((ref) {
  return ref.watch(authStateProvider).value;
});

// Auth Repository
class AuthRepository {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final FirebaseFirestore _db = FirebaseFirestore.instance;

  Future<void> signIn(String email, String password) =>
      _auth.signInWithEmailAndPassword(email: email, password: password);

  Future<void> register(String email, String password, String name) async {
    final cred = await _auth.createUserWithEmailAndPassword(
      email: email, password: password);
    // Profil in Firestore anlegen
    await _db.collection(FirestorePaths.users).doc(cred.user!.uid).set({
      'email': email,
      'name': name,
      'createdAt': FieldValue.serverTimestamp(),
    });
  }

  Future<void> signOut() => _auth.signOut();

  // DSGVO: Alle Daten löschen
  Future<void> deleteAccount(String userId) async {
    // 1. Alle Mitgliedschaften finden und entfernen
    // 2. Alle eigenen Listen (als Owner) löschen
    // 3. Profil löschen
    // 4. Auth Account löschen
    await _auth.currentUser!.delete();
  }
}
```

## Push Notifications (FCM)

```dart
// lib/main.dart – FCM Setup
Future<void> setupFCM(String userId) async {
  final messaging = FirebaseMessaging.instance;

  // Permission anfragen
  await messaging.requestPermission(alert: true, badge: true, sound: true);

  // Token speichern
  final token = await messaging.getToken();
  if (token != null) {
    await FirebaseFirestore.instance
        .collection(FirestorePaths.users)
        .doc(userId)
        .update({'fcmToken': token});
  }

  // Token refresh
  messaging.onTokenRefresh.listen((newToken) {
    FirebaseFirestore.instance
        .collection(FirestorePaths.users)
        .doc(userId)
        .update({'fcmToken': newToken});
  });

  // Foreground Messages
  FirebaseMessaging.onMessage.listen((RemoteMessage message) {
    // Local notification zeigen
  });
}
```

## Deep Link Handling (Invite Links)

```dart
// android/app/src/main/AndroidManifest.xml – Intent Filter
// <intent-filter>
//   <action android:name="android.intent.action.VIEW"/>
//   <category android:name="android.intent.category.DEFAULT"/>
//   <category android:name="android.intent.category.BROWSABLE"/>
//   <data android:scheme="packedmanager" android:host="join"/>
// </intent-filter>

// GoRouter – Deep Link Route
GoRoute(
  path: '/join/:token',
  builder: (context, state) => JoinListScreen(
    token: state.pathParameters['token']!,
  ),
),
```

## Häufige Fehler & Fixes

| Problem | Lösung |
|---------|--------|
| Firestore offline funktioniert nicht | `persistenceEnabled: true` in main.dart setzen |
| Realtime updates kommen nicht | `.snapshots()` statt `.get()` verwenden |
| Permission denied | Firestore Rules prüfen, `isMember()` Helper |
| FCM kommt nicht im Background | `flutter_local_notifications` für Foreground |
| Build schlägt fehl wegen google-services.json | `flutterfire configure` nochmal ausführen |

## Deployment Checklist

- [ ] `google-services.json` in `android/app/` vorhanden
- [ ] Firestore Security Rules deployed (`firebase deploy --only firestore:rules`)
- [ ] Firebase Region: europe-west3 (Frankfurt)
- [ ] ProGuard Rules für Firebase in `android/app/proguard-rules.pro`
- [ ] `minSdkVersion` auf 21 (Android 5.0+)
- [ ] Release APK: `flutter build apk --release`
- [ ] Signed APK für Play Store: Keystore erstellen und konfigurieren

## GitHub Actions – Automatischer APK Build

```yaml
# .github/workflows/build.yml
name: Build Android APK

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.19.0'
      - run: flutter pub get
      - run: flutter build apk --release
      - uses: actions/upload-artifact@v4
        with:
          name: release-apk
          path: build/app/outputs/flutter-apk/app-release.apk
```
