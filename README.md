# EnderStorage

**Chests, tanks and pouches linked by colour.** Every ender chest, ender tank and ender pouch has three colour
buttons. Everything with the same three colours shares the same inventory or tank, anywhere in the world, even
across dimensions.

For **Minecraft 26.1.2 on NeoForge**, one jar without dependencies.

## Blocks and items

- **Ender Chest**: a shared inventory (27 slots by default). Hoppers and pipes can use it.
- **Ender Tank**: a shared fluid tank (16 buckets by default). Right click with a bucket to fill or empty it.
  Its valve opens with a redstone signal and then pushes the fluid into the blocks around it; click the valve to
  turn this around (open without a signal).
- **Ender Pouch**: opens the inventory of its colours from anywhere. Sneak + right click an ender chest to give the
  pouch the colours of that chest.

## Colours and personal storage

- Right click one of the three buttons with a **dye** to change that colour.
- Right click the lock of a chest or the valve of a tank with a **diamond** to make it **personal**: it gets its own
  storage that only you can open. Sneak + right click the lock with an empty hand to get the diamond back.
- Recolour items in the crafting grid: put the chest, tank or pouch in the grid with up to three dyes in the row
  above it (left, middle, right).
- Breaking a chest or tank keeps its colours on the item; the contents stay in the shared storage.
- A comparator next to a chest or tank shows how full it is.

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
| `personalItem` | `minecraft:diamond` | The item that makes a chest or tank personal |
| `anarchyMode` | false | Anyone can open personal storage, and broken personal blocks drop the diamond |
| `storageSize` | 1 | Chest size: 0 = 3x3, 1 = 3x9, 2 = 6x9 |
| `tankBuckets` | 16 | How many buckets a tank holds |
| `useVanillaEnderChestSounds` | false | Use the vanilla ender chest sounds |

## License

[Don't Be an Asshole License](LICENSE.md): do whatever you want with it, port it, share it, just don't be an asshole.
