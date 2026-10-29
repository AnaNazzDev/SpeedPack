# SpeedPack

Client-side Fabric mod (Minecraft 1.21 to 26.3) that makes resource packs load faster: joining servers with big server resource packs, enabling or switching packs, `F3 + T`, game startup and the Mojang loading screen.

Measured when joining a server with a 24,000-file / 25 MB resource pack:

| | Vanilla | SpeedPack |
|---|---|---|
| Joining the server | 27.9 s | 2.4 s |
| Leaving the server | 18.9 s | 0.4 s |
| Loading screen after loading ends | ~2.0 s | ~0.3 s |

## How

- **Zip index** (`FilePackResourcesMixin`): zipped packs are indexed once instead of scanning every entry on each lookup.
- **Lazy font tables** (`FontSetMixin`): a font's glyph table is built on first use instead of for every font at each reload.
- **Font warm-up once** (`FontManagerMixin`): shared font providers are pre-warmed once instead of once per font.
- **Shader cache** (`GlProgramMixin`, `ProgramBinaryCache`): linked GL programs are cached on disk with `glProgramBinary` (Minecraft 1.21.5 and later).
- **Shorter loading screen** (`LoadingOverlayMixin`): 0.3 s fade instead of ~2 s after loading ends.

Each feature can be turned off in `config/speedpack.properties`.

## Diagnostics

With `diagnostics=true`, every resource reload is profiled and written to `<gameDir>/speedpack/`:
- `reload-listeners.tsv`: time of every reload listener (prepare vs apply), one block per reload.
- `reload-N.top.txt` / `reload-N.folded`: stack samples of each reload (folded stacks open in speedscope / flamegraph.pl).
- `prelaunch.top.txt`: stack samples of Minecraft bootstrap and mod init.
- `timeline.tsv`: startup and reload timestamps, including shader cache hits/misses.
- `system-report.json`, `downloads-inventory.json`: hardware, JVM flags, mod list and pack inventories.

Please attach that folder and `logs/latest.log` when reporting a slow reload.

## Building (Stonecutter multi-version)

One jar per Minecraft version, from the same sources:

```
./gradlew buildAndCollect
```

Jars end up in `build/libs/<mod version>/`.

- `stonecutter.properties.toml`: per-version Fabric API, `fabric.mod.json` range (`mod.mc_compat`) and Modrinth game versions (`mod.mc_releases`).
- Version-specific code uses Stonecutter comments (`//? if >=26.3 {` ... `//?} else {` ... `//?}`). The sources are kept in the 26.3 state (`vcsVersion`).
- `./gradlew :26.2:runClient` runs one version; `run/` is shared.
- Adding a version: add it to `versions(...)` in `settings.gradle.kts` and a block in `stonecutter.properties.toml`.
