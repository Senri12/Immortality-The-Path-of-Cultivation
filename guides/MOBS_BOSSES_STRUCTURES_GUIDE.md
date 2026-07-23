# 🐉 Mobs, Bosses & Structures Guide

The mod adds elite spirit beasts, world structures, and cultivation bosses.

---

## 🐲 1. Boss: Tribulation Lord (`TribulationLordEntity`)

- **Summon ID**: `/summon immortality:tribulation_lord`
- **Stats**: `300 HP` (150 hearts), `14 damage`, `10 armor`, a dedicated **Boss Bar**.
- **⚡ Qi Barrier (Immunity)**:
  - Any regular vanilla attacks (wooden/iron/diamond swords without Qi, axes, bows, fists) **deal no damage to the boss whatsoever**!
  - Attacking with a regular weapon plays a shield-bounce sound, and **20% of the damage is reflected back** onto the attacker.
  - **How to break it**: The boss is only vulnerable to **Qi-infused items** (tempered weapons with aspect blueprints, mod items, and attacks from players above the Mortal stage with a Qi reserve).
- **Special ability**: Strikes its target with lightning every 6 seconds.
- **Drops**: 2-4 `Dragon Vein Stone`, 2-4 `Heavenly Iron`, `Golden Core Pill`, `Lightning Beast Core`, 15-25 `Spirit Stone`.

---

## 🦎 2. Elite Mobs

### 🐊 Flame Salamander (`FlameSalamanderEntity`)
- **Summon ID**: `/summon immortality:flame_salamander`
- **Stats**: `70 HP`, `10 damage`, full immunity to fire and lava.
- **Abilities**: Sets its victim on fire for 5 seconds on hit.
- **Drops**: 1-3 `Flame Beast Core`, `Spirit Stone`.

### 🦊 Nine-Tailed Frost Fox (`FrostFoxEntity`)
- **Summon ID**: `/summon immortality:frost_fox`
- **Stats**: `45 HP`, `6 damage`, high movement speed (0.35).
- **Abilities**: Applies a strong Slowness III on hit.
- **Drops**: 1-3 `Frost Beast Core`, `Spirit Grass`, `Spirit Stone`.

### 🐺 Spirit Beast (`SpiritBeastEntity`)
- **Summon ID**: `/summon immortality:spirit_beast`
- **Stats**: `50 HP`, `8 damage`.
- **Spawning**: Very rare in the Overworld (weight 5). Common in the cultivator dimension (`World of Immortals`, weight 35).
- **Drops**: `Spirit Beast Core`, `Spirit Stone`.

---

## 🏛️ 3. Structures & Buildings

You can generate structures using the `/place feature` command:

1. **`immortality:ancient_pagoda`** (Ancient Pagoda of Enlightenment):
   - A tall 3-story jade pagoda.
   - Contains an Infusion Altar, pedestals, and chests with randomized loot.
2. **`immortality:ruined_dao_shrine`** (Ruined Dao Shrine):
   - A ruined spiritual sanctuary with formation flags and a `Formation Core`.
3. **`immortality:spirit_vein_grotto`** (Underground Spirit Grotto):
   - An underground cave with a spiritual pool and stone deposits.

---

## 🌌 4. Dimension: World of Immortals (`World of Immortals`)

- **Teleport**:
  ```bash
  /execute in immortality:world_of_immortals run tp ~ 100 ~
  ```
- A dimension with distinctive spiritual biomes, high Qi concentration, and frequent spirit beast spawns.
