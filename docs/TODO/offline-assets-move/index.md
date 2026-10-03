---
title: Heavy assets out of the APK
status: planned
document_type: implementation-plan
fork_repository: "https://github.com/lagovi/Operit"
last_reviewed: 2026-10-03
---

# Тяжёлые assets — из APK наружу

[Что и сколько](./1_Inventory.md) · [Очередь работ](./2_WorkQueue.md) ·
[Механика переезда](./3_Mechanics.md)

## Цель

Убрать ~155 МБ assets из `base.apk` (внутренняя память) и 92 МБ их копий в
`files/` на чистой установке. Цель — не размер скачивания, а занятое место
на внутренней памяти телефона.

## Почему не download-on-demand сразу

Единого хостинга payload у форка нет (GitHub Releases hosting не создан), а
upstream Google Drive форк менять не может. Поэтому первая очередь — переезд
на SD-карту при первом запуске (механика мест уже есть: `AppDataLocation`),
вторая — вынос в скачивание, когда появится хостинг.
