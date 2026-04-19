# EquiPay — Shared Group Finance App
<img width="1280" height="720" alt="изображение" src="https://github.com/user-attachments/assets/fb1ecc0c-36dd-4e45-a595-3c7e8f48091c" />

A fully working Android + Ktor backend app built for the **TatraBank Hackathon**. Shared group wallets with PSD2 open banking, per-member virtual cards, AI-powered expense splitting, voice assistant, and receipt scanning.

Monorepo: **Android** (Kotlin + Jetpack Compose) + **Backend** (Ktor + PostgreSQL + Redis).

---

## 🚀 Quick Start (local via Docker)

```bash
cd backend
cp .env.example .env
# Edit .env — add your OpenRouter / Resend / Tatra keys
docker compose up --build
```

After startup:
- API: `http://localhost:8080`
- MailHog UI: `http://localhost:8025` (see email codes in dev)
- Postgres: `localhost:5432` · Redis: `localhost:6379`

The Android app in an emulator automatically hits `http://10.0.2.2:8080/`.

---

## ☁️ Deploy to Render

1. Push the project to GitHub
2. Create a Render Blueprint from the root — `render.yaml` is already included
3. In the Render dashboard, add **sync: false** secrets:
   - `RESEND_API_KEY`
   - `OPENROUTER_API_KEY`
   - `TATRA_API_KEY`
4. Deploy. Render will auto-create Postgres + Redis + Web Service and inject URLs.

After deploy, update `API_BASE_URL` in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://your-app.onrender.com/\"")
```

---

## 📱 What the App Can Do

### Auth flow
1. Welcome → Sign up (email + password + optional name)
2. 6-digit verification code is sent to email (Resend in prod / MailHog in dev)
3. PIN setup (6 digits, BCrypt hashed)
4. Home screen

Re-login: PIN in 2 seconds, fallback to email+password if PIN fails 5 times.

---

### Home screen
<img width="350" alt="1" src="https://github.com/user-attachments/assets/c06a376e-8fab-429d-a70d-47268c5fc601" />

- Shows **group name**, total **linked bank balance** (real PSD2 data if connected)
- **Per-member bank balances** — each participant's real account balance from their connected bank (falls back to contributed amount if no bank linked)
- **Monthly spending limit bar** — progress bar shows how much of the limit has been spent this month (green → red as limit approaches)
- **Logout button** in the header
- Group switcher chips (horizontal scroll, "+ New" at the end)
- **3 action buttons**: Manage · Create/Join · Insights
- **Participants section** — shows up to 4 members with real balances, "See all" → full group detail
- **Join a group** field at the bottom — enter invite code quickly

### Group Management (Manage button)
<img width="350" alt="2" src="https://github.com/user-attachments/assets/2b241c90-c014-4db3-b48b-560e0d0e4eb5" />

- Set a **monthly spending limit** for the group (e.g. €4,000 limit on a €5,000 balance)
- See current month's stats: limit / spent / remaining with a progress bar
- Quick preset amounts: €1,000 / €2,000 / €4,000 / €10,000
- **Connect Bank** section at the bottom — navigates to PSD2 bank connection

### Group Detail (See All button)
<img width="350" alt="зображення" src="https://github.com/user-attachments/assets/6af16d13-26ec-401d-89d4-4a2a4cf3a80e" />

- Full member list with real bank balances
- **Owner can kick members** (red remove button per member, with confirmation dialog)
- **Non-owners can leave the group** (danger zone, confirmation dialog)
- **Invite member** button → InviteMemberScreen

### Create / Join Screen (Create/Join button)
Two tabs:

**Create tab**

<img width="350" alt="зображення" src="https://github.com/user-attachments/assets/d30f3b33-f232-4f74-b548-f8afbb6f09a6" />

- Enter group name → create instantly
- Quick name suggestions: Family, Friends, Work team, Trip 2025, Roommates

**Join tab**

<img width="350" alt="зображення" src="https://github.com/user-attachments/assets/252b1ea3-2ddb-4b48-8d14-e90009fe15d0" />

- Enter **Group ID** (UUID from the invite email) + **6-digit code** (from the same email)
- Calls `POST /accounts/{id}/members/join-with-code` — invitee joins themselves, no owner action needed

### Invite Member Flow (owner side)
<img width="350" alt="зображення" src="https://github.com/user-attachments/assets/c8224cae-24ed-4ea6-b80a-7eae466fc157" />

- Owner goes to: See All → Invite member
- Enters the invitee's email address (they must have an EquiPay account)
- Taps "Send invite"
- Backend emails the invitee with:
  - The **Group ID** (UUID) to paste into the Join tab
  - A **6-digit code** that expires in 10 minutes
- Success screen shows the owner exactly what the invitee should do next
- Owner does **not** need to confirm anything — the invitee joins themselves

### AI Assistant (central mic button in bottom bar)
<img width="350" alt="зображення" src="https://github.com/user-attachments/assets/c884efcc-30b4-4704-a823-cd076ca6bf5a" />

The bottom bar mic button opens the full **AI Assistant screen** with:

- **Voice mode** (default) — large central mic button, animated waveform during listening
- **Chat mode** — full text chat with message bubbles
- **Camera mode** — snap a receipt photo → AI reads merchant + amount
- **Gallery mode** — pick an existing receipt image

The assistant has **full app context**:
- Knows the current group name, all members, and their balances
- Sees the last 20 transactions
- Knows monthly totals and top merchants

**What you can ask:**
- "Split 45€ for pizza between us" → shows a **Split** action button to go directly to payment
- "Who owes the most?" → AI answers based on real balance data
- "What did we spend this month?" → calculated from actual transactions
- "Go to history" → shows a **Navigate** button that takes you there
- "Financial tips for our group" → personalized Gemini advice
- "How do I invite someone?" → explains the flow
- Language auto-detection — answers in English, Slovak, Russian, Ukrainian, German, Czech

The assistant remembers the last 10 messages (multi-turn conversation).

**Language selector** in the header — choose speech recognition language:
🇺🇸 English · 🇸🇰 Slovenčina · 🇷🇺 Русский · 🇺🇦 Українська · 🇩🇪 Deutsch

### Voice Split (legacy flow, also accessible)
- Tap mic → speak → AI parses amount + merchant + split mode
- Shows confirmation card → Confirm → goes to pre-filled NewPaymentScreen

### Bank Connection (PSD2)
- Tatra banka: real PSD2 sandbox flow (OAuth consent → real balance)
- SLSP / ČSOB / VÚB: mock consent flow
- Connected bank balances appear per-member on the Home screen and Group Detail

### Virtual Cards
- Create a virtual card per group
- Freeze / unfreeze
- Shows last 4 digits

### Transactions
- **New Payment** — enter amount, merchant, category, split mode (equal / solo / smart AI)
- **Smart Split** — AI analyzes who contributed less and weights the split accordingly
- **History** — all transactions grouped by Today / Yesterday with split details per member
- **Insights** — weekly spending bar chart, top categories, AI-generated group tip

---

## 🔑 API Endpoints

### Auth
| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/auth/register` | — |
| POST | `/auth/verify-email` | — |
| POST | `/auth/login` | — |
| POST | `/auth/login-pin` | — |
| POST | `/auth/pin` | 🔒 |
| POST | `/auth/refresh` | — |
| POST | `/auth/logout` | 🔒 |

### Users
| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/users/me` | 🔒 |
| PUT | `/users/me` | 🔒 |
| GET | `/users/search?q=` | 🔒 |
| GET | `/users/by-email?email=` | 🔒 |

### Accounts (Groups)
| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/accounts` | 🔒 |
| POST | `/accounts` | 🔒 |
| GET | `/accounts/{id}` | 🔒 |
| GET | `/accounts/{id}/linked-balance` | 🔒 |
| POST | `/accounts/{id}/members` | 🔒 |
| POST | `/accounts/{id}/members/request-invite` | 🔒 (owner only) |
| POST | `/accounts/{id}/members/confirm-invite` | 🔒 (owner only) |
| POST | `/accounts/{id}/members/join-with-code` | 🔒 (invitee) |
| DELETE | `/accounts/{id}/members/me` | 🔒 |
| DELETE | `/accounts/{id}/members/{userId}` | 🔒 (owner only) |
| POST | `/accounts/{id}/add-funds` | 🔒 |
| GET | `/accounts/{id}/limit` | 🔒 |
| POST | `/accounts/{id}/limit` | 🔒 |

### Cards
| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/cards` | 🔒 |
| POST | `/cards` | 🔒 |
| POST | `/cards/{id}/freeze` | 🔒 |
| GET | `/accounts/{id}/cards` | 🔒 |

### Banks
| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/banks/available` | 🔒 |
| GET | `/banks/connections` | 🔒 |
| POST | `/banks/tatra/connect/start` | 🔒 |
| DELETE | `/banks/connections/{id}` | 🔒 |

### Transactions
| Method | Endpoint | Auth |
|--------|----------|------|
| GET | `/transactions` | 🔒 |
| POST | `/transactions` | 🔒 |
| GET | `/transactions/{id}` | 🔒 |
| GET | `/accounts/{id}/transactions` | 🔒 |
| GET | `/insights` | 🔒 |

### AI
| Method | Endpoint | Auth |
|--------|----------|------|
| POST | `/ai/voice-parse` | 🔒 |
| POST | `/ai/receipt-parse` | 🔒 |
| POST | `/ai/smart-split` | 🔒 |
| GET | `/ai/insights-hint` | 🔒 |
| POST | `/ai/chat` | 🔒 |

🔒 = requires `Authorization: Bearer <access_token>`

---

## 🏗 Architecture

```
┌─────────────────┐  HTTPS/JSON  ┌──────────────────────┐  LLM   ┌─────────────┐
│  Android App    │─────────────▶│  Ktor backend        │───────▶│ OpenRouter  │
│  Compose        │              │  - auth (JWT)        │        │ (Gemini 2.5)│
│  Retrofit       │              │  - accounts/groups   │        └─────────────┘
│  StateFlow      │              │  - cards             │        ┌─────────────┐
│  SpeechRec      │              │  - transactions      │───────▶│ Tatra PSD2  │
│  EncryptedPrefs │              │  - AI chat/parse     │  PSD2  │ (real+mock) │
└─────────────────┘              │  - invite system     │        └─────────────┘
                                 │  PostgreSQL + Redis  │        ┌─────────────┐
                                 └──────────────────────┘  SMTP  │  Resend API │
                                                                  └─────────────┘
```

### Backend stack
- Kotlin 2.0.20 + Ktor 2.3.12 (Netty)
- Exposed ORM + HikariCP + PostgreSQL 16
- Redis 7 (Jedis) — rate limiting, invite codes, email verification
- Ktor Client (CIO) — OpenRouter / Resend / Tatra API calls
- JWT (Auth0 java-jwt), BCrypt (jbcrypt)
- simple-java-mail (SMTP fallback)

### Android stack
- Kotlin 2.0.20 + Jetpack Compose (BOM 2024.09.02)
- Material3 + Navigation Compose 2.8.0
- Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization
- EncryptedSharedPreferences (Security Crypto 1.1)
- ViewModel + StateFlow
- Android SpeechRecognizer (multi-language)

---

## 📂 Project Structure

```
EquiPay/
├── render.yaml
├── backend/
│   ├── .env.example
│   ├── docker-compose.yml
│   ├── docker/Dockerfile + init.sql
│   └── src/main/kotlin/com/equipay/api/
│       ├── Application.kt
│       ├── accounts/          AccountService.kt, AccountRoutes.kt
│       ├── ai/                AiService.kt, AiClient.kt
│       ├── auth/              AuthService.kt, AuthRoutes.kt, JwtService.kt
│       ├── banks/             BankService.kt, TatraBankClient.kt
│       ├── cards/             CardService.kt, CardRoutes.kt
│       ├── email/             EmailService.kt (Resend + SMTP)
│       ├── transactions/      TransactionService.kt, TransactionRoutes.kt
│       └── users/             UserService.kt
│
└── app/src/main/java/com/equipay/app/
    ├── auth/                  TokenStore, SessionViewModel, AuthRepository
    ├── network/               ApiClient, Apis, ApiDtos (all DTOs)
    ├── navigation/            Screen.kt, AppNavHost.kt
    └── ui/
        ├── screens/
        │   ├── auth/          Welcome, SignUp, EmailVerify, Pin, Login, Splash
        │   ├── HomeScreen.kt
        │   ├── AiAssistantScreen.kt    ← new full AI chat screen
        │   ├── VoiceAssistantScreen.kt ← legacy voice-parse flow
        │   ├── CreateJoinScreen.kt     ← create group / join by code
        │   ├── GroupDetailScreen.kt    ← full member list, kick, leave
        │   ├── GroupManageScreen.kt    ← monthly limit + connect bank
        │   ├── InviteMemberScreen.kt   ← owner sends invite email
        │   ├── NewPaymentScreen.kt
        │   ├── ConnectBankScreen.kt
        │   ├── VirtualCardScreen.kt
        │   ├── InsightsScreen.kt
        │   └── HistoryScreen.kt
        └── viewmodels/
            ├── HomeViewModel.kt        ← loads balances + limits in parallel
            └── ViewModels.kt           ← all other VMs
```

---

## ⚠️ Security Notes

### What's done correctly
- Passwords and PINs: BCrypt cost 12
- Refresh tokens stored as SHA-256 hash only (actual token in Android EncryptedSharedPreferences)
- JWT: 15-minute access token / 30-day refresh with rotation
- Rate limiting via Redis (login: 10/15min, PIN: 5/15min)
- Invite codes: stored in Redis with 10-minute expiry, rate-limited to 5 attempts

### Hackathon shortcuts (not for production)
- Mock last4 for virtual cards (real production needs a certified card issuer like Marqeta)
- IBAN and PSD2 consent tokens stored in plain DB (needs a token vault)
- Group balances in cents as BIGINT (fine for demo, needs audit trail in prod)

### API keys — ROTATE THESE
If any keys were shared in chat, revoke them immediately:
- https://openrouter.ai/settings/keys
- https://resend.com/api-keys
- Tatra banka dev portal

---

## 🎬 Hackathon Demo Script

1. `docker compose up --build` — starts in ~30 seconds
2. Open Android emulator
3. Sign up → check MailHog at `localhost:8025` for the code → enter PIN
4. Home (empty state) → tap **Create/Join** → Create tab → "Trip to Vienna"
5. Home → **See all** → **Invite member** → enter a second account's email
6. Second account opens EquiPay → **Create/Join** → **Join tab** → paste Group ID + code from email
7. Home → **Manage** → **Connect Bank** → connect Tatra banka (real PSD2 sandbox!)
8. Tap the mic button (bottom center) → say "Split 40 euros for pizza between us"
9. AI confirms → tap the **Split** action button → NewPayment opens pre-filled
10. Submit → History shows the transaction with per-member split statuses
11. Insights → Gemini-generated tip based on real spending data



