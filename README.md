<p align="center">
  <img src="design/logo-v3/icon-fluidcapsule-v3-preview.png" width="300" alt="FluidCapsule Logo / 流体胶囊 Logo">
</p>

<h1 align="center">FluidCapsule</h1>

<p align="center">
  <strong> ColorOS Pixel </strong><br>
  <strong>Interactive native live notifications on ColorOS and Pixel.</strong>
</p>
Turn Android notifications into interactive live notifications on supported ColorOS and Pixel devices.

Преобразует уведомления Android в интерактивные живые оповещения ColorOS Fluid Cloud или нативные живые уведомления Pixel, включая распознавание кодов подтверждения и ссылок верификации, белый список уведомлений, режим «Только коды для почты», переход к исходному уведомлению и быстрый ответ.

> [!IMPORTANT]
> FluidCapsule is an independent, unofficial open-source project. It is not affiliated with or endorsed by OPPO, ColorOS, Telegram, WeChat, or any other app vendor.
> FluidCapsule — независимый неофициальный проект с открытым исходным кодом. Он не связан, не авторизован и не одобрен OPPO, ColorOS, Telegram, WeChat или любыми другими разработчиками приложений.

## Features / Функции

### English

* Listen for notifications from user-selected apps.
* Optionally keep a local notification history. Choose a retention period in days, months, or years, or keep it forever. Turning recording off never deletes existing entries; history can be browsed by time or by folded app groups ordered by notification count.
* Extract one-time passwords from SMS notifications and display the code directly.
* Copy an OTP by tapping its capsule, with an optional masked clipboard preview.
* Recognize explicit email, account, and identity verification requests. A matching URL gets an `Open verification link` action; when the notification exposes no URL, its original destination can be opened instead. Ordinary links are not treated as verification links.
* Mirror whitelisted notifications with the original app icon, sender avatar, title, and message.
* Rebuild the observed sequence of successive WeChat and QQ summary updates so multiple messages remain visible in order while the listener stays connected.
* Keep up to eight pending capsule events in memory. Newer messages preempt the visible slot, while valid earlier messages return after the current event is opened, acted on, removed, or timed out.
* Open the source notification and, when it exposes Android `RemoteInput`, forward reply and mark-as-read actions.
* Add `Open reply` for actionless WeChat/QQ messages and a distinct trash-icon `Close` action that also dismisses the matching source notification from the notification shade.
* Show one-tap smart replies and adapt the reply panel accent color to the source app icon.
* Choose a light, dark, or system-following app appearance.
* Turn LocalSend transfers and Meituan order updates into progress-aware capsules while filtering recognized Meituan promotions.
* Reformat Speedtest's final `Test Complete` notification so download and upload results remain visible on one line.
* Configure every user-facing setting through an ADB-friendly CLI, including per-app rules and history retention.
* Keep notification processing available with explicit foreground-service and accessibility options.

### Русский

* Отслеживание уведомлений от приложений, выбранных пользователем.
* Возможность сохранения локальной истории уведомлений: выбор срока хранения в днях, месяцах, годах или бессрочно. Отключение записи не удаляет уже сохранённые записи; поддерживается просмотр по времени или по свёрнутым группам приложений с сортировкой по количеству уведомлений.
* Извлечение одноразовых паролей из SMS-уведомлений и их прямое отображение.
* Копирование кода подтверждения нажатием на капсулу с опциональной маскировкой предпросмотра в буфере обмена.
* Распознавание явных запросов подтверждения для почты, аккаунтов и личности. При наличии ссылки добавляется действие «Открыть ссылку подтверждения»; если ссылка не указана, можно открыть исходное уведомление. Обычные ссылки не считаются ссылками подтверждения.
* Дублирование уведомлений из белого списка с сохранением исходной иконки приложения, аватара отправителя, заголовка и текста сообщения.
* Восстановление наблюдаемой последовательности цепочек сообщений WeChat и QQ для корректного поочерёдного отображения нескольких сообщений, пока служба прослушивания активна.
* Сохранение в памяти до 8 ожидающих событий капсул; новые сообщения временно занимают активный слот, а актуальные предыдущие сообщения возвращаются после открытия, обработки, удаления или истечения времени текущего события.
* Открытие исходного уведомления; пересылка ответов и действий «Отметить как прочитанное», если источник поддерживает Android `RemoteInput`.
* Добавление кнопки «Открыть ответ» для сообщений WeChat/QQ без встроенных действий, а также отдельной кнопки «Закрыть» с иконкой корзины, которая также удаляет соответствующее исходное уведомление из шторки.
* Быстрые умные ответы в одно касание и адаптация акцентного цвета панели ответов под цвет иконки приложения-источника.
* Поддержка светлой, тёмной и системной темы оформления интерфейса.
* Преобразование передачи файлов LocalSend и статусов заказов Meituan в капсулы с индикатором прогресса с фильтрацией распознанной рекламы Meituan.
* Переформатирование финального уведомления Speedtest `Test Complete` для отображения результатов загрузки и отдачи в одну строку.
* Настройка всех пользовательских параметров через удобный для ADB консольный интерфейс (CLI), включая правила для отдельных приложений и сроки хранения истории.
* Явные параметры работы в приоритетном режиме (foreground service) и служб специальных возможностей (accessibility) для стабильной работы службы обработки уведомлений в фоне.

## Platform support / Поддержка платформ

### English

* Additional verified device: Pixel 11 Pro XL, Android 17 / API 37; see [Pixel notes](https://www.google.com/search?q=docs/PIXEL.md).
* Supported device: OPPO CPH2797 running Android 16 / API 36.
* Verified firmware baseline: `CPH2797_16.0.9.400(EX01)`.
* Promoted/live notifications must be enabled. The operating system owns the final live-notification rendering and may change it in a firmware update.

FluidCapsule 1.x intentionally sets `minSdk = 36`. Older Android versions and untested devices are outside the verified scope.

### Русский

* Дополнительно протестированное устройство: Pixel 11 Pro XL, Android 17 / API 37; см. [заметки по Pixel](https://www.google.com/search?q=docs/PIXEL.md).
* Поддерживаемое устройство: OPPO CPH2797 под управлением Android 16 / API 36.
* Проверенная базовая сборка прошивки: `CPH2797_16.0.9.400(EX01)`.
* Требуется включить закреплённые/живые оповещения. Окончательная отрисовка живых оповещений выполняется операционной системой, и её поведение может меняться при обновлениях прошивки.

В FluidCapsule 1.x параметр `minSdk` намеренно установлен на значение 36; старые версии Android и непротестированные устройства не входят в рамки гарантированной совместимости.

## Privacy model / Политика конфиденциальности

### English

FluidCapsule processes notification text locally and does not upload notification content. It requests internet access only to check the fixed official GitHub rule-subscription files when the app opens; a signed rule pack is downloaded and installed only after a tap. The subscription is on by default and can be disabled on the Rules page. OTPs, verification URLs, and reply text are not written to diagnostic logs. When notification history is enabled, captured text follows the selected day, month, year, or forever policy and can be deleted by entry, app, or in full. Turning recording off does not delete existing entries.

Read [Privacy and security](https://www.google.com/search?q=docs/PRIVACY.md) before enabling notification access, history, or accessibility features.

### Русский

FluidCapsule обрабатывает текст уведомлений локально на устройстве и не выгружает их содержимое в сеть. Приложение запрашивает доступ к интернету исключительно для проверки фиксированных официальных файлов подписки на правила на GitHub при запуске; подписанный пакет правил скачивается и устанавливается только после явного нажатия пользователем. Подписка включена по умолчанию и может быть отключена на странице «Правила». Одноразовые коды, ссылки подтверждения и тексты ответов не записываются в диагностические логи. При включённой истории уведомлений перехваченный текст хранится в соответствии с выбранным правилом (дни, месяцы, годы или бессрочно) и может быть удалён по отдельным записям, по приложениям или полностью; отключение самой записи не удаляет уже существующую историю.

Перед включением доступа к уведомлениям, истории или службам специальных возможностей ознакомьтесь с документом [Конфиденциальность и безопасность](https://www.google.com/search?q=docs/PRIVACY.md).

## Storage estimate / Расчёт объёма хранилища

### English

On the verified OPPO snapshot from 11 August 2026, 758 notification records occupied 663,552 bytes (0.63 MiB) in the SQLite database, or about 875 bytes per record including the current indexes and allocated pages. Using the current seven-day count as a steady-rate approximation gives about 108 records per day.

| Retention horizon | Approximate records | Linear database estimate | Conservative planning allowance |
| --- | --- | --- | --- |
| 1 year | 39,500 | 33 MiB | about 50 MiB |
| 3 years | 118,500 | 99 MiB | about 150 MiB |
| 10 years | 395,000 | 330 MiB | about 500 MiB |

At the current volume, even permanent retention is unlikely to cause severe database growth. Notification volume and message length can change, and SQLite may keep allocated pages after records are deleted, so these figures are capacity estimates rather than a storage guarantee. The more important trade-off for permanent retention is privacy: old notification text remains readable on the device until it is manually deleted or app data is cleared.

### Русский

На проверенном слепке данных OPPO от 11 августа 2026 года 758 записей уведомлений занимали в базе данных SQLite 663 552 байта (0,63 МиБ); с учётом существующих индексов и выделенных страниц это составляет в среднем около 875 байт на запись. Экстраполируя текущий объём за 7 дней как стабильное среднее значение, получаем около 108 записей в день.

| Срок хранения | Ожидаемое число записей | Линейная оценка размера БД | Консервативный запас объёма |
| --- | --- | --- | --- |
| 1 год | 39 500 | 33 МиБ | около 50 МиБ |
| 3 года | 118 500 | 99 МиБ | около 150 МиБ |
| 10 лет | 395 000 | 330 МиБ | около 500 МиБ |

При текущей интенсивности уведомлений даже бессрочное хранение едва ли приведёт к чрезмерному разрастанию базы данных. Объём уведомлений и длина текста могут со временем меняться, кроме того, SQLite может удерживать выделенные страницы после удаления записей, поэтому данные значения являются предварительной оценкой, а не гарантией ёмкости. Главный фактор при бессрочном хранении — это конфиденциальность: текст старых уведомлений остаётся доступным на устройстве до момента их ручного удаления или очистки данных приложения.

## Build / Сборка

### Requirements / Требования к окружению

* Android SDK 36
* JDK 21
* Android platform-tools（для команд ADB / for ADB commands）

Build, test, lint, and install the debug APK / Сборка, тестирование, проверка Lint и установка отладочного APK:

```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

```

Run the eight CPH2797 instrumentation checks / Запуск 8 инструментальных тестов для CPH2797:

```bash
adb -s SERIAL install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s SERIAL shell am instrument -w \
  io.github.venompool888.fluidcapsule.test/androidx.test.runner.AndroidJUnitRunner
./scripts/verify-cph2797.sh --serial SERIAL

```

The signed release build uses a keystore outside the repository and a password stored in macOS Keychain.

Для релизной сборки с подписью используется хранилище ключей (keystore) вне репозитория, пароль к которому хранится в связке ключей macOS Keychain.

```bash
./scripts/build-release.sh

```

The output is `app/build/outputs/apk/release/app-release.apk`. APKs and signing material are intentionally excluded from Git.

Итоговым файлом сборки является `app/build/outputs/apk/release/app-release.apk`. Файлы APK и материалы подписи намеренно исключены из репозитория Git.

## Upgrade / Обновление

Download the signed APK from [Releases](https://github.com/Venompool888/FluidCapsule/releases) and install it over the existing release. The package name and release signing key stay the same, so settings and local history are retained. Do not uninstall or clear app data when upgrading. A debug APK uses a different signing key and cannot replace a release installation.

Скачайте подписанный релизный APK из раздела [Releases](https://github.com/Venompool888/FluidCapsule/releases) и установите поверх существующей версии. Имя пакета и ключ подписи остаются прежними, поэтому все параметры и локальная история уведомлений сохранятся (включая выбранное бессрочное хранение). При обновлении не удаляйте приложение и не очищайте данные; отладочный APK подписан другим ключом и не может быть установлен поверх релизной версии.

Protected ADB history export is documented in [CLI](https://www.google.com/search?q=docs/CLI.md). Exported files contain complete notification text and should remain private.

Защищённый экспорт истории через ADB описан в разделе [CLI](https://www.google.com/search?q=docs/CLI.md). Экспортированные файлы содержат полный текст уведомлений и должны храниться конфиденциально.

## Initial setup / Начальная настройка

### English

1. Install and open FluidCapsule.
2. Grant notification-listener and notification-posting access.
3. Select trusted source apps in the notification whitelist.
4. Allow promoted/live notifications for FluidCapsule on the supported ColorOS or Pixel version.
5. Optionally enable the explicit keep-alive controls if the system stops the listener in the background.

### Русский

1. Установите и откройте FluidCapsule.
2. Предоставьте разрешения на доступ к чтению и отправке уведомлений.
3. Добавьте доверенные приложения-источники в белый список уведомлений.
4. Включите отображение живых оповещений для FluidCapsule в настройках поддерживаемой версии ColorOS или Pixel.
5. При необходимости включите опции поддержания фоновой активности, если система закрывает службу прослушивания в фоне.

For repeatable device configuration, see [ADB CLI](https://www.google.com/search?q=docs/CLI.md).

Инструкции по быстрой и воспроизводимой настройке через консоль приведены в [ADB CLI](https://www.google.com/search?q=docs/CLI.md).

## How it works / Принцип работы

```text
Source notification / explicitly supported foreground status
Исходное уведомление / явно поддерживаемый статус на переднем плане
        ↓
NotificationListenerService / package-scoped accessibility adapter
Служба чтения уведомлений / адаптер специальных возможностей с привязкой к пакетам
        ↓
Normalize → OTP parser / known-app adapter / whitelist policy
Нормализация → парсер OTP / адаптер известных приложений / белый список
        ↓
CapsuleEvent / событие капсулы
        ↓
In-memory priority queue → one visible capsule slot
Очередь приоритетов в памяти → один активный слот капсулы
        ↓
Android 16 promoted ongoing notification
Продвигаемое закреплённое уведомление Android 16

```

More detail is available in [Architecture](https://www.google.com/search?q=docs/ARCHITECTURE.md) and [ColorOS notes](https://www.google.com/search?q=docs/COLOROS.md).

Подробная информация доступна в [описании архитектуры](https://www.google.com/search?q=docs/ARCHITECTURE.md) и [заметках по ColorOS](https://www.google.com/search?q=docs/COLOROS.md).

## Important limitations / Важные ограничения

### English

* Direct reply is possible only when the source notification supplies a valid `RemoteInput` action. FluidCapsule cannot invent a private sending API for another app.
* Smart replies are sent immediately when tapped. Manually typed replies still require the Send button.
* OEM live-notification behavior can change between ColorOS releases.
* Verified devices are CPH2797 on the documented Android 16 baseline and Pixel 11 Pro XL on Android 17.
* Accessibility UI automation for apps without native reply actions is not implemented. The optional accessibility service does not read screen content.
* The project does not include third-party APKs, decompiled source, proprietary assets, private protocols, or account-bypass features.

### Русский

* Прямой ответ возможен только в том случае, если исходное уведомление предоставляет корректное действие `RemoteInput`; FluidCapsule не создаёт закрытые API для отправки сообщений в сторонние приложения.
* Нажатие на умный ответ приводит к немедленной отправке сообщения; для набранного вручную текста по-прежнему требуется нажатие кнопки отправки.
* Поведение фирменных живых уведомлений может отличаться в зависимости от версии ColorOS.
* Проверенными устройствами являются CPH2797 на базовой сборке Android 16 и Pixel 11 Pro XL на Android 17.
* Автоматизация интерфейса через службу специальных возможностей для приложений без нативной поддержки ответа не реализована; опциональная служба специальных возможностей не читает содержимое экрана.
* Проект не включает сторонние APK, декомпилированный исходный код, проприетарные ресурсы, закрытые протоколы или методы обхода авторизации учётных записей.

## Quality gate / Критерии качества

### English

* Local JUnit suite: 92 tests.
* Pixel instrumentation: 4 checks for notification construction and history export. The existing OPPO suite was not rerun for 1.1.2.
* CPH2797 instrumentation suite: 8 tests, including action fallback structure, continuous history scrolling, and system notification queue preemption/restoration.
* Required build gate: unit tests, debug APK, instrumentation APK, and Android lint with no findings.
* GitHub Actions runs unit tests, the debug build, and lint for every push to `main` and every pull request.

### Русский

* Локальный набор тестов JUnit: 92 теста.
* Инструментальные тесты Pixel: 4 проверки создания уведомлений и экспорта истории. Для версии 1.1.2 набор тестов для OPPO повторно не запускался.
* Инструментальный набор тестов CPH2797: 8 проверок, включая структуру резервных действий (fallback), контейнер плавной прокрутки истории и вытеснение/восстановление системной очереди уведомлений.
* Обязательный порог прохождения сборки: модульные тесты, сборка debug APK, сборка APK инструментальных тестов и отсутствие замечаний Android Lint.
* GitHub Actions запускает модульные тесты, сборку debug-версии и проверку Lint при каждом пуше в ветку `main` и для каждого Pull Request.

## Contributing / Участие в разработке

Bug reports and pull requests are welcome. Remove notification text, OTPs, usernames, avatars, device serials, and local paths from logs or screenshots before posting. See [CONTRIBUTING.md](https://www.google.com/search?q=CONTRIBUTING.md).

Приветствуются отчёты об ошибках и Pull Request. Перед отправкой логов или скриншотов удаляйте из них текст уведомлений, одноразовые пароли, имена пользователей, аватары, серийные номера устройств и локальные пути. Подробнее см. в [CONTRIBUTING.md](https://www.google.com/search?q=CONTRIBUTING.md).

## License / Лицензия

[MIT](https://www.google.com/search?q=LICENSE) © 2026 FluidCapsule contributors.

Проект распространяется под [лицензией MIT](https://www.google.com/search?q=LICENSE), © 2026 FluidCapsule contributors.
