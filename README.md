# EnderStorage

**Chests, tanks and pouches linked by colour.** Every ender chest, ender tank and ender pouch has three colour
buttons. Everything with the same three colours shares the same inventory or tank, anywhere in the world, even
across dimensions.

For **Minecraft 26.1.2 on NeoForge**, one jar without dependencies.

## Blocks and items

- **Ender Chest**: a shared inventory (27 slots, upgradable to 108). Hoppers and pipes can use it. Bigger
  inventories get a scroll bar.
- **Ender Tank**: a shared fluid tank (16 buckets, upgradable to 256). Right click with a bucket to fill or empty
  it, or with an empty hand to see what is in it. Its valve opens with a redstone signal and then pushes the fluid
  into the blocks around it; click the valve to turn this around (open without a signal).
- **Ender Pouch**: opens the inventory of its colours from anywhere. Sneak + right click an ender chest to give the
  pouch the colours of that chest.

## Colours, personal and team storage

- Right click one of the three buttons with a **dye** to change that colour.
- Right click the lock of a chest or the valve of a tank with a **diamond** to make it **personal**: it gets its own
  storage that only you can open.
- Right click the lock or valve with an **emerald** to make it a **team** chest or tank: its own storage that
  everyone in your team can use. The team is your FTB Teams party when FTB Teams is installed, otherwise your
  vanilla `/team`.
- Sneak + right click the lock or valve with an empty hand to get the diamond or emerald back.
- Recolour items in the crafting grid: put the chest, tank or pouch in the grid with up to three dyes in the row
  above it (left, middle, right).
- Breaking a chest or tank keeps its colours on the item; the contents stay in the shared storage.
- A comparator next to a chest or tank shows how full it is.

## Upgrades

Right click a chest or tank (not the buttons) with:

| Item | Chest | Tank |
| --- | --- | --- |
| Ender pearl | +3 slots | +4 buckets |
| Eye of ender | +9 slots | +16 buckets |
| Piston | | +1 pump upgrade (up to 4) |

Capacity upgrades belong to the colours (and owner or team), so every chest or tank with those colours gets the
bigger storage. Pump upgrades belong to the tank block and stay on it when you break it: an open valve pushes
250 mB per tick, plus 1000 mB per pump upgrade.

## Recipes

| Item | Recipe |
| --- | --- |
| Ender Chest | blaze rod, wool, blaze rod / obsidian, chest, obsidian / blaze rod, ender pearl, blaze rod |
| Ender Tank | blaze rod, wool, blaze rod / obsidian, cauldron, obsidian / blaze rod, ender pearl, blaze rod |
| Ender Pouch | blaze powder, leather, blaze powder / leather, ender pearl, leather / blaze powder, wool, blaze powder |

The colour of the wool sets all three colours of the result.

## Configuration

`<world>/serverconfig/enderstorage-server.toml` (or in game through the mod list):

| Setting | Default | What it does |
| --- | --- | --- |
| `access.personalItem` | `minecraft:diamond` | The item that makes a chest or tank personal |
| `access.teamItem` | `minecraft:emerald` | The item that makes a chest or tank a team chest or tank |
| `access.anarchyMode` | false | Anyone can open personal and team storage, and broken blocks lose their lock |
| `access.useVanillaEnderChestSounds` | false | Use the vanilla ender chest sounds |
| `upgrades.smallUpgradeItem` | `#c:ender_pearls` | Item for a small capacity upgrade |
| `upgrades.largeUpgradeItem` | `minecraft:ender_eye` | Item for a large capacity upgrade |
| `upgrades.pumpItem` | `minecraft:piston` | Item for a pump upgrade |
| `chest.startSlots` / `chest.maxSlots` | 27 / 108 | Chest slots before upgrades and the most it can get |
| `chest.smallUpgradeSlots` / `chest.largeUpgradeSlots` | 3 / 9 | Slots per upgrade |
| `tank.tankBuckets` / `tank.maxBuckets` | 16 / 256 | Tank buckets before upgrades and the most it can get |
| `tank.smallUpgradeBuckets` / `tank.largeUpgradeBuckets` | 4 / 16 | Buckets per upgrade |
| `tank.pumpBaseRate` / `tank.pumpUpgradeRate` | 250 / 1000 | mB per tick of an open valve, and added per pump upgrade |
| `tank.pumpMaxUpgrades` | 4 | Pump upgrades per tank |

Item settings take an item id or `#namespace:tag`.

## License

[Don't Be an Asshole License](LICENSE.md): do whatever you want with it, port it, share it, just don't be an asshole.
