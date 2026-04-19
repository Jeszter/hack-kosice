# HackKosice local server

Это локальный backend для Android-приложения с общим виртуальным счётом.

## Что уже есть

- регистрация
- логин
- JWT авторизация
- PostgreSQL
- профиль текущего пользователя
- создание shared wallet
- добавление участников в wallet
- просмотр своих wallet
- mock endpoint для банковских подключений

## Запуск PostgreSQL

```bash
docker compose up -d
```

## Переменные окружения

Для PowerShell:

```powershell
$env:JWT_SECRET="change-this-to-a-long-random-secret-key-with-at-least-32-characters"
$env:DB_URL="jdbc:postgresql://localhost:5432/hackkosice"
$env:DB_USERNAME="hackkosice"
$env:DB_PASSWORD="hackkosice"
```

Для bash:

```bash
export JWT_SECRET="change-this-to-a-long-random-secret-key-with-at-least-32-characters"
export DB_URL="jdbc:postgresql://localhost:5432/hackkosice"
export DB_USERNAME="hackkosice"
export DB_PASSWORD="hackkosice"
```

## Запуск сервера

```bash
./gradlew bootRun
```

Сервер поднимется на:

```text
http://localhost:8080
```

## Основные маршруты

### Auth

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

### Wallets

```text
POST /api/wallets
GET  /api/wallets
GET  /api/wallets/{walletId}
POST /api/wallets/{walletId}/members
```

### Bank connections

```text
GET  /api/bank-connections
POST /api/bank-connections/mock-link
```

## Примеры запросов

Регистрация:

```json
POST /api/auth/register
{
  "email": "yehor@example.com",
  "password": "StrongPass123",
  "fullName": "Yehor Tatarenko"
}
```

Логин:

```json
POST /api/auth/login
{
  "email": "yehor@example.com",
  "password": "StrongPass123"
}
```

Создание wallet:

```json
POST /api/wallets
Authorization: Bearer <token>
{
  "name": "Trip to Vienna",
  "currency": "EUR"
}
```

Добавление mock bank connection:

```json
POST /api/bank-connections/mock-link
Authorization: Bearer <token>
{
  "bankName": "Tatra banka",
  "accountName": "Personal account",
  "iban": "SK1234567890123456789012",
  "balance": 420.50,
  "currency": "EUR"
}
```
