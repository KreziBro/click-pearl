# Click Pearl

A lightweight **Fabric** client-side mod for Minecraft that lets you throw an Ender Pearl with a single keybind - without manually switching your hotbar slot.

## Features

- **Instant pearl throw** — press the keybind and the pearl flies without changing your selected item
- **No animation glitch** — the hotbar swap happens and reverts in the same tick, so your hand never visually switches
- **Anti-cheat safe** — sends proper server packets (`ServerboundSetCarriedItemPacket` + `ServerboundUseItemPacket`) in the correct order, mimicking a fast but legitimate hotbar switch
- **Configurable keybind** — rebindable in Minecraft's standard Controls settings menu (default: **Middle Mouse Button / СКМ**)
- **Conflict resolution** — automatically suppresses conflicting actions (e.g. vanilla Pick Block) that share the same key
- **Offhand support** — if pearls are in the offhand, they are thrown directly without any slot swap
- **Cooldown-aware** — does nothing if Ender Pearls are on cooldown
- **Multi-version** — works on Minecraft **1.16.5 through 1.21.4**

## Requirements

- [Fabric Loader](https://fabricmc.net/use/installer/) `>= 0.16.0`
- [Fabric API](https://modrinth.com/mod/fabric-api) (any version matching your MC version)
- Java `>= 17`

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for your Minecraft version.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for your Minecraft version and put it in your `mods` folder.
3. Put `clickpearl-1.0.0.jar` in your `mods` folder.
4. Launch the game, go to **Options → Controls → Click Pearl** and configure the keybind.

## Building from source

```bash
./gradlew build
```

The compiled jar will be in `build/libs/`.

## License

MIT
