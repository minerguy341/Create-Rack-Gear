# Create: Rack Gear

A [Create](https://github.com/Creators-of-Create/Create) addon for NeoForge, adding the **Rack
Gear** — a cogwheel that meshes with a linear rack to turn rotation into linear motion.

> Status: the mechanic works and the project is set up for further content. Rendering polish (the
> pinion reuses Create's large cogwheel model) and Ponder scenes are still open.

## Targets

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.249 (accepts `[21.1.219,)`) |
| Create | 6.0.11-300 (accepts `[6.0,7)`) |
| Registrate | MC1.21-1.3.0+67 |
| Mappings | Mojang official + Parchment 2024.11.17 |
| Java | 21 |

Ponder, Catnip and Flywheel come in transitively through Create. All versions live in
`gradle.properties`.

## Building

```bash
./gradlew build          # jar lands in build/libs/, runs the unit tests
./gradlew test           # meshing geometry and speed conversion tests
./gradlew runClient      # dev client with Create installed
./gradlew runServer      # dev server
./gradlew runData        # regenerate src/generated/resources
```

The first invocation downloads NeoForge and decompiles Minecraft, which takes several minutes and a
few GB of disk. Subsequent builds are fast.

For IDE setup, import the project as a Gradle project; ModDevGradle generates the run
configurations. In IntelliJ, run `./gradlew build` once first so the sources are attached.

## The rack and pinion

Two blocks:

- **Rack** — a toothed bar with an axis, no kinetics of its own. Teeth run along all four sides, so
  a pinion meshes with it from any side.
- **Rack Pinion** — a large cogwheel that is also a kinetic *generator*. It meshes with Create's
  cogwheels exactly like a large cogwheel does (it implements `ICogWheel`), and it produces rotation
  when a rack is dragged past it.

**Meshing.** The pinion looks at the four blocks around its shaft (never the two shaft faces). A
rack in one of those positions meshes when it runs along the axis perpendicular to both the pinion's
shaft and the direction to the rack — a pinion on the Z axis with a rack below it engages a rack
running along X.

**Speed.** The rack's velocity along its own axis is converted at Create's own linear rate: a
contraption moving `n / 512` blocks per tick turns the pinion at `n` RPM. Because Create moves
pistons, pulleys and gantries at `speed / 512` blocks per tick, a piston running at 32 RPM drives a
pinion at 32 RPM. Generated speed is rounded to whole RPM (so small speed jitter doesn't churn the
kinetic network) and capped at Create's default maximum of 256 RPM.

**Direction.** Rotation follows rolling contact: reverse the contraption and the pinion reverses;
move the rack along the pinion's other side and it reverses too. The pinion supports 8 su per RPM it
generates, the same as a water wheel.

### What drives what

The pinion generates rotation for the *world* network, so the rack is the part that moves:

| Rack | Pinion | Result |
| --- | --- | --- |
| on a moving contraption | placed in the world | pinion spins, powers the network it is attached to |
| in the world | in the world | nothing moves, so nothing turns |
| in the world | on a moving contraption | **not implemented** — see below |

A pinion riding a contraption cannot power anything: contraptions have no kinetic network in Create,
so rotation generated there would have nowhere to go. Making the cog *look* like it spins as it
travels along a world rack is possible (a `MovementBehaviour` with an `ActorVisual`), and is the
obvious next addition if the visual is what you are after — but it stays cosmetic.

Any contraption that translates works as the rack's carrier: pistons, pulleys, gantry carriages and
trains. A contraption that only rotates (a bearing) imparts no linear motion at the meshing point
and is ignored.

## Layout

```
src/main/java/com/minerguy341/rackgear/
├── CreateRackGear.java                  @Mod entry point, owns the CreateRegistrate
├── content/
│   ├── RackMeshing.java                 meshing geometry, direction and speed conversion
│   ├── rack/RackBlock.java              the toothed bar
│   └── pinion/
│       ├── RackPinionBlock.java         large cog that Create's propagator meshes with
│       ├── RackPinionBlockEntity.java   finds passing racks, generates the rotation
│       ├── RackPinionRenderer.java      draws the cog spinning
│       └── RackPinionModel.java         keeps the static copy out of the chunk mesh
└── registry/
    ├── RackGearBlocks.java              block + item registration
    ├── RackGearBlockEntities.java       block entity types and renderers
    └── RackGearCreativeTab.java         creative tab (Registrate fills the contents)

src/main/resources/
├── META-INF/neoforge.mods.toml          mod metadata, templated from gradle.properties
├── META-INF/accesstransformer.cfg       empty; add entries when reaching Create/MC internals
├── assets/create_rack_gear/             hand-authored block models and textures
└── data/create_rack_gear/recipe/        crafting recipes

src/generated/resources/                 datagen output — committed, regenerate with runData
```

## How registration works

Registration goes through `CreateRegistrate`, the builder Create uses for its own content. A block
declared in `RackGearBlocks` also gets its blockstate, block model, item model, loot table and
`en_us` entry generated by `./gradlew runData` — so adding content is normally a single builder
chain plus a texture.

Entries built after `CreateRegistrate#setCreativeTab` (called in the mod constructor) are added to
the mod's creative tab automatically, which is why `RackGearCreativeTab` declares no display items.

## Next steps

- **Its own model.** The pinion currently parents `create:block/large_cogwheel`, so it is
  indistinguishable from a large cogwheel in hand and in world.
- **A Flywheel visual.** `RackPinionRenderer` draws on every backend rather than instancing through
  Flywheel; a `SingleAxisRotatingVisual` would batch it like Create's own cogwheels.
- **Cosmetic spin on contraptions**, per the table above.
- **A Ponder scene** under `data/create_rack_gear/ponder/`, which is how Create explains mechanics.

## License

MIT — see [LICENSE](LICENSE).
