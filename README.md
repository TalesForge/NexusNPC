# MasterNPC

**[English](README.md) | [Русский](README.ru.md)**

A NeoForge 1.21.1 framework for creating custom NPCs. MasterNPC handles the
shared plumbing (entity registration, attitude/behavior AI, networking, an
in-game editor screen) so other mods can add their own NPC types, dialogue,
skins, and behaviors without rewriting the basics.

## Features

- **`NpcEntity`** — a public, extendable base class (`PathfinderMob`) with
  animation state, editable settings, and a save/load NBT contract.
- **Registries instead of enums** — `NpcRegistries.ATTITUDES` and
  `NpcRegistries.BEHAVIORS` let addons register their own attitude/behavior
  types (`NpcAttitudeType`, `NpcBehaviorType`) alongside the built-in
  `friendly` / `neutral` / `hostile` and `stay` / `wander` / `avoid_players`.
- **`MasterNpcApi`** — register new NPC entity types (`registerType`), skins
  (`registerSkin`), and spawn NPCs by type id (`spawn`) without duplicating
  attribute/renderer boilerplate.
- **Events** — `NpcInteractEvent` (right-click) and `NpcGoalsEvent` (AI goal
  assembly) let other mods hook in without subclassing.
- **In-game editor** — the Staff of Control item opens a settings screen on
  an existing NPC, or a creation screen on a clicked block. Name, attitude,
  behavior, skin, health, damage, and speed are all editable and synced over
  a custom payload protocol.
- **Safe editing** — while an NPC is being edited it freezes (`isImmobile`)
  and becomes invulnerable; the lock auto-releases if the editor
  disconnects, dies, walks away, or the game world unpauses.
- **Server-authoritative** — all client input (packets, settings) is
  clamped and validated server-side against `Config` limits.

## Requirements

- Minecraft 1.21.1
- NeoForge `21.1.250`+

## For addon developers

```java
// Register a new NPC type reusing the base entity or your own subclass
public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> MY_NPC =
        MasterNpcApi.registerType(ENTITY_TYPES, "my_npc", NpcEntity::new,
                NpcTypeProperties.create()
                        .category(MobCategory.CREATURE)
                        .size(0.6F, 1.8F)
                        .eyeHeight(1.62F));

// Register a custom behavior
public static final DeferredRegister<NpcBehaviorType> BEHAVIORS =
        DeferredRegister.create(NpcRegistries.BEHAVIORS, "mymod");
static {
    BEHAVIORS.register("patrol", PatrolBehavior::new);
}
```

See the Javadoc on `MasterNpcApi`, `NpcRegistries`, `NpcInteractEvent`, and
`NpcGoalsEvent` for details.

## Configuration

Server-synced config (`config/masternpc-server.toml`):

| Key | Default | Description |
|---|---|---|
| `maxHealthLimit` | 100.0 | Max health an NPC can be given |
| `maxDamageLimit` | 20.0 | Max attack damage an NPC can be given |
| `allowHostileNpc` | true | Whether the hostile attitude is selectable |
| `maxNpcsPerLevel` | 200 | Max NPCs per dimension (0 = unlimited) |
| `requireOpPermission` | true | Require permission level 2 to create/edit NPCs |

## Status

Early development. The API is not yet stable and may change between
versions until a 1.0 release.

## License

See `LICENSE.txt`.