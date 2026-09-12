# Setup Credentials Guide

## 🔐 Before Building the App

This project requires Firebase and Supabase credentials. These are **NOT** included in the repository for security reasons.

---

## 📋 Step 1: Firebase Configuration

### Get google-services.json

1. Go to: https://console.firebase.google.com/
2. Create a project or select existing one
3. Add Android app with package: **`com.yourteam.deliveryagent`**
4. Download the `google-services.json` file
5. Place it in: `app/google-services.json`

**Example structure** (see `google-services.json.example`):
```json
{
  "project_info": {
    "project_number": "YOUR_NUMBER",
    "project_id": "YOUR_PROJECT_ID",
    "storage_bucket": "YOUR_BUCKET"
  },
  // ... rest of config
}
```

---

## 📋 Step 2: Supabase Configuration

### Get Supabase Credentials

1. Go to: https://supabase.com/
2. Create project or select existing one
3. Get your credentials from **Project Settings**
4. Create `local.properties` in project root:

```properties
supabase.url=https://your-project-id.supabase.co
supabase.key=your-anon-key
```

**Example** (see `local.properties.example`):
```properties
# Replace with your actual values
supabase.url=https://abcdef123456.supabase.co
supabase.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## ✅ Files to Create

After setup, you should have:

```
DeliveryAgent/
├── app/
│   └── google-services.json      ← Created from Firebase Console
├── local.properties               ← Created with Supabase credentials
└── ...
```

---

## 🚀 Then Build

Once credentials are in place:

### Option 1: Android Studio
- File → Open → Select folder
- Wait for Gradle sync
- Build → Build APK(s)

### Option 2: Command Line
```bash
cd DeliveryAgent
gradle clean build
```

---

## 🧪 Test Login

After building and installing:
- **Phone**: 9876543210 (any 10 digits)
- **OTP**: 123456 (any 6 digits)

---

## ⚠️ Important

- ❌ **Never** commit `google-services.json` to Git
- ❌ **Never** commit `local.properties` with real credentials
- ✅ Both are in `.gitignore`
- ✅ Use `.example` files as templates

---

## 📚 Templates Available

- `google-services.json.example` - Firebase template
- `local.properties.example` - Supabase template

Copy these and fill in your values.

---

## 🆘 Troubleshooting

### Error: "Task :app:processDebugGoogleServices FAILED"
→ Missing `google-services.json` in `app/` folder

### Error: "Supabase URL not found"
→ Missing `local.properties` in project root

### Error: "gradlew.bat not found"
→ Use Android Studio (auto-downloads Gradle)

---

**Setup takes ~5 minutes. Then you're ready to build!** ✅
