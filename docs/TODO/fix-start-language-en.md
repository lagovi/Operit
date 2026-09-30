# TODO: стартовый язык — английский по умолчанию

## Проблема
`LocaleUtils.resolveSupportedLanguageCode()` для неподдерживаемой системной локали (ru-RU и т.п.) возвращает её как есть → Android фолбечит на `values/` (китайский). Отсюда «включать английский наугад».

## Что сделать
Файл: `app/src/main/java/com/ai/assistance/operit/util/LocaleUtils.kt`, конец функции `resolveSupportedLanguageCode` (~строка 288).

Заменить:
```kotlin
        return normalizedCode
```
на:
```kotlin
        // Fork behavior: a system locale the app does not support (e.g. ru-RU) must
        // not leak through as-is — Android would then fall back to values/ (Chinese),
        // which is exactly the "start language is a lottery" complaint. Default to
        // English, the only fully maintained non-Chinese locale.
        return LanguageCodes.ENGLISH
```

## Проверка
1. CI-сборка зелёная.
2. Чистая установка на телефоне (ru-RU система, БЕЗ патча datastore) → первый запуск сразу на английском.
3. Регресс: система zh → китайский; выбор языка в настройках — в приоритете над дефолтом.

## Статус
Фикс НЕ применён: на момент попытки сломался слой инструментов агента (Cannot find module './lazy/...', Bash/Edit/Read недоступны). Применить при восстановлении.
