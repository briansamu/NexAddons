# Architecture

NexAddons starts as a single Fabric client module.

## Entry Point

`dev.nexaddons.NexAddons` is the Fabric client entrypoint declared in `fabric.mod.json`. It initializes config, commands, and client tick hooks.

## Config

`ConfigManager` stores a small JSON file in the Fabric config directory. Keep config defaults backward compatible when adding fields.

## Commands

`/nexaddons` and `/na` are registered with Fabric API's client command API. Commands should be safe to run anywhere and should not require being on Hypixel unless the command explicitly reads SkyBlock state.

## SkyBlock Context

`SkyBlockContext` is the starter state boundary for Hypixel and SkyBlock detection. Today it only detects Hypixel server addresses. Future SkyBlock detection should be added here before feature modules depend on it.

## Feature Packages

Add features under `dev.nexaddons.feature.<name>`. Prefer this shape:

```kotlin
object ExampleFeature {
    fun register() {
        // Register events, commands, or render hooks here.
    }
}
```

Keep parsing and calculations pure where possible so they can be unit-tested without launching Minecraft.
