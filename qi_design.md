# Immortality Mod -- Qi System Design Notes

## Core Principle

Do not rely on simple mob drops. Qi should be obtained through
interaction with the world, player behavior, and conditions.

------------------------------------------------------------------------

## 1. Environmental Qi Gathering

Qi is generated based on surroundings:

-   Water Qi: rain, rivers, fishing
-   Fire Qi: lava, Nether, burning state
-   Wood Qi: forests, tree growth
-   Earth Qi: underground, stone
-   Metal Qi: ores, caves

------------------------------------------------------------------------

## 2. Yin / Yang System

Qi polarity depends on conditions:

### Yin

-   Night
-   Cold environments
-   Underground
-   Low HP
-   Isolation

### Yang

-   Day
-   Sunlight
-   Combat
-   High HP
-   Nearby entities

------------------------------------------------------------------------

## 3. Domains

-   Qi: universal base energy
-   Body: physical endurance, survival
-   Soul: death, mobs, darkness
-   Spirit: meditation, rare events
-   Void: instability, failures

------------------------------------------------------------------------

## 4. Contextual Mob Drops

Drops depend on conditions:

-   Cow → Body Yin
-   Zombie → Soul Yin
-   Skeleton → Metal Yin
-   Creeper → Fire Yang

Modifiers: - Night → more Yin - Day → more Yang - Explosions → Void
chance

------------------------------------------------------------------------

## 5. Trials (Key Mechanic)

Examples:

-   Fire Trial: survive in lava
-   Water Trial: hold breath underwater
-   Yin Trial: survive a night without light
-   Yang Trial: kill mobs during the day

------------------------------------------------------------------------

## 6. Meditation System

Player can meditate to gain Qi: - Affected by environment - Affected by
time - Interruptible

------------------------------------------------------------------------

## 7. Random Events

-   Lightning → rare Yang Qi
-   Blood Moon → Soul Qi
-   Storm → Water Qi
-   Eclipse → Yin/Yang balance

------------------------------------------------------------------------

## 8. Deviation System

-   Wrong conditions → impure Qi
-   Correct conditions → pure Qi
-   Impure Qi increases deviation

------------------------------------------------------------------------

## 9. Anti-Grind Mechanic

-   Diminishing returns on repeated Qi farming
-   Encourages varied gameplay

------------------------------------------------------------------------

## 10. Example Gameplay Loop

1.  Gather Fire Qi in Nether
2.  Gather Metal Qi via mining
3.  Meditate at night for Spirit Qi
4.  Solve Qi puzzle
5.  Achieve breakthrough

------------------------------------------------------------------------

## Design Goal

Create a system based on: - Interaction with the world - Philosophical
balance - Player behavior - Risk vs reward

Avoid: - Pure grinding - Simple drops - Linear progression
