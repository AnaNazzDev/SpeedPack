![SpeedPack](BANNER_URL)

**Resource packs load faster, everywhere.** SpeedPack is a client-side Fabric mod that cuts the time Minecraft spends reloading resources:

- 🌐 **Joining a server** that sends its own resource pack
- 🎒 **Enabling or switching packs** in the Resource Packs menu, in singleplayer or multiplayer
- 🔄 **`F3 + T`** reloads while making or testing a pack
- 🚀 **Game startup**, with your favourite packs already enabled

Server packs and the packs you pick yourself go through the exact same resource reload, so every optimization below applies to all of them.

> **Minecraft 1.21 → 26.3** · Fabric · client-side · [report an issue](https://github.com/AnaNazzDev/SpeedPack/issues)

![SpeedPack vs vanilla](https://i.imgur.com/szizSbr.gif)

## Results

![Results](https://i.imgur.com/jU63k4q.png)

Resource reload times measured when joining a real server (24,000-file / 25 MB server resource pack, 20 CPU threads):

| | Vanilla | SpeedPack |
|---|---|---|
| Joining the server (pack applied) | 27.9 s | **2.4 s** |
| Leaving the server (pack removed) | 18.9 s | **0.4 s** |
| Loading screen kept after loading ends | ~2.0 s | **~0.3 s** |

The bigger the pack, the bigger the gain: packs with thousands of files, custom fonts, items, GUIs or core shaders (ItemsAdder / Oraxen / Nexo style packs, big texture packs, UI overhauls) benefit the most, whether a server sends them or you enable them yourself. With a small or no resource pack, vanilla is already fast, so the main thing you will notice is the shorter loading screen.

## What it does

![Features](https://i.imgur.com/oOqzdqP.png)

- **Zip index:** vanilla scans every file of a zipped pack for each lookup. SpeedPack indexes the pack once.
- **Lazy font tables:** big packs ship thousands of custom fonts. Vanilla rebuilds a glyph table for every one of them on the render thread at each reload (this alone froze the game for ~18 s on the test server). SpeedPack builds a font's table the first time it is actually used.
- **Font warm-up once:** font providers shared by many fonts are pre-warmed once instead of once per font.
- **Shader cache:** linked shader programs are cached on disk, so packs with custom shaders do not have to wait for the graphics driver to recompile them every time.
- **Shorter loading screen:** vanilla keeps the Mojang screen (and blocks input) for about 2 s after loading has finished, and at least 1 s for in-game reloads. SpeedPack keeps a short 0.3 s fade. This applies to every reload: startup, pack menu, `F3 + T`, servers.

Nothing about how packs look is changed.

## FAQ

<details>
<summary><b>Which Minecraft versions are supported?</b></summary>

**26.1, 26.2 and 26.3** are fully tested. **1.21 to 1.21.11** are supported and built from the same code, but tested less: it should work, and if anything goes wrong please [open an issue](https://github.com/AnaNazzDev/SpeedPack/issues) so it can be fixed quickly.

</details>

<details>
<summary><b>Why is the first load slower than the next ones?</b></summary>

That is normal: the first time, SpeedPack **builds its shader cache**. Shaders are compiled once and saved to the `speedpack/shader-cache` folder.

**From the second launch on, it is fast:** shaders are loaded straight from the cache instead of being compiled again.

The cache is rebuilt once (and only once) when something really changes:
- a resource pack with different shaders,
- a graphics driver update,
- a new Minecraft version.

The other optimizations (zip index, fonts, loading screen) work from the very first load. When you join a server, the first join also includes downloading its resource pack, which depends on your connection and the server, not on SpeedPack.

</details>

<details>
<summary><b>Do I need it on the server?</b></summary>

No. It is client-side only. Install it on your game and it works on any server and in singleplayer.

</details>

<details>
<summary><b>Does it work with Sodium?</b></summary>

Yes, it was tested with Sodium.

</details>

<details>
<summary><b>Does it work with Iris / shaderpacks?</b></summary>

Yes. If you ever see a shader problem, set `shaderCache=false` in the config and please report it.

</details>

<details>
<summary><b>Forge / NeoForge?</b></summary>

No, Fabric only for now.

</details>

<details>
<summary><b>Will it change how my packs look?</b></summary>

No. It only changes *when* work is done during loading, not what is loaded.

</details>

<details>
<summary><b>Something looks wrong. What do I do?</b></summary>

Turn off the matching option in `config/speedpack.properties` (fonts → `lazyFonts`, shaders → `shaderCache`, etc.) to get going again, then [open an issue on GitHub](https://github.com/AnaNazzDev/SpeedPack/issues/new/choose) so it gets fixed for everyone. See *Found a bug?* below for what to include.

</details>

<details>
<summary><b>Is it safe to remove?</b></summary>

Yes. It saves nothing into your worlds. You can delete the `speedpack/` folder (shader cache) at any time.

</details>

## Configuration

`config/speedpack.properties` (created on first launch). Every optimization can be turned off on its own if it conflicts with another mod:

```
zipIndex=true
lazyFonts=true
fontWarmOnce=true
shaderCache=true
fastLoadingScreen=true
diagnostics=false
```

Restart the game after editing. `diagnostics=true` writes reload profiles to the `speedpack/` folder.

## Compatibility

| Minecraft | Status |
|---|---|
| **26.1 · 26.2 · 26.3** | ✅ Tested |
| **1.21 → 1.21.11** | ⚠️ Supported, less tested: issues can happen, please report them |

- Fabric Loader + Fabric API required.
- Client-side only: works in singleplayer and on any server.
- Works alongside Sodium and Iris.

## Found a bug? 🐞

Please report it on **[GitHub Issues](https://github.com/AnaNazzDev/SpeedPack/issues/new/choose)**, not in the Modrinth comments: issues are tracked there and fixed faster.

To get it fixed quickly, include:
1. Your **Minecraft**, **SpeedPack** and **Fabric Loader** versions.
2. Your **mod list** (or modpack name).
3. **`logs/latest.log`**, and the crash report if the game crashed.
4. Which **config option** makes the problem go away when set to `false`, if you found one.
5. For a slow load: set `diagnostics=true`, reproduce it once, and attach the zipped **`speedpack/`** folder from your game directory.

Ready-made forms (bug, slow loading, feature idea) are waiting on the issues page.
