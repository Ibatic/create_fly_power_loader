# Create: Power Loader (Fabric / Create Fly port)

A from-scratch Fabric port of [hlysine/create_power_loader](https://github.com/hlysine/create_power_loader), rebuilt against [Create Fly](https://github.com/ZurrTum/Create-Fly) for Minecraft 26.2.

Adds mechanical chunk loaders for Create Fly: an Andesite tier (single chunk, static or contraption-mounted) and a Brass tier (configurable radius via scroll wheel, works while attached to train or train stations). Both loaders are powered by capturing a Ghast inside them.

Ported to Fabric by Ibatic. All credit for the original design and content goes to [hlysine](https://github.com/hlysine).

## Building

Requires a local copy of the `create-fly` jar placed in `libs/` (not included in this repo). See `build.gradle` for the expected dependency name.

```
./gradlew build
```

## License

MIT — see [LICENSE](LICENSE). Original copyright hlysine (2025); Fabric port copyright Ibatic (2026).
