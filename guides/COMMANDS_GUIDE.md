# 📜 Справочник всех команд мода (Commands Guide)

Мод **Immortality: The Path of Cultivation** предоставляет наборы команд для игроков, администраторов, генерации построек и призыва сущностей.

---

## 🧘 1. Основные команды игрока (`/cultivation`)

| Команда | Описание |
| :--- | :--- |
| `/cultivation` или `/cultivation status` | Показать текущий статус: Стадия, запас Ци, чистота, стабильность, тело, трактат и активная техника |
| `/cultivation techniques` | Открыть интерактивный экран управления техниками |
| `/cultivation effects` | Открыть экран распределения фокуса Ци (Голова, Руки, Торс, Ноги) |
| `/cultivation breakthrough` | Начать попытку прорыва на следующую стадию культивации |
| `/cultivation manual <id>` | Экипировать древний трактат по его ID *(например: `wandering_cloud_manual`, `crimson_flame_manual`, `stone_body_manual`, `omniscience_manual`, `nine_dragons_manual`)* |

---

## ⚡ 2. Команды техник и исследований (`/cultivation technique` / `research`)

| Команда | Описание |
| :--- | :--- |
| `/cultivation technique next` | Переключить активную технику на следующую изученную |
| `/cultivation technique clear` | Снять активную технику |
| `/cultivation technique invoke` | Моментально использовать/вызвать активную технику |
| `/cultivation technique <id>` | Установить активную технику по её ID *(например: `wandering_breath`, `crimson_heartbeat`, `stone_marrow`, `omniscient_mirror`, `nascent_avatar`, `karma_sight`)* |
| `/cultivation research next` | Автоматически завершить доступное исследование с наименьшей стоимостью |
| `/cultivation research <id>` | Завершить конкретное исследование по ID *(например: `qi_sense`, `meridian_cycle`, `iron_body_method`, `golden_core_method`)* |

---

## 🛠️ 3. Команды отладки и администратора (`/cultivation debug`)

*(Требуются права оператора / OP)*

### Управление Стадиями и Ци:
- `/cultivation debug stage <STAGE>` — Установить любую стадию культивации:
  - `MORTAL` (Смертный)
  - `QI_GATHERING` (Закаливание Ци)
  - `FOUNDATION_ESTABLISHMENT` (Возведение Основания)
  - `CORE_FORMATION` (Формирование Ядра)
  - `NASCENT_SOUL` (Зарождающаяся Душа)
  - `SPIRIT_SEVERING` (Отсечение Духа)
  - `ASCENDANT` (Вознесение)
  - `ILLUSORY_YIN` (Призрачный Инь)
  - `CORPOREAL_YANG` (Плотский Янь)
  - `NIRVANA_SCRYER` (Видящий Нирваны)
  - `NIRVANA_CLEANSER` (Очищающий Нирвану)
  - `VOID_TRIBULANT` (Испытуемый Пустоты)
- `/cultivation debug next` — Поднять стадию на +1
- `/cultivation debug prev` — Опустить стадию на -1
- `/cultivation debug qi set <amount>` — Задать точное количество Ци
- `/cultivation debug qi add <amount>` — Добавить указанное количество Ци
- `/cultivation debug qi fill` — Полностью заполнить Ци до текущего максимума

### Управление Телом, Трактатами и Разблокировками:
- `/cultivation debug body <id>` — Задать тело (`iron_body`, `spirit_vessel`, `demonic_veins`, `none`)
- `/cultivation debug manual <id>` — Выдать и закрепить трактат
- `/cultivation debug focus <id>` — Переключить фокус Ци (`head`, `hands`, `torso`, `legs`)
- `/cultivation debug technique <id>` — Выдать конкретную технику
- `/cultivation debug technique all` — Разблокировать абсолютно все техники
- `/cultivation debug insight <id>` — Выдать озарение по ID
- `/cultivation debug insight all` — Разблокировать все озарения
- `/cultivation debug research prepare <id>` — Подготовить исследование на доске изучения
- `/cultivation debug research <id>` — Мгновенно изучить исследование
- `/cultivation debug research all` — Разблокировать всё древо исследований

---

## 🏛️ 4. Команды Спавна Структур (`/place feature`)

Позволяют моментально сгенерировать постройку прямо перед собой:

```bash
/place feature immortality:pedestal_shrine
/place feature immortality:ancient_pagoda
/place feature immortality:ruined_dao_shrine
/place feature immortality:spirit_vein_grotto
```

- **`pedestal_shrine`**: Святилище Нефритовых Пьедесталов с центральным Алтарем Насыщения и 4 3D-пьедесталами.
- **`ancient_pagoda`**: 3-этажная Древняя Пагода Озарения с алтарями и сундуками.
- **`ruined_dao_shrine`**: Святилище Древнего Дао с флагами формаций и `Formation Core`.
- **`spirit_vein_grotto`**: Подземный Духовный Грот с духовным озером и залежами камней.

---

## 🐉 5. Команды Призыва Мобoв и Боссов (`/summon`)

```bash
/summon immortality:tribulation_lord
/summon immortality:flame_salamander
/summon immortality:frost_fox
/summon immortality:spirit_beast
```

- **`tribulation_lord`**: Босс Владыка Небесной Кары (300 HP, уязвим **только для оружия с Ци**).
- **`flame_salamander`**: Пламенная Саламандра (70 HP, поджигает, дропает `Flame Beast Core`).
- **`frost_fox`**: Девятихвостая Морозная Лиса (45 HP, замедляет III, дропает `Frost Beast Core`).
- **`spirit_beast`**: Духовный Зверь Ци.

---

## 🌌 6. Команда Телепортации в Измерение

```bash
/execute in immortality:world_of_immortals run tp ~ 100 ~
```
- Телепортирует игрока в измерение культиваторов **World of Immortals**.
