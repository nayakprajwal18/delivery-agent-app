# Delivery Agent App

**Hyperlocal Commerce Hackathon — 24-Hour MVP**

A native Android app (Kotlin + Jetpack Compose) for delivery agents in Tier-2/Tier-3 Indian cities. Part of a three-app ecosystem (Shop Owner, Customer, Delivery Agent) for hyperlocal product discovery and delivery.

## Tech Stack

- **Language**: Kotlin (native Android)
- **UI**: Jetpack Compose + Material Design 3
- **Backend**: Supabase (PostgreSQL + Auth + Realtime)
- **Auth**: Supabase Auth (phone OTP)
- **Push**: Firebase Cloud Messaging (FCM)
- **Realtime**: Supabase Realtime (WebSocket subscriptions)
- **Maps**: Google Maps Intent (native app handoff)
- **Architecture**: MVVM + Repository pattern
- **Async**: Kotlin Coroutines + StateFlow
- **DI**: Manual (no Hilt, for MVP speed)

## Features Implemented

### ✅ Phase 1-2: Project Setup & Infrastructure
- Gradle configuration with version catalog
- Supabase client singleton
- Data models with kotlinx.serialization
- Repositories (Auth, Order, Agent)
- Manual dependency injection via AppContainer

### ✅ Phase 3: Login Screen
- Phone number input (10-digit Indian format, auto +91 prepend)
- OTP verification via Supabase Auth
- Profile & delivery_agent row creation on first login
- Session persistence (auto-login on app relaunch)
- 30-second resend cooldown

### ✅ Phase 4: Home Dashboard
- Real-time stats: available deliveries count, today's completed, today's earnings
- Active delivery card with navigation to details
- Availability toggle (online/offline) with Realtime sync
- Realtime subscriptions to orders table for live stat updates
- Pull-to-refresh and error handling

### ✅ Phase 5: Available Deliveries Screen
- List of READY_FOR_PICKUP orders (status = READY_FOR_PICKUP, agent_id IS NULL)
- Each card shows: shop name, drop address, distance (Haversine formula), earnings
- Accept button with concurrent collision protection (DB-level WHERE guard)
- Realtime updates: new orders appear live, claimed orders disappear
- Error handling for concurrent accepts (shows toast "Already accepted by another agent")

### ✅ Phase 6-7: Delivery Details Screen (In Progress)
- Show order items (quantity, unit)
- Pickup and drop locations with Realtime tracking
- Call customer button (Intent.ACTION_DIAL)
- Navigate button (opens Google Maps with geo: URI)
- Pickup confirmation (status AGENT_ASSIGNED → PICKED_UP)
- Delivery confirmation (status PICKED_UP → DELIVERED)
- Real-time order status updates via Realtime subscription

### ⏳ Future Phases (Out of Scope)
- History screen (completed deliveries list)
- Profile screen (edit info, view earnings)
- Advanced error handling & retry logic
- Production hardening (rate limiting, OTP brute-force protection)
- Photo proof upload (Firebase Storage)
- In-app map rendering

## Project Structure

```
DeliveryAgent/
├── gradle/libs.versions.toml          # Version catalog (dependencies)
├── app/
│   ├── build.gradle.kts               # App-level build config
│   ├── proguard-rules.pro             # Serialization + Supabase keeps
│   └── src/main/
│       ├── AndroidManifest.xml        # Permissions, FCM service, Application
│       ├── kotlin/com/yourteam/deliveryagent/
│       │   ├── DeliveryAgentApp.kt    # Application class, FCM notification channel
│       │   ├── MainActivity.kt         # Single activity, session check, NavHost
│       │   ├── core/
│       │   │   ├── SupabaseClientProvider.kt      # Singleton Supabase client
│       │   │   ├── AppContainer.kt                # Manual DI for repos
│       │   │   ├── DistanceUtils.kt               # Haversine formula
│       │   │   └── MapsNavigationUtils.kt         # Google Maps Intent
│       │   ├── data/
│       │   │   ├── model/             # Order, OrderItem, Profile, DeliveryAgent, Shop, Notification
│       │   │   └── repository/        # AuthRepository, OrderRepository, AgentRepository
│       │   ├── navigation/
│       │   │   ├── NavRoutes.kt       # Route constants & helpers
│       │   │   └── AppNavGraph.kt     # NavHost + Scaffold + bottom nav
│       │   ├── ui/
│       │   │   ├── screens/           # All screen composables
│       │   │   │   ├── LoginScreen.kt
│       │   │   │   ├── HomeScreen.kt
│       │   │   │   ├── AvailableDeliveriesScreen.kt
│       │   │   │   ├── DeliveryDetailsScreen.kt
│       │   │   │   ├── HistoryScreen.kt
│       │   │   │   └── ProfileScreen.kt
│       │   │   ├── [screens name]/    # Wrappers + ViewModels
│       │   │   │   ├── login/
│       │   │   │   ├── dashboard/
│       │   │   │   ├── availabledeliveries/
│       │   │   │   └── deliverydetails/
│       │   │   └── theme/             # Color.kt, Type.kt, Theme.kt
│       │   └── fcm/
│       │       └── DeliveryFCMService.kt  # FCM message handling + deeplinks
│       └── res/values/                # strings.xml, themes.xml
└── local.properties                   # SUPABASE_URL, SUPABASE_ANON_KEY (not committed)
```

## Getting Started

### Prerequisites
- Android Studio (latest)
- Kotlin 2.1.20+
- minSdk 26 (required by supabase-kt)
- Google Play Services installed on device/emulator
- Supabase project with:
  - Phone OTP auth enabled
  - PostgreSQL schema matching data models
  - Row-Level Security (RLS) policies for delivery agents
- Firebase project with Cloud Messaging enabled

### Setup Steps

1. **Clone & Open Project**
   ```bash
   git clone https://github.com/[your-username]/delivery-agent-app.git
   cd DeliveryAgent
   open in Android Studio
   ```

2. **Configure Credentials**
   - Get `SUPABASE_URL` and `SUPABASE_ANON_KEY` from Supabase Dashboard
   - Get `google-services.json` from Firebase Console
   - Update `local.properties`:
     ```properties
     SUPABASE_URL=https://your-project.supabase.co
     SUPABASE_ANON_KEY=your-anon-key
     ```
   - Place `google-services.json` in `app/` directory

3. **Update Package Name**
   - Find & replace `com.yourteam.deliveryagent` with your package name
   - Update `applicationId` in `app/build.gradle.kts`

4. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   # Or in Android Studio: Build → Build Bundle / APK
   ```

## Architecture

### MVVM + Repository Pattern
- **View**: Jetpack Compose screens
- **ViewModel**: Holds StateFlow<UiState>, orchestrates business logic
- **Repository**: Supabase queries, Realtime subscriptions
- **Model**: Data classes with kotlinx.serialization

### Realtime Data Flow
```
PostgreSQL Change → Supabase Realtime WebSocket → Repository postgresChangeFlow 
→ ViewModel state update → UI recomposition
```

### Authentication Flow
```
Phone Input → SendOtp (Supabase Auth) → OTP Input → VerifyOtp 
→ ensureDeliveryAgentProfile (create rows if missing) → Session stored locally → Auto-login
```

### Concurrent Accept Protection
```
Agent A clicks Accept Order X
↓
acceptOrder(orderId=X, agentId=A)
↓
Database: UPDATE orders SET agent_id='A', status='AGENT_ASSIGNED' WHERE id=X AND agent_id IS NULL
↓
0 rows affected → Another agent took it → Show toast, refresh list
1 row affected → Success → Navigate to Details
```

## Key Implementation Notes

### No Hilt (for MVP speed)
- Manual DI via `AppContainer` created in `Application.onCreate()`
- Repositories injected directly into ViewModels
- ViewModels created with manual `ViewModelProvider.Factory`

### Error Handling
- All Repository functions return `Result<T>` (Kotlin stdlib)
- ViewModels map failures to `UiState.Error` or event-based snackbars
- Network errors show inline messages; no retry logic (user taps button again)

### Realtime Subscriptions
- Channels opened in ViewModel `init`, closed automatically on ViewModel clear
- Subscriptions emit to `SharedFlow` or update `StateFlow` directly
- Handles INSERT/UPDATE/DELETE events appropriately

### FCM Integration
- `DeliveryFCMService` extends `FirebaseMessagingService`
- `onNewToken()` saves token to `delivery_agents.fcm_token`
- `onMessageReceived()` builds notifications with deeplink intents
- Foreground messages show as snackbars (optional)

### Distance Calculation
- Haversine formula approximates walking distance between coordinates
- Formatted to 1 decimal place (e.g., "2.4 km")
- Good enough for MVP; no real-time GPS updates needed

## Testing

### Manual Test Scenarios (24-hour hackathon demo)

1. **Login Flow**
   - Enter valid 10-digit phone → receive OTP → verify → auto-created profile
   - Kill app → relaunch → should auto-login (session persisted)

2. **Dashboard Updates**
   - View dashboard with available count
   - Another device: create new order in DB → count increments live (Realtime)
   - Accept order → count decrements live

3. **Concurrent Accept**
   - Two agents see same order
   - Both tap Accept simultaneously
   - One succeeds, other sees toast "Already taken"

4. **Delivery Confirmation**
   - Accept order → navigate to Details
   - Tap "Confirm Pickup" → status changes to PICKED_UP (Realtime reflects)
   - Tap "Confirm Delivery" → status DELIVERED → navigate to Home

5. **Push Notifications** (if FCM is set up)
   - New available delivery → agent should receive notification
   - Tap notification → app opens to available deliveries or order details

## Future Scope (Post-Hackathon)

- **Hilt Integration**: Replace manual DI
- **Offline Mode**: Room database + WorkManager sync queue
- **Photo Proof**: Firebase Storage upload on delivery confirmation
- **Advanced Maps**: In-app navigation with Google Maps SDK or Mapbox
- **Analytics**: Crashlytics + custom event tracking
- **Payment**: Payout integration for earnings
- **Rating System**: Customer feedback from delivery agent
- **Production Hardening**: Rate limiting, OTP brute-force protection, extensive logging

## Contributing

This is a hackathon MVP. For production readiness:
1. Add comprehensive error handling & logging
2. Implement Hilt DI
3. Add unit + UI tests
4. Implement advanced caching & offline support
5. Set up CI/CD pipeline

## License

MIT (for hackathon submission)

---

**Built in 24 hours for the Hyperlocal Commerce Hackathon**  
Contact: dev@deliveryagent.local
