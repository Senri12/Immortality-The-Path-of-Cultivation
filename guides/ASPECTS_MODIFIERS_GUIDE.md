# 🔮 Spiritual Aspects & Modifiers Guide

**Immortality: The Path of Cultivation** lets you infuse spiritual modifiers onto armor, weapons, and tools through the **Jade Infusion Altar**.

---

## 📜 1. List of All Aspects & Modifiers

> ⚡ **Cultivation Scaling**: Power, damage, burn duration, shield strength, and bonus HP for all aspects **automatically scale with the player's current cultivation stage** (`stageMult = 1.0x ... 5.0x`).

| Modifier | Name | Compatible Items | Levels | Main Effect & Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **`unyielding`** | **Unyielding Fortitude** | **Any armor / shield / item** | I - III | • **Passive**: Grants *Resistance*.<br>• **Emergency Shield**: When HP drops below 35%, instantly grants *Absorption*, *Regeneration*, and a lifesaving spiritual barrier.<br>• **Weapon**: Full knockback immunity. |
| **`electrum`** | **Electrum Lightning** | **Any weapon / armor** | I - III | • **Attack**: On hit, calls down **Heavenly Lightning** on the target and deals electrical damage (scales with stage).<br>• **Defense**: Retaliates with a lightning discharge against attackers. |
| **`vigor`** | **Spirit Vein** | **Any armor / item** | I - V | • Increases the player's max health (base +4 HP, multiplied by the cultivator's stage coefficient). |
| **`ignis`** | **Heavenly Flame** | **Any weapon / tool** | I - V | • Sets the target on fire and deals heavy fire damage (duration and damage scale with cultivation level). |
| **`swift`** | **Wind of Swiftness** | **Any armor / footwear** | I - III | • Grants a passive **Speed** and **Haste** effect. |
| **`sharp`** | **Spiritual Edge** | **Any weapon** | I - V | • Deals direct **true damage** that bypasses the target's armor entirely (damage increases with stage). |
| **`tempered`** | **Tempering** | **Any item / tool** | Flag | • Protects the item from breaking (durability never drops below 1). Preserves all enchantments and properties. |

---

## 🛠️ 2. How to Obtain Modifiers

1. **Infusion Rituals at the Jade Infusion Altar**:
   - Place the item in the center of the altar (`Jade Infusion Altar`).
   - Place the required ingredients on the **Jade Pedestals** surrounding the altar.
   - Supply the required amount of **Qi** from a cultivator of the appropriate stage.

> ⚙️ **Important**: The infusion recipes below (`tempered`, `ignis`, `vigor`, `electrum`, `unyielding`, `sharp`, `swift`) are not bound to a specific item — the code uses an "any equipment" predicate (a damageable item, or an item with maxStackSize = 1), so **any** matching weapon/tool/armor/shield can be placed in the altar's center, not just the item shown in the example.

### 🧪 Example Modifier Recipes:
- **`tempered`** (Universal Tempering):
  - *Center*: Any equipment item
  - *Pedestals*: `Spirit Stone` x4
  - *Stage*: `QI_GATHERING` | *Cost*: 100 Qi
- **`ignis` II** (Heavenly Flame):
  - *Center*: Any weapon/tool
  - *Pedestals*: `Flame Beast Core` + `Immortals Jade`
  - *Stage*: `FOUNDATION_ESTABLISHMENT` | *Cost*: 200 Qi
- **`vigor` III** (Spirit Vein):
  - *Center*: Any armor/equipment item
  - *Pedestals*: `Earth Beast Core` + `Immortals Jade` x2
  - *Stage*: `FOUNDATION_ESTABLISHMENT` | *Cost*: 300 Qi
- **`electrum` III** (Electrum Lightning Blade):
  - *Center*: Any weapon/armor (Diamond Sword shown as an example)
  - *Pedestals*: `Lightning Beast Core` + `Heavenly Iron`
  - *Stage*: `CORE_FORMATION` | *Cost*: 300 Qi
- **`unyielding` III** (Dragon Vein Armor):
  - *Center*: Any armor/shield (Diamond Chestplate shown as an example)
  - *Pedestals*: `Dragon Vein Stone` + `Heavenly Iron` + `Immortals Jade`
  - *Stage*: `NASCENT_SOUL` | *Cost*: 450 Qi
- **`sharp` III** (Spiritual Edge):
  - *Center*: Any weapon
  - *Pedestals*: `Frost Beast Core` + `Heavenly Iron`
  - *Stage*: `CORE_FORMATION` | *Cost*: 300 Qi
- **`swift` II** (Wind of Swiftness):
  - *Center*: Any armor/equipment item
  - *Pedestals*: `Spirit Grass` + `Phoenix Feather`
  - *Stage*: `FOUNDATION_ESTABLISHMENT` | *Cost*: 220 Qi

---

## 👁️ 3. Tooltips & UI

All modifiers are automatically shown right in the item's tooltip on hover:
```text
✦ Spiritual Blueprint
  ✧ Tempered (Unbreakable)
  ⚡ Electrum Lightning III (Calls lightning and chain discharge)
  ✦ Unyielding Fortitude III (Resistance and low-HP shield)
```
