# EquiPay v2 — Full Working App

Рабочее приложение для **TatraBank Challenge** — общий групповой счёт с PSD2 open banking, виртуальными картами на участника, AI split через Gemini, голосовым ассистентом. Всё соединено с реальным backend.

Монорепа: **Android** (Kotlin + Jetpack Compose) + **Backend** (Ktor + PostgreSQL + Redis, deploy на Render или Docker).

---

## 🚀 Быстрый старт (local через Docker)

```bash
cd backend
cp .env.example .env
# Отредактируй .env — добавь свои ключи OpenRouter / Resend / Tatra
docker compose up --build
```

После старта:
- API: http://localhost:8080
- MailHog UI: http://localhost:8025 (видны dev-email коды)
- Postgres: localhost:5432 · Redis: localhost:6379

Android приложение в эмуляторе автоматически ходит на `http://10.0.2.2:8080/`.

---

## ☁️ Deploy на Render

1. Залей проект на GitHub
2. Создай Render Blueprint из корня репо — файл `render.yaml` уже есть
3. В Render dashboard добавь **sync: false** secrets:
   - `RESEND_API_KEY`
   - `OPENROUTER_API_KEY`
   - `TATRA_API_KEY`
4. Deploy. Render автоматически создаст Postgres + Redis + Web Service, подставит URL'ы через `fromDatabase` / `fromService`

После деплоя обнови `API_BASE_URL` в `app/build.gradle.kts` на свой Render URL:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://equipay-api.onrender.com/\"")
```

---

## ⚠️ Безопасность

### Что хранится на backend (для хакатона, НЕ для production)
- Mock last4 виртуальных карт (в проде должен быть card issuer типа Marqeta)
- IBAN и PSD2 consent tokens в `bank_connections`
- Балансы групповых счетов в центах (`BIGINT`)

**В production это нарушает PCI DSS.** Нужна интеграция с сертифицированным card issuer и token vault.

### Что делается правильно
- Пароли и PIN — BCrypt cost 12
- Refresh tokens в БД только как SHA-256 хеш (сам токен — только в Android EncryptedSharedPreferences)
- JWT access 15мин / refresh 30 дней с rotation
- Rate limiting через Redis (login 10/15мин, PIN 5/15мин)
- Audit log всех auth-событий

### API ключи
- **НИКОГДА** не коммить `.env` — он в `.gitignore`
- В Render Dashboard все ключи помечены `sync: false` — их нужно вручную ввести после деплоя
- `.env.example` содержит пустые placeholder'ы

---

## 📱 Что умеет приложение

### Auth flow
1. Welcome → Sign up (email + password)
2. Verification code приходит на email (через Resend в prod / MailHog в dev)
3. PIN setup (6 цифр, BCrypt)
4. Home

Повторный вход: PIN за 2 секунды, fallback на email+пароль если 5 раз неверный PIN.

### Main features
- **Групповые счета** — `Trip to Vienna` и т.д. Создание, инвайты по email, свитчер между группами
- **Virtual cards** — создание через backend, freeze/unfreeze
- **PSD2 bank connections** — Tatra banka (реальный API sandbox с fallback), SLSP/ČSOB/VÚB (mock consent flow)
- **Pay & Split** — при клике `Pay & Split` backend:
  1. Создаёт transaction со split по участникам
  2. Fan-out: для каждого участника пытается списать с его bank connection
     - Для Tatra banka — реальный POST на sandbox
     - Для других — mock success
  3. Отмечает split статусы (completed/failed)
  4. Обновляет баланс группы и contributed per user
- **Smart split (AI)** — `/ai/smart-split` использует Gemini 2.5 Flash через OpenRouter. Анализирует контрибьюции участников и предлагает более справедливый split
- **Voice Split** — Android SpeechRecognizer записывает речь → POST на `/ai/voice-parse` → Gemini парсит amount/merchant/split/category → Confirm → prefilled NewPaymentScreen
- **Insights** — `/insights` агрегирует транзакции по неделе + `/ai/insights-hint` даёт реальный совет от Gemini типа "Yehor paid 70% this week"
- **History** — `/transactions` с группировкой Today / Yesterday

### Global Mic FAB
Центральная кнопка микрофона теперь **всегда** в BottomBar (Home / Insights / Card / History), а не только на одном экране.

---

## 🏗 Архитектура

```
┌─────────────────┐  HTTPS/JSON  ┌──────────────────────┐  LLM   ┌─────────────┐
│  Android App    │─────────────▶│  Ktor backend        │───────▶│ OpenRouter  │
│  Compose        │              │  - auth (JWT)        │        │ (Gemini 2.5)│
│  Retrofit       │              │  - accounts          │        └─────────────┘
│  StateFlow      │              │  - cards             │        ┌─────────────┐
│  SpeechRec      │              │  - transactions      │───────▶│ Tatra PSD2  │
│  EncryptedPrefs │              │  - fanout payments   │  PSD2  │ (real+mock) │
└─────────────────┘              │  - AI proxy          │        └─────────────┘
                                 │                      │        ┌─────────────┐
                                 │  PostgreSQL + Redis  │───────▶│  Resend API │
                                 └──────────────────────┘  SMTP  └─────────────┘
```

### Backend stack
- Kotlin 2.0.20 + Ktor 2.3.12 (Netty)
- Exposed ORM + HikariCP + PostgreSQL 16
- Redis 7 (Jedis) — rate-limit, email codes
- Ktor Client (CIO) — вызовы OpenRouter / Resend / Tatra
- JWT (Auth0 java-jwt), BCrypt (jbcrypt)
- simple-java-mail (fallback SMTP)

### Android stack
- Kotlin 2.0.20 + Jetpack Compose (BOM 2024.09.02)
- Material3 + Navigation Compose 2.8.0
- Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization
- EncryptedSharedPreferences (Security Crypto 1.1)
- ViewModel Compose + StateFlow
- Android SpeechRecognizer

---

## 📂 Структура

```
EquiPay/
├── render.yaml                   # Render Blueprint
├── backend/                      # Ktor API
│   ├── .env.example
│   ├── .gitignore
│   ├── docker-compose.yml
│   ├── docker/
│   │   ├── Dockerfile
│   │   └── init.sql              # схема БД
│   ├── build.gradle.kts
│   └── src/main/
│       ├── resources/
│       │   ├── application.conf  # HOCON + ENV override
│       │   └── logback.xml
│       └── kotlin/com/equipay/api/
│           ├── Application.kt
│           ├── config/AppConfig.kt
│           ├── db/{Tables, DbFactory}.kt
│           ├── redis/RedisClient.kt
│           ├── email/EmailService.kt       # Resend + SMTP
│           ├── util/Crypto.kt
│           ├── auth/                       # /auth/*
│           ├── users/                      # /users/*
│           ├── accounts/                   # /accounts/*
│           ├── cards/                      # /cards/*
│           ├── banks/                      # /banks/* + TatraBankClient
│           ├── transactions/               # /transactions/* + /insights
│           └── ai/                         # /ai/* (OpenRouter proxy)
│
└── app/                          # Android Compose
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        └── java/com/equipay/app/
            ├── MainActivity.kt
            ├── EquiPayApp.kt              # Application class
            ├── auth/
            │   ├── TokenStore.kt          # EncryptedSharedPreferences
            │   ├── AccessTokenHolder.kt
            │   ├── AuthRepository.kt
            │   └── SessionViewModel.kt
            ├── network/
            │   ├── Dtos.kt, ApiDtos.kt    # request/response DTOs
            │   ├── AuthApi.kt, Apis.kt    # все Retrofit интерфейсы
            │   └── ApiClient.kt           # OkHttp + auto-refresh 401
            ├── navigation/
            │   ├── Screen.kt
            │   └── AppNavHost.kt          # guard'ы по SessionState
            └── ui/
                ├── theme/                 # Color/Type/Theme
                ├── components/
                │   ├── Avatar, SectionLabel, Sparkline
                │   └── EquiBottomBar       # глобальный mic FAB
                ├── viewmodels/             # AppState + все VM
                └── screens/
                    ├── auth/               # Welcome, SignUp, EmailVerify, Pin, Login, Splash
                    ├── HomeScreen.kt       # + group switcher + empty state
                    ├── CreateGroupScreen.kt  # НОВЫЙ
                    ├── VoiceAssistantScreen.kt  # + SpeechRecognizer
                    ├── NewPaymentScreen.kt # + prefill от голоса
                    ├── ConnectBankScreen.kt
                    ├── VirtualCardScreen.kt
                    ├── InsightsScreen.kt
                    └── HistoryScreen.kt
```

---

## 🎬 Demo скрипт на хакатон

1. `docker compose up --build` — 30 сек старт
2. Открой Android эмулятор
3. Sign up → получи код в MailHog / email → PIN
4. Home → empty state → **Create group** → "Trip to Vienna" + 2-3 email друзей
5. Home → Connect Bank → подключи Tatra banka (реальный PSD2 sandbox call!)
6. Card → Create Virtual Card → получаешь `•••• XXXX`
7. Tap mic FAB (внизу центр) → скажи "Split 40 euros for pizza between us"
8. Gemini подтверждает → Confirm → NewPayment уже заполнен
9. **Pay & Split** → backend делает fanout: Tatra→реальный POST, остальные→mock
10. History показывает транзакцию со статусами по участникам
11. Insights показывает Gemini-совет по распределению

---

## 🔑 API Endpoints reference

### Auth
- `POST /auth/register` · `POST /auth/verify-email` · `POST /auth/login` · `POST /auth/login-pin` · `POST /auth/pin` 🔒 · `POST /auth/refresh` · `POST /auth/logout` · `GET /auth/me`

### Users
- `GET /users/me` 🔒 · `PUT /users/me` 🔒 · `GET /users/search?q=` 🔒 · `GET /users/by-email?email=` 🔒

### Accounts (groups)
- `GET /accounts` 🔒 · `POST /accounts` 🔒 · `GET /accounts/{id}` 🔒 · `POST /accounts/{id}/members` 🔒 · `POST /accounts/{id}/add-funds` 🔒

### Cards
- `GET /cards` 🔒 · `POST /cards` 🔒 · `POST /cards/{id}/freeze` 🔒 · `GET /accounts/{id}/cards` 🔒

### Banks
- `GET /banks/available` 🔒 · `GET /banks/connections` 🔒 · `POST /banks/connect` 🔒 · `DELETE /banks/connections/{id}` 🔒

### Transactions
- `GET /transactions` 🔒 · `POST /transactions` 🔒 · `GET /transactions/{id}` 🔒 · `GET /accounts/{id}/transactions` 🔒 · `GET /insights` 🔒

### AI
- `POST /ai/voice-parse` 🔒 · `POST /ai/smart-split` 🔒 · `GET /ai/insights-hint` 🔒

🔒 = требует `Authorization: Bearer <access_token>`

---

## ⚠️ ВАЖНО: Роти свои ключи

Ключи, которые ты запостил в чате, **скомпрометированы**:
- https://openrouter.ai/settings/keys → revoke
- https://resend.com/api-keys → revoke
- Tatra banka dev portal → revoke

Создай новые и подставь в `.env` (local) и Render Dashboard (prod).
