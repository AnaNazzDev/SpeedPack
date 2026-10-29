![SpeedPack](BANNER_URL)

**Resource packs load faster, everywhere.** SpeedPack is a client-side Fabric mod that cuts the time Minecraft spends reloading resources:

- 🌐 **Joining a server** that sends its own resource pack
- 🎒 **Enabling or switching packs** in the Resource Packs menu, in singleplayer or multiplayer
- 🔄 **`F3 + T`** reloads while making or testing a pack
- 🚀 **Game startup**, with your favourite packs already enabled

Server packs and the packs you pick yourself go through the exact same resource reload, so every optimization below applies to all of them.

![SpeedPack vs vanilla](GIF_URL)

## Results

![Results](RESULTS_URL)

Resource reload times measured when joining a real server (24,000-file / 25 MB server resource pack, GTX 1080, 20 CPU threads):

| | Vanilla | SpeedPack |
|---|---|---|
| Joining the server (pack applied) | 27.9 s | **2.4 s** |
| Leaving the server (pack removed) | 18.9 s | **0.4 s** |
| Loading screen kept after loading ends | ~2.0 s | **~0.3 s** |

The bigger the pack, the bigger the gain: packs with thousands of files, custom fonts, items, GUIs or core shaders (ItemsAdder / Oraxen / Nexo style packs, big texture packs, UI overhauls) benefit the most, whether a server sends them or you enable them yourself. With a small or no resource pack, vanilla is already fast, so the main thing you will notice is the shorter loading screen.

## What it does

![Features](FEATURES_URL)

- **Zip index:** vanilla scans every file of a zipped pack for each lookup. SpeedPack indexes the pack once.
- **Lazy font tables:** big packs ship thousands of custom fonts. Vanilla rebuilds a glyph table for every one of them on the render thread at each reload (this alone froze the game for ~18 s on the test server). SpeedPack builds a font's table the first time it is actually used.
- **Font warm-up once:** font providers shared by many fonts are pre-warmed once instead of once per font.
- **Shader cache:** linked shader programs are cached on disk, so packs with custom shaders do not have to wait for the graphics driver to recompile them every time.
- **Shorter loading screen:** vanilla keeps the Mojang screen (and blocks input) for about 2 s after loading has finished, and at least 1 s for in-game reloads. SpeedPack keeps a short 0.3 s fade. This applies to every reload: startup, pack menu, `F3 + T`, servers.

Nothing about how packs look is changed.

## FAQ

**Do I need it on the server?**
No. It is client-side only. Install it on your game and it works on any server and in singleplayer.

**Does it work with Sodium?**
Yes, it was tested with Sodium and ImmediatelyFast.

**Does it work with Iris / shaderpacks?**
Yes. If you ever see a shader problem, set `shaderCache=false` in the config and please report it.

**Forge / NeoForge?**
No, Fabric only for now.

**Will it change how my packs look?**
No. It only changes *when* work is done during loading, not what is loaded.

**Something looks wrong. What do I do?**
Turn off the matching option in `config/speedpack.properties` (fonts → `lazyFonts`, shaders → `shaderCache`, etc.), then open an issue with `diagnostics=true` and the `speedpack/` folder attached.

**Is it safe to remove?**
Yes. It saves nothing into your worlds. You can delete the `speedpack/` folder (shader cache) at any time.

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

- Fabric, Minecraft 26.1 to 26.3, Fabric API required.
- Client-side only: works in singleplayer and on any server.
