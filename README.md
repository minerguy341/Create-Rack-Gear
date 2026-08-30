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

The dev runs also load Sable and Create: Aeronautics, so the mechanic can be tested against
sub-level vehicles as well as Create contraptions. Aeronautics brings Simulated and Offroad with it
through jar-in-jar. Set `aeronautics_enable=false` in `gradle.properties` to leave them out; they are
dev-run companions only and are not dependencies of the built mod.

The first invocation downloads NeoForge and decompiles Minecraft, which takes several minutes and a
few GB of disk. Subsequent builds are fast.

For IDE setup, import the project as a Gradle project; ModDevGradle generates the run
configurations. In IntelliJ, run `./gradlew build` once first so the sources are attached.

## The rack and pinion

Two blocks:

- **Rack** — a toothed bar with an axis, no kinetics of its own. Teeth run along all four sides, so
  a pinion meshes with it from any side.
- **Driven Rack** — a rack segment with a shaft, for taking power off a rack line when the *pinion*
  is the part that moves. Drop one into the line wherever the shaft should be.
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
| on a moving contraption | placed in the world | the pinion spins and powers the network it is attached to |
| in the world | on a moving contraption | the pinion rolls, and powers any **Driven Rack** it passes over |
| in the world | in the world | nothing moves, so nothing turns |

Both directions are computed the same way, from the relative motion of the two parts. They differ
only in where the rotation is produced: rotation cannot leave a contraption — Create gives
contraptions no kinetic network — so when the pinion is the moving part, the generator has to be the
block standing in the world, which is what the Driven Rack is for.

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

Taking over the rendering means the pinion is never drawn from its baked model; see
[Rendering](#rendering) for how every path draws it instead, and what that costs.

### Locking up

Teeth that cannot turn the gear they are pressed into do not slip past it. So when the network a
pinion drives is **overstressed**, the rack stops too, and the contraption pushing it stalls
mid-stroke — a piston frozen halfway out, a train that will not pull away. Hang too much on a rack
line and the machine driving it jams, rather than the network quietly stopping while the piston
carries on.

Both directions lock, and each needs an actor to do it, since only an actor can stall a contraption:
`RackPinionMovementBehaviour` when the pinion is the part riding, and `RackMovementBehaviour` — the
only reason a plain rack has a tick at all — when the rack is.

While locked, the load is deliberately held on. Releasing it would drop the generated speed to zero,
which would un-overstress the network, which would clear the stall, which would start the whole thing
moving again — chattering once a tick. Holding it means the lock stays until you fix the network. The
hold is gated on the jam being this pinion's own, so a contraption stalled for its own reasons (a
drill against bedrock, say) parks its rack on a free pinion and generates nothing from it.

To get moving again: cut the load, add capacity, or break the rack or the pinion.

### The Driven Rack

A pinion riding a contraption produces rotation that has nowhere to go, but an actor *can* reach into
the world it passes over — so the generator lives on the world side instead. A Driven Rack is a rack
segment carrying a shaft: while a rolling pinion is meshed with it, it generates that pinion's speed
for whatever the shaft is connected to. Plain racks make up the rest of the line, so only the
segments you actually take power from carry a block entity.

Two axes matter and they are always perpendicular. The bar runs along the line, like any rack. The
shaft leaves along one of the two axes across the bar; placement aims it across your line of sight
and a wrench cycles it. A pinion only drives the rack if its own rotation axis matches that shaft —
otherwise the teeth are meshing but the shaft points the wrong way.

The pinion releases the rack it is leaving in the same tick it takes up the next one, so two racks in
one network never both claim to be a source, which Create would report as a conflict. A rack that
stops being renewed for more than two ticks drops back to zero on its own, in case a contraption is
unloaded mid-roll. The pattern is Create's own powered shaft, which is driven from outside by a steam
engine in exactly this way.

## Layout

```
src/main/java/com/minerguy341/rackgear/
├── CreateRackGear.java                      @Mod entry point, owns the CreateRegistrate
├── client/
│   └── RackGearPartialModels.java           models drawn outside the baked chunk mesh
├── content/
│   ├── RackMeshing.java                     meshing geometry, direction and speed conversion
│   ├── rack/
│   │   ├── RackTeeth.java                   shared teeth: what a pinion can mesh with
│   │   ├── RackBlock.java                   the plain toothed bar
│   │   ├── DrivenRackBlock.java             rack segment with a shaft, bar and shaft axes
│   │   └── DrivenRackBlockEntity.java       holds the rotation a passing pinion produces
│   └── pinion/
│       ├── RackPinionBlock.java             large cog that Create's propagator meshes with
│       ├── RackPinionBlockEntity.java       finds passing racks, generates the rotation
│       ├── RackPinionMovementBehaviour.java actor: rolls along world racks, drives Driven Racks
│       ├── RackPinionActorVisual.java       instanced rendering of the rolling cog
│       ├── RackPinionActorRenderer.java     fallback rendering of the rolling cog
│       ├── RackPinionRenderer.java          fallback rendering of the cog in the world
│       └── RackPinionModel.java             keeps the block out of the baked chunk mesh
└── registry/
    ├── RackGearBlocks.java                  block + item registration
    ├── RackGearBlockEntities.java           block entity types, visuals and renderers
    └── RackGearCreativeTab.java             creative tab (Registrate fills the contents)

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

## Rendering

Everything that turns is instanced through Flywheel, with a renderer for when the backend is off:

| Part | Flywheel | Fallback |
| --- | --- | --- |
| Pinion standing in the world | `SingleAxisRotatingVisual.of(RACK_PINION)` | `RackPinionRenderer` |
| Pinion rolling on a contraption | `RackPinionActorVisual` | `RackPinionActorRenderer` |
| Driven Rack shaft | `SingleAxisRotatingVisual::shaft`, Create's own shaft visual | `ShaftRenderer` |

A block whose model is drawn turning cannot also sit in the baked chunk mesh, or it would appear
twice. So `RackPinionModel` drops the pinion's quads, and every path above draws
`RackGearPartialModels.RACK_PINION` instead — the same split Create uses for its own cogwheels,
which is why their renderer draws `SHAFTLESS_LARGE_COGWHEEL` rather than the block's own model. The
partial is authored along Y, like Create's cog models, and each renderer turns it onto the block's
axis.

Dropping the baked quads has one known cost: anything that draws the block from its baked model
alone — a schematic preview, a Ponder scene — shows nothing where the pinion should be. If that
matters, the fix is the one Create uses for brackets: stash a flag in `ModelData` from
`getModelData`, which does get handed the world, and keep the static quads for every virtual world
except a contraption's.

## Compatibility

**Create contraptions** are the supported case throughout: pistons, pulleys, gantry carriages and
trains all carry racks and pinions, because all of them are `AbstractContraptionEntity` and all of
them tick actors.

**Create: Aeronautics** works differently, and interestingly so. Its vehicles are not Create
contraptions — Aeronautics 1.3.2 depends on [Sable](https://modrinth.com/mod/sable), "a library mod
for interactive moving block structures, or sub-levels", and its jar is full of sub-level machinery
(`SubLevelAssemblyHelperMixin`, `ServerSubLevelMixin`, `retain_in_sub_level` entity tags). A
sub-level keeps the assembled structure as **real blocks in a level of its own**, transformed and
rendered in the world, rather than snapshotting them into a `Contraption`.

That has a consequence worth knowing: **block entities aboard a sub-level tick**, so a Create kinetic
network does run on an Aeronautics vehicle. Carried kinetics — impossible on a Create contraption —
is simply how sub-levels work. It also means none of this mod's contraption code applies there: a
sub-level is not an `AbstractContraptionEntity`, and blocks aboard one are ticking block entities
rather than actors. Supporting it means a second detection path through Sable's own API, which is
also the version where a pinion aboard a ship needs no Driven Rack at all — it can generate straight
into the ship's own network. Sub-levels also rotate freely, so meshing there cannot assume the three
world axes.

### Sable's physics properties

Sable gives blocks their physical properties through data files at
`data/sable/physics_block_properties/`, each pairing a `selector` with the properties it applies:

```json
{ "selector": "#sable:heavy", "properties": { "sable:mass": 2.0 },
  "overrides": { "type=double": { "sable:mass": 4.0 } } }
```

So membership of tags like `sable:heavy` (mass 2.0), `sable:half_volume` (volume 0.5),
`sable:bouncy` (restitution 0.5) is the intended way for another mod to declare how its blocks behave
aboard a vehicle. This mod ships two: the rack blocks are `heavy`, being iron and andesite, and all
three blocks are `half_volume`, being bars and a cogwheel rather than solid cubes. Both tag files are
inert when Sable is absent. A mod that wants exact numbers can ship its own
`physics_block_properties` file instead of borrowing Sable's buckets.

### What the sub-level path would use

Sable's API is a workable target for it. `dev.ryanhcode.sable.sublevel.SubLevel`, with its server and
client subclasses, is the level itself; `SubLevelHelper` converts entity positions in and out of one
and walks connected chains; `SablePrePhysicsTickEvent` and `SablePostPhysicsTickEvent` bracket the
physics step. Most directly, `dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor` is the
sub-level's answer to a Create actor — a block entity aboard implements `sable$tick(ServerSubLevel)`
and `sable$physicsTick(ServerSubLevel, RigidBodyHandle, double)` — which is the natural home for a
pinion that has to notice the world rolling past beneath it. The world-to-sub-level transforms
themselves live in the bundled `sable-companion-common` library behind `SubLevelAccess`, which is the
next thing to read.

## Next steps

- **Its own model.** The pinion currently parents `create:block/large_cogwheel`, so it is
  indistinguishable from a large cogwheel in hand and in world.
- **Kinetic output for the rolling pinion**, per the section above.
- **A Ponder scene** under `data/create_rack_gear/ponder/`, which is how Create explains mechanics.
- **Sable sub-level support**, per the compatibility notes above.

## License

MIT — see [LICENSE](LICENSE).
