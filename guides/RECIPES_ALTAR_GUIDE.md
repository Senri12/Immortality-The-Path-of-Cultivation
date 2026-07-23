# ⛩️ Jade Infusion Altar & Recipes Guide

The **Jade Infusion Altar** is the central crafting block for high-tier items, protective formation runes, cultivation pills, and applying spiritual modifiers.

---

## 🏛️ 1. Ritual Setup

The ritual requires:
1. A **Jade Infusion Altar** in the center.
2. 1 to 5 **Jade Pedestals** placed 2–4 blocks around the altar.
3. A cultivator of the required stage to start the ritual (feeding Qi via right-click on the altar).

---

## 📜 2. Altar Recipe Catalog

| # | Result / Name | Central Item | Pedestal Ingredients | Min. Stage | Qi Cost |
| :-: | :--- | :--- | :--- | :--- | :-: |
| **1** | **`Spirit Stone`** | `Deepslate` | `Spirit Grass` + `Immortals Jade` | `QI_GATHERING` | 100 Qi |
| **2** | **`Foundation Pill`** | `Spirit Stone` | `Spirit Grass` + `Earth Beast Core` + `Immortals Jade` | `QI_GATHERING` | 200 Qi |
| **3** | **`Formation Compass`** | `Compass` | `Immortals Jade` + `Spirit Stone` + `Amethyst Shard` | `QI_GATHERING` | 150 Qi |
| **4** | **`Research Study Board`** | `Crafting Table` | `Immortals Jade` + `Paper` + `Ink Sac` | `MORTAL` | 50 Qi |
| **5** | **`Bamboo Flag`** | `Bamboo` | `White Wool` + `Stick` | `MORTAL` | 50 Qi |
| **6** | **`Jade Flag`** | `Bamboo Flag` | `Immortals Jade` x2 + `Spirit Stone` | `QI_GATHERING` | 250 Qi |
| **7** | **`Formation Core`** | `Jade Block` | `Immortals Jade` x4 + `Diamond` | `FOUNDATION_ESTABLISHMENT` | 400 Qi |
| **8** | **`Spirit Convergence Rune`** | `Stone` | `Spirit Beast Core` | `QI_GATHERING` | 100 Qi |
| **9** | **`Taiji Shield Rune`** | `Stone` | `Earth Beast Core` + `Shield` | `QI_GATHERING` | 150 Qi |
| **10** | **`Mirage Concealment Rune`** | `Stone` | `Spirit Beast Core` + `Fermented Spider Eye` | `QI_GATHERING` | 150 Qi |
| **11** | **`Sword Forest Rune`** | `Stone` | `Flame Beast Core` + `Iron Sword` | `FOUNDATION_ESTABLISHMENT` | 200 Qi |
| **12** | **`Heavenly Lightning Rune`** | `Stone` | `Lightning Beast Core` + `Spirit Stone` | `FOUNDATION_ESTABLISHMENT` | 250 Qi |
| **13** | **`Frost Domain Rune`** | `Stone` | `Frost Beast Core` + `Ice` | `FOUNDATION_ESTABLISHMENT` | 250 Qi |
| **14** | **`Life Spring Rune`** | `Stone` | `Spirit Grass` + `Spirit Beast Core` | `QI_GATHERING` | 120 Qi |
| **15** | **`Gravity Suppression Rune`** | `Stone` | `Earth Beast Core` + `Dragon Vein Stone` | `CORE_FORMATION` | 350 Qi |
| **16** | **`Flame Lotus Rune`** | `Stone` | `Flame Beast Core` + `Phoenix Feather` | `CORE_FORMATION` | 350 Qi |
| **17** | **`Qi Sealing Rune`** | `Stone` | `Immortals Jade` + `Spirit Beast Core` + `Dragon Vein Stone` | `CORE_FORMATION` | 400 Qi |
| **18** | **Electrum Lightning Infusion** (`electrum` III) | **Any weapon/armor\*** | `Lightning Beast Core` + `Heavenly Iron` | `CORE_FORMATION` | 300 Qi |
| **19** | **Unyielding Fortitude Infusion** (`unyielding` III) | **Any armor/shield\*** | `Dragon Vein Stone` + `Heavenly Iron` + `Immortals Jade` | `NASCENT_SOUL` | 450 Qi |
| **20** | **`Lightning Talisman`** | `Paper` | `Lightning Beast Core` + `Spirit Stone` x2 | `QI_GATHERING` | 100 Qi |
| **21** | **`Golden Core Pill`** | `Foundation Pill` | `Spirit Grass` + `Flame Beast Core` + `Immortals Jade` | `FOUNDATION_ESTABLISHMENT` | 250 Qi |
| **22** | **Sharp Qi-Edge Infusion** (`sharp` III) | **Any weapon\*** | `Frost Beast Core` + `Heavenly Iron` | `CORE_FORMATION` | 300 Qi |
| **23** | **Swift Wind Infusion** (`swift` II) | **Any equipment\*** | `Spirit Grass` + `Phoenix Feather` | `FOUNDATION_ESTABLISHMENT` | 220 Qi |

\* The central item is not bound to a specific `Item` (`centralItem = null`) — the recipe fires on **any** item that passes the "equipment" predicate (`stack.isDamageableItem() || stack.getMaxStackSize() == 1`), i.e. any weapon, tool, armor piece, or shield. The example items shown (Diamond Sword, Diamond Chestplate, etc.) are illustrations only, not restrictions.

---

## 🌌 3. Universal Infusion Recipes (Any Weapon/Armor)

In addition to the item-specific recipes above, the code (`JadeInfusionAltarBlockEntity`, static `RECIPES` list) implements 7 recipes with an "open" central item — they accept **any** item satisfying the `isEquipment` predicate (a damageable item, or an item with maxStackSize = 1: weapons, tools, armor, shields, etc.), and add/replace a modifier in the result's `SpiritualBlueprintComponent`:

| Recipe | Modifier | Central Item Condition | Pedestal Ingredients | Min. Stage | Qi Cost |
| :--- | :--- | :--- | :--- | :-: | :-: |
| **Universal Tempered Equipment** | `tempered` (flag) | Any equipment | `Spirit Stone` x4 | `QI_GATHERING` | 100 Qi |
| **Ignis Flame Infusion** | `ignis` II | Any equipment | `Flame Beast Core` + `Immortals Jade` | `FOUNDATION_ESTABLISHMENT` | 200 Qi |
| **Vigor Life Infusion** | `vigor` III | Any equipment | `Earth Beast Core` + `Immortals Jade` x2 | `FOUNDATION_ESTABLISHMENT` | 300 Qi |
| **Electrum Lightning Infusion** | `electrum` III | Any equipment | `Lightning Beast Core` + `Heavenly Iron` | `CORE_FORMATION` | 300 Qi |
| **Unyielding Fortitude Infusion** | `unyielding` III | Any equipment | `Dragon Vein Stone` + `Heavenly Iron` + `Immortals Jade` | `NASCENT_SOUL` | 450 Qi |
| **Sharp Qi-Edge Infusion** | `sharp` III | Any weapon | `Frost Beast Core` + `Heavenly Iron` | `CORE_FORMATION` | 300 Qi |
| **Swift Wind Infusion** | `swift` II | Any equipment | `Spirit Grass` + `Phoenix Feather` | `FOUNDATION_ESTABLISHMENT` | 220 Qi |
