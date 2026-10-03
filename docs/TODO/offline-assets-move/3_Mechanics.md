# Механика переезда (как, технически)

## Что уже есть

- `AppDataLocation` (`util/AppDataLocation.kt`): выбор места (INTERNAL /
  EXTERNAL_APP / SDCARD_APP / EXTERNAL_SHARED), `dirFor(location, leaf)`,
  `migrate()` с проверкой места, тест `AppDataLocationTest` (8 тестов).
- `SttModelStorage` (`data/speech/SttModelStorage.kt`): тонкая обёртка над
  `AppDataLocation` для одного payload — образец для `TerminalRootfsStorage`,
  `SubpackStorage` и т.д.
- `CustomSttModels.migrateTo(source, target)` (`data/speech/`): образец
  миграции нескольких записей; source передаётся параметром, потому что
  встроенный переезд уже перевернул настройку.
- `StorageLocationPicker` в `GigaAMModelSection.kt`: готовый UI переезда.
- `AppStorageUsage` (`util/`) + карточка в About: замер результата.

## Паттерн на каждый payload

1. Новый `XxxStorage(context)` по образцу `SttModelStorage` со своим leaf.
2. Точка первого запуска: вместо `assets.open(...)` + копия в `files/` —
   копия в `storage.currentDir()` (по умолчанию INTERNAL, поведение не
   меняется, пока пользователь не переедет).
3. Пикер места в настройках фичи (копия `StorageLocationPicker` + вызов
   `xxxStorage.migrateTo()` + `CustomSttModels`-миграция при общем переезде).
4. Приёмка на тестовом телефоне: `du` до/после, карточка Storage в About,
   фича работает с SD (терминал стартует, сабпак ставится).

## Проверка размера

После каждой очереди: `AppStorageUsage` на устройстве + `research-size.md`.
Ничего не записывать как выигрыш без замера на телефоне (урок SIZE-007:
APK и installed — разные числа).
