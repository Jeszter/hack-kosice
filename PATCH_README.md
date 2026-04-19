# EquiPay — 4 Interaction Modes patch

Добавляет полноценные Voice / Chat / Camera / Gallery режимы в SplitFlow Assistant. Дает доп. очки по критерию **Interaction Modes: "more modes = more points"**.

## Что где лежит — просто распакуй zip в корень проекта

Структура архива точно совпадает с твоим `hack-kosice`, файлы заменяются **по месту**.

### Backend (2 файла)
```
backend/src/main/kotlin/com/equipay/api/ai/
├── AiClient.kt        ← ЗАМЕНИТЬ (добавлен chatWithImage() для Vision)
└── AiService.kt       ← ЗАМЕНИТЬ (добавлен /ai/receipt-parse endpoint)
```

### Android (5 файлов + 1 gradle)
```
app/build.gradle.kts   ← ЗАМЕНИТЬ (добавлен androidx.exifinterface)

app/src/main/
├── AndroidManifest.xml                                           ← ЗАМЕНИТЬ (CAMERA + FileProvider)
├── res/xml/file_paths.xml                                        ← НОВЫЙ ФАЙЛ
└── java/com/equipay/app/
    ├── network/
    │   ├── ApiDtos.kt                                            ← ЗАМЕНИТЬ
    │   └── Apis.kt                                               ← ЗАМЕНИТЬ
    ├── ui/viewmodels/ViewModels.kt                               ← ЗАМЕНИТЬ
    └── ui/screens/VoiceAssistantScreen.kt                        ← ЗАМЕНИТЬ
```

## Что изменилось

### Voice (было)
Уже работал — SpeechRecognizer → `/ai/voice-parse` → Gemini Flash парсит.

### Chat (новое)
Прямо в экране — `BasicTextField` с send-кнопкой. Отправляет текст в то же `/ai/voice-parse` (бэку без разницы, он просто парсит текст). Плюс 3 clickable example-chip'а для быстрого демо.

### Camera (новое)
- Запрашивает `Manifest.permission.CAMERA`
- Открывает системную камеру через `ActivityResultContracts.TakePicture()` + FileProvider
- Даунскейлит фото до 1024px (чтобы не гонять 10 МБ на бэк), фиксит EXIF-ориентацию
- Кодирует в JPEG 80% + base64
- Отправляет на новый `/ai/receipt-parse`
- Gemini 2.5 Flash читает чек (он multimodal) и возвращает merchant + amount + category

### Gallery (новое)
Тот же flow, только через `ActivityResultContracts.GetContent("image/*")` — выбор любого фото из галереи. Удобно для демо — можно заранее сохранить в галерею скриншот чека.

### Reset (4-ая кнопка)
Кнопка "Reset" рядом для сброса состояния — UX нужен, чтобы попробовать другой режим без перезахода.

## Что НЕ изменилось (можешь не трогать)
- Всё auth
- Home / Cards / Banks / Transactions / Insights / History
- Навигация (route `VoiceAssistant` уже работал)
- Backend: только AiClient и AiService тронуты, всё остальное нетронуто

## Как тестировать

### Local (Docker)
```bash
cd backend
docker compose up --build -d
```

В Android emulator открой приложение → микрофон снизу → SplitFlow Assistant:

1. **Voice:** скажи "Split 40 euros for pizza between us"
2. **Chat:** напиши "I paid 12€ for Uber" или жми на example
3. **Camera:** наведи на любой чек или скриншот
4. **Gallery:** выбери картинку из Photos

Все 4 режима конвертируются в один `VoiceParseResponse` → Confirm → предзаполнение NewPayment.

### Важные моменты
- `OPENROUTER_API_KEY` обязателен в `.env` — без него Vision вернёт fallback "Couldn't scan receipt"
- Gemini 2.5 Flash через OpenRouter **поддерживает multimodal** — проверено по схеме `image_url` OpenAI-совместимого API
- На реальном устройстве (не эмуляторе) Camera и Gallery работают нативно; в эмуляторе тебе нужно сначала положить картинку в `/sdcard/Pictures` через drag-and-drop

## Демо-скрипт на хакатон

1. Открыть SplitFlow Assistant
2. Tap **Voice** → "Forty euros for pizza with the squad" → Gemini парсит
3. Reset
4. Tap **Chat** → жмём example chip "Museum tickets 60€ split 3 ways" → парсится мгновенно
5. Reset
6. Tap **Camera** → снимаем любой чек со стола → Gemini Vision читает → Confirm → Pay & Split
7. Reset
8. Tap **Gallery** → тот же чек из фото → подтверждаем

Комментарий жюри: "All 4 modes route through the same pipeline, so users never lose context. Same confirmation step, same NewPayment screen. That's Interaction Breadth + Consistency + Clarity."
