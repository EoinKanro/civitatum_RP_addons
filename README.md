# Civitatum RP Addons

Моды Civitatum для Minecraft 1.21.1 / NeoForge 21.1.256. Один проект Gradle, по модулю на мод:

| Модуль | Мод | modId | Версия | jar |
|---|---|---|---|---|
| [inventory](inventory/) | Civitatum Inventory | `civitatum_inventory` | 0.4.1 | `civitatum-inventory-1.21.1-0.4.1.jar` |
| [character-creation](character-creation/) | Civitatum Character Creation | `civitatum` | 0.4.2 | `civitatum-character-creation-1.21.1-0.4.2.jar` |
| [couple-interactions](couple-interactions/) | Civitatum Couple Interactions | `civitatum_couple` | 0.2.0-alpha.1 | `civitatum-couple-interactions-1.21.1-0.2.0-alpha.1.jar` |

Описание, проверки и история каждого мода — в README его модуля.

## Сборка

Нужен JDK 21 (Gradle найдёт установленный или скачает сам).

```
./gradlew build                          # все моды, с проверками; jar в <модуль>/build/libs
./gradlew :inventory:build               # один мод
./gradlew :character-creation:runClient  # запустить игру с одним модом
```

В IntelliJ IDEA открывается этот корневой каталог: все моды оказываются в одном окне.

## Устройство

- `settings.gradle` — список модулей и версия плагина ModDevGradle;
- `gradle.properties` — настройки Gradle и версия NeoForge (`neo_version`) для всех модов;
- `build.gradle` — общее для всех модов: Java 21, кодировка UTF-8, Maven Central, версия NeoForge;
- `<модуль>/build.gradle` — своё у каждого мода: версия, имя jar, запуски, тестовые наборы и необязательные зависимости.

Исходники собраны из веток `codex/civitatum-inventory`, `codex/ccc/development` и `codex/cci/development` (версии 0.4.1, 0.4.2 и 0.2.0-alpha.1); эти ветки — родители коммита объединения, их история доступна из `main`.
