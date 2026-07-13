# 🌀 Immortality: The Path of Cultivation

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-blue.svg?logo=minecraft&color=3F7E3C)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-lightgrey.svg?logo=fabric&color=dbd5cc)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-red.svg?logo=oracle&color=ED8B00)](https://oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

An advanced Eastern-fantasy magic mod for **Minecraft Fabric 1.21.11+**, merging the deep cultivation lore of the Xianxia webnovel **"Renegade Immortal" (Противостояние Святого)** with the interactive, progression-based research mechanics of **"Thaumcraft 4"**.

---

## 📌 Table of Contents
1. [Overview & Lore](#-overview--lore)
2. [Technology Stack](#-technology-stack)
3. [Project Folder Structure](#-project-folder-structure)
4. [In-Depth Game Mechanics](#-in-depth-game-mechanics)
   - [Realms of Cultivation](#realms-of-cultivation)
   - [Body Constitutions](#body-constitutions)
   - [Modular Magic Arrays & Runes](#modular-magic-arrays--runes)
   - [Altar & Study Table Research](#altar--study-table-research)
5. [Crafting Recipes](#-crafting-recipes)
6. [Call for Contributors (Open Source)](#-call-for-contributors-open-source)
7. [Getting Started & Building](#-getting-started--building)
8. [License](#-license)
9. [Author & Credits](#-author--credits)

---

## 🌌 Overview & Lore

Step onto the path of defiance against the heavens. Inspired by **Er Gen's Renegade Immortal**, you start as a fragile mortal in a hostile world. By gathering spiritual energy (**Qi**), tempering your body, studying manuals, and surviving **Heavenly Tribulations (молнии трибуляции)**, you can climb the 12 realms of cultivation to attain absolute immortality.

Progression is non-linear and is governed by a **Thaumcraft 4-inspired** research system. Players must analyze elemental aspects, connect them in a hexagonal grid, and navigate scrollable parchment maps to unlock deep magical secrets.

---

## 💻 Technology Stack

*   **Platform:** Fabric Loader & Fabric API
*   **Minecraft Version Compatibility:** 1.21.11 (configured via Loom 1.16+)
*   **Java Version:** Java 21 (utilizing modern language features and records)
*   **API Integrations:**
    *   **Data Components API:** Zero raw NBT data on items; utilizes strict, type-safe custom Data Components (`ComponentType` with Codecs) for networking and inventory sync.
    *   **Custom Packet Payloads:** Modern Minecraft network protocol (`CustomPacketPayload`) for S2C and C2S communication.
    *   **Server Claim Events:** Integration hooks for server land claim plugins (WorldGuard, FTB Chunks, GriefPrevention) to prevent array griefing.

---

## 📂 Project Folder Structure

Below is an overview of the codebase architecture to help new developers navigate the repository:

```text
immortality-template-1.21.11
├── src/main/java/immortality
│   ├── block/                # Custom block classes (Altars, Portals, Cores, Flags)
│   │   ├── entity/           # Block Entities containing array logic and Qi storage
│   ├── client/               # Client-side initialization, keybindings, and HUD rendering
│   ├── item/                 # Custom items (Formation Compass, Beast Cores, Manuals)
│   │   ├── component/        # Data Components definition (e.g. FormationCompassComponent)
│   ├── loot/                 # Dynamically injects mod items into vanilla structure chests
│   ├── mixin/                # Mixins for gameplay modifications (creative flight, tempered items)
│   ├── network/              # S2C and C2S network packet registries & custom payloads
│   ├── registry/             # Boostrappers for JSON-defined files (Manuals, Aspects, Researches)
│   ├── screen/               # GUI screens & handlers (Altar Map, Focus screen, Aspect Grid)
│   └── Immortality.java      # Main mod entrypoint & registry initializations
│
├── src/main/resources
│   ├── assets/immortality
│   │   ├── lang/             # Translations (en_us.json, ru_ru.json)
│   │   ├── models/           # Custom item and block JSON models
│   │   ├── textures/         # Beautiful fantasy GUI backgrounds, medallion HUD and icons
│   │   └── icon.png          # Project logo (1:1 aspect ratio)
│   │
│   └── data/immortality
│       ├── dimension/        # World of Immortals custom dimension rules
│       ├── recipe/           # 16 standard recipes in Minecraft 1.21+ format (uses "id")
│       └── [registries].json # Core game databases (bodies.json, qi_aspects.json, researches.json)
```

---

## ⚡ In-Depth Game Mechanics

### Realms of Cultivation

The mod features a 12-stage realm progression. Higher realms grant passive **Strength (attack boost)** and **Resistance (damage reduction)**.

| Realm Name | Qi Capacity | Passive Abilities |
| :--- | :---: | :--- |
| **Mortal (Смертный)** | 30 | None |
| **Qi Gathering (Сбор Ци)** | 80 | Passive Qi accumulation |
| **Foundation Establishment (Создание Основы)** | 140 | Ability to start base-level arrays |
| **Core Formation (Формирование Ядра)** | 220 | Inedia (No hunger), Wither/Poison immunities, speed boost |
| **Nascent Soul (Зарождающаяся Душа)** | 320 | Jump height boost, fall damage immunity |
| **Spirit Severing (Отсечение Духа)** | 450 | Water breathing |
| **Ascendant (Вознесение)** | 620 | **Infinite survival-creative flight (0 Qi cost)** |
| **Illusory Yin (Иллюзорный Инь)** | 820 | Flight speed x2.0 |
| **Corporeal Yang (Телесный Ян)** | 1050 | Flight speed x2.5 |
| **Nirvana Scryer (Провидец Нирваны)** | 1320 | Flight speed x3.0 |
| **Nirvana Cleanser (Очиститель Нирваны)** | 1650 | Flight speed x3.5 |
| **Void Tribulant (Испытуемый Пустотой)** | 2050 | Flight speed x4.0, ultimate status |

### Body Constitutions

Every player is born with or reincarnated into a custom spiritual body defined in `bodies.json`:
*   **Iron Body (Железное Тело):** +6% breakthrough rate, +10% Qi stability. Compatible with Earth Cores.
*   **Spirit Vessel (Духовный Сосуд):** +12% breakthrough rate, +4% Qi stability. Compatible with Spirit Cores.
*   **Demonic Veins (Демонические Меридианы):** +18% breakthrough rate, but -12% Qi stability penalty (High Qi Deviation risk!). Compatible with Fire Cores.

### Modular Magic Arrays & Runes

Deploy a **Formation Core** surrounded by **Formation Flags** (Bamboo or Jade).
1.  **Register:** Right-click the flags with a **Formation Compass** to log their coordinates (up to 8 flags).
2.  **Bind:** Right-click the Core block with the compass to bind the flags. (To reset flags, press `Shift + Right-Click` with the compass in the air).
3.  **Run:** Core block entity will consume Qi from the flags to sustain active area modifiers (runes).
4.  **Rune Slots:**
    *   *Spirit Convergence Rune:* Accelerates Qi meditation rates.
    *   *Taiji Shield Rune:* Creates a defensive shield blocking hostiles.
    *   *Mirage Concealment Rune:* Hides blocks/entities within the boundaries.
    *   *Sword Forest Rune:* Attacks hostile targets with spiritual swords.

### Altar & Study Table Research

*   **Enlightenment Altar:** Spend XP and materials to unlock insights. The map GUI features drag-to-scroll controls on an ancient parchment overlay.
*   **Research Study Table:** Merge primal aspects (Water, Fire, Earth, Metal, Order, Chaos) in a hexagonal board matching the required pattern to master new recipes.

---

## 📜 Crafting Recipes

All recipes are 1.21+ compliant (using strings instead of object arrays in ingredients, and the `"id"` output field). Below is a summary:

*   **Meditation Mat:** 3 String + 3 Wheat + 3 Leather.
*   **Enlightenment Altar:** 3 Jade + 2 Stone Bricks + 1 Obsidian + 3 Stone Bricks.
*   **Research Study Table:** 1 Feather + 1 Ink Sac + 3 Planks + 1 Bookshelf.
*   **Jade Block:** 9 Immortals Jade.
*   **Formation Compass:** 4 Immortals Jade surrounding 1 Compass.
*   **Formation Core:** 1 Diamond + 2 Redstone + 6 Jade Blocks.
*   **Bamboo Flag:** 3 Lime Wool + 2 Bamboo.
*   **Jade Flag:** 3 Green Wool + 1 Jade Block + 1 Stick.
*   **Spirit Stone:** 4 Immortals Jade + 5 Stone blocks.

---

## 🤝 Call for Contributors (Open Source)

> **RU:** Я бы очень хотел увидеть активное развитие этого мода! К сожалению, я не профессиональный программист или дизайнер, и у меня мало свободного времени. Если вы умеете писать код на Java, создавать 3D-модели, рисовать текстуры или придумывать новые механики — я буду бесконечно благодарен за любую помощь и пул-реквесты! Давайте развивать этот проект вместе!
> 
> **EN:** This project is fully open-source and welcoming to developers, artists, sound designers, and writers. Whether you want to fix bugs, optimize core calculations, add new arrays, or model spiritual beasts, your contributions are highly valued. Feel free to fork the repository and open a pull request!

---

## 🛠️ Getting Started & Building

To run or build the mod locally, make sure you have Java 21 installed.

1.  Clone the repository:
    ```bash
    git clone https://github.com/Senri12/Immortality-The-Path-of-Cultivation.git
    cd Immortality-The-Path-of-Cultivation
    ```
2.  Run the client locally:
    ```bash
    ./gradlew runClient
    ```
3.  Run automated client game tests:
    ```bash
    ./gradlew runClientGameTest
    ```
4.  Build the production JAR:
    ```bash
    ./gradlew build
    ```
    The compiled `.jar` file will be located in `build/libs/`.

---

## 📄 License

This project is licensed under the **MIT License**. You are free to modify, distribute, and integrate this mod into your modpacks, repositories, or derivatives, provided that proper credit is given to the original repository.

---

## 👥 Author & Credits

*   **Mod Author:** [Senri12](https://github.com/Senri12)
*   **Lore Inspiration:** "Renegade Immortal" (Xian Ni) by Er Gen.
*   **UI & Design Concept:** "Thaumcraft 4" by Azanor.
