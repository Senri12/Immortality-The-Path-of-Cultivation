# 📜 Full Command Reference — Immortality: The Path of Cultivation

> 📚 All detailed guides and documentation have moved to the [`guides/`](guides/README.md) folder.
> Full version of this guide: [guides/COMMANDS_GUIDE.md](guides/COMMANDS_GUIDE.md)

---

## 🧘 1. Core Player Commands (`/cultivation`)

| Command | Description |
| :--- | :--- |
| `/cultivation` or `/cultivation status` | Show current status: Stage, Qi reserve, purity, stability, body, manual, and active technique |
| `/cultivation techniques` | Open the interactive technique management screen |
| `/cultivation effects` | Open the Qi focus allocation screen (Head, Hands, Torso, Legs) |
| `/cultivation breakthrough` | Attempt a breakthrough to the next cultivation stage |
| `/cultivation manual <id>` | Equip an ancient manual by its ID *(e.g. `wandering_cloud_manual`, `crimson_flame_manual`, `stone_body_manual`, `omniscience_manual`, `nine_dragons_manual`)* |

---

## ⚡ 2. Technique & Research Commands (`/cultivation technique` / `research`)

| Command | Description |
| :--- | :--- |
| `/cultivation technique next` | Switch the active technique to the next learned one |
| `/cultivation technique clear` | Unequip the active technique |
| `/cultivation technique invoke` | Instantly use/invoke the active technique |
| `/cultivation technique <id>` | Set the active technique by its ID *(e.g. `wandering_breath`, `crimson_heartbeat`, `stone_marrow`, `omniscient_mirror`, `nascent_avatar`, `karma_sight`)* |
| `/cultivation research next` | Automatically complete the cheapest available research |
| `/cultivation research <id>` | Complete a specific research by ID *(e.g. `qi_sense`, `meridian_cycle`, `iron_body_method`, `golden_core_method`)* |

---

## 🛠️ 3. Debug & Admin Commands (`/cultivation debug`)

*(Operator/OP permissions required)*

### Stage & Qi Management:
- `/cultivation debug stage <STAGE>` — Set any cultivation stage:
  - `MORTAL`
  - `QI_GATHERING`
  - `FOUNDATION_ESTABLISHMENT`
  - `CORE_FORMATION`
  - `NASCENT_SOUL`
  - `SPIRIT_SEVERING`
  - `ASCENDANT`
  - `ILLUSORY_YIN`
  - `CORPOREAL_YANG`
  - `NIRVANA_SCRYER`
  - `NIRVANA_CLEANSER`
  - `VOID_TRIBULANT`
- `/cultivation debug next` — Raise the stage by +1
- `/cultivation debug prev` — Lower the stage by -1
- `/cultivation debug qi set <amount>` — Set an exact Qi amount
- `/cultivation debug qi add <amount>` — Add the specified amount of Qi
- `/cultivation debug qi fill` — Fully fill Qi up to the current max

### Body, Manual & Unlock Management:
- `/cultivation debug body <id>` — Set body constitution (`iron_body`, `spirit_vessel`, `demonic_veins`, `none`)
- `/cultivation debug manual <id>` — Grant and equip a manual
- `/cultivation debug focus <id>` — Switch Qi focus (`head`, `hands`, `torso`, `legs`)
- `/cultivation debug technique <id>` — Grant a specific technique
- `/cultivation debug technique all` — Unlock absolutely all techniques
- `/cultivation debug insight <id>` — Grant an insight by ID
- `/cultivation debug insight all` — Unlock all insights
- `/cultivation debug research prepare <id>` — Prepare a research on the study board
- `/cultivation debug research <id>` — Instantly complete a research
- `/cultivation debug research all` — Unlock the entire research tree

---

## 🏛️ 4. Structure Spawn Commands (`/place feature`)

Instantly generate a structure right in front of you:

```bash
/place feature immortality:ancient_pagoda
/place feature immortality:ruined_dao_shrine
/place feature immortality:spirit_vein_grotto
```

- **`ancient_pagoda`**: A 3-story Ancient Pagoda of Enlightenment with altars and chests.
- **`ruined_dao_shrine`**: A Ruined Dao Shrine with formation flags and a `Formation Core`.
- **`spirit_vein_grotto`**: An underground Spirit Grotto with a spiritual pool and stone deposits.

---

## 🐉 5. Mob & Boss Summoning Commands (`/summon`)

```bash
/summon immortality:tribulation_lord
/summon immortality:flame_salamander
/summon immortality:frost_fox
/summon immortality:spirit_beast
```

- **`tribulation_lord`**: The Tribulation Lord boss (300 HP, vulnerable **only to Qi-infused weapons**).
- **`flame_salamander`**: Flame Salamander (70 HP, sets targets on fire, drops `Flame Beast Core`).
- **`frost_fox`**: Nine-Tailed Frost Fox (45 HP, applies Slowness III, drops `Frost Beast Core`).
- **`spirit_beast`**: Spirit Beast of Qi.

---

## 🌌 6. Dimension Teleport Command

```bash
/execute in immortality:world_of_immortals run tp ~ 100 ~
```
- Teleports the player to the cultivator dimension **World of Immortals**.
