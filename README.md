<div align="center">

<img src="https://github.com/user-attachments/assets/87babc51-cb6a-486e-81f2-74489cdcdb07" width="120">

# Create Fly: Power Loader (Fabric)

<div align="left">
<p>
  <a href="https://modrinth.com/mod/create-fly-power-loader">
    <img src="https://img.shields.io/badge/Modrinth-20232a?style=for-the-badge&logo=modrinth&logoColor=1BD96A" alt="Modrinth">
  </a>
</p>

A from-scratch Fabric port of [hlysine/create_power_loader](https://github.com/hlysine/create_power_loader), rebuilt against [Create Fly](https://github.com/ZurrTum/Create-Fly) for Minecraft.

Adds mechanical chunk loaders for Create Fly: an Andesite tier (single chunk, static or contraption-mounted) and a Brass tier (configurable radius via scroll wheel, works while attached to trains or train stations). Both loaders are powered by capturing a Ghast inside them.

Ported to Fabric by Ibatic. All credit for the original design and content goes to [hlysine](https://github.com/hlysine).

## Building

Requires a local copy of the `create-fly` jar placed in `libs/` (not included in this repo). See `build.gradle` for the expected dependency name.

```
./gradlew build
```

## License

MIT — see [LICENSE](LICENSE). Original copyright hlysine (2025); Fabric port copyright Ibatic (2026).
