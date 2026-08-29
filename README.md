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
| on a moving contraption | placed in the world | pinion spins and powers the network it is attached to |
| in the world | on a moving contraption | pinion rolls along the rack and spins, but drives nothing |
| in the world | in the world | nothing moves, so nothing turns |

Both directions are computed the same way, from the relative motion of the two parts; they differ
only in where the rotation can go. Rotation on a contraption has nowhere to go — Create gives
contraptions no kinetic network — so the rolling pinion turns visibly and stops there. See
[Making the rolling pinion do work](#making-the-rolling-pinion-do-work) for what would change that.

Any contraption that translates works as the carrier: pistons, pulleys, gantry carriages and trains.
A contraption that only rotates (a bearing) imparts no linear motion at the meshing point and is
ignored.

### The pinion as a contraption actor

A pinion riding a contraption is registered as a Create **actor** (a `MovementBehaviour`), which is
what lets it roll at all. Actors are ticked with their world position and motion on both sides, so
`RackPinionMovementBehaviour` reads the rolling speed straight from the contraption's motion,
looks up the world rack it is meshing with, and keeps the accumulated angle in the actor's data.
Rendering follows Create's own actors: `RackPinionActorVisual` instances the cog through Flywheel,
`RackPinionActorRenderer` draws it on the fallback path.

Being an actor also means the pinion is disabled by Contraption Controls like any other actor, and
it has `visitNewPosition` available — the hook Create's drills and harvesters use to affect the world
they pass over.

Taking over the rendering has one known cost: `RackPinionModel` now drops the block's baked quads
everywhere, so anything that draws the block from its baked model alone — a schematic preview, a
Ponder scene — shows nothing where the pinion should be. If that turns out to matter, the fix is the
one Create uses for brackets: stash a flag in `ModelData` from `getModelData`, which does get handed
the world, and keep the static quads for every virtual world except a contraption's.

### Making the rolling pinion do work

Rotation cannot leave a contraption, but an actor *can* reach into the world it passes. So the way to
make the second row of the table do something is to move the generator to the world side: give the
**rack** a kinetic block entity with an output axis, and have the passing actor pinion drive it, so
the rack powers whatever shafts or cogwheels it is connected to. That is a real change in scope —
a block entity per rack block, an output direction to place, and a hand-off as the pinion crosses
from one rack to the next — but it is the version where a train rolling past a rack line generates
rotation in the world.

## Layout

```
src/main/java/com/minerguy341/rackgear/
├── CreateRackGear.java                  @Mod entry point, owns the CreateRegistrate
├── content/
│   ├── RackMeshing.java                 meshing geometry, direction and speed conversion
│   ├── rack/RackBlock.java              the toothed bar
│   └── pinion/
│       ├── RackPinionBlock.java              large cog that Create's propagator meshes with
│       ├── RackPinionBlockEntity.java        finds passing racks, generates the rotation
│       ├── RackPinionMovementBehaviour.java  actor: rolls along world racks on a contraption
│       ├── RackPinionActorVisual.java        instanced rendering of the rolling cog
│       ├── RackPinionActorRenderer.java      fallback rendering of the rolling cog
│       ├── RackPinionRenderer.java           draws the cog spinning in the world
│       └── RackPinionModel.java              keeps the static copy out of the baked mesh
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
- **Kinetic output for the rolling pinion**, per the section above.
- **A Ponder scene** under `data/create_rack_gear/ponder/`, which is how Create explains mechanics.

## License

MIT — see [LICENSE](LICENSE).
