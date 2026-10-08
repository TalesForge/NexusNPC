# NexusNPC

**[English](README.md) | [Русский](README.ru.md)**

Фреймворк для NeoForge 1.21.1, упрощающий создание кастомных NPC. NexusNPC
берёт на себя общую инфраструктуру (регистрация сущностей, AI отношения и
поведения, сеть, экран настройки в игре), чтобы другие моды могли добавлять
свои типы NPC, диалоги, скины и поведения, не переписывая базу заново.

## Возможности

- **`NpcEntity`** — публичный, расширяемый базовый класс (`PathfinderMob`)
  с состоянием анимации, изменяемыми настройками и контрактом сохранения в NBT.
- **Реестры вместо enum** — `NpcRegistries.ATTITUDES` и
  `NpcRegistries.BEHAVIORS` позволяют аддонам регистрировать свои типы
  отношения и поведения (`NpcAttitudeType`, `NpcBehaviorType`) рядом со
  встроенными `friendly` / `neutral` / `hostile` и
  `stay` / `wander` / `avoid_players`.
- **`NexusNPCApi`** — регистрация новых типов NPC (`registerType`), скинов
  (`registerSkin`) и спавн NPC по id типа (`spawn`) без дублирования кода
  атрибутов и рендерера.
- **События** — `NpcInteractEvent` (клик) и `NpcGoalsEvent` (сборка целей
  AI) позволяют другим модам подключаться без наследования.
- **Редактор в игре** — предмет «Посох контроля» открывает окно настройки
  на существующем NPC или окно создания при клике по блоку. Имя, отношение,
  поведение, скин, здоровье, урон и скорость редактируются и синхронизируются
  через собственный сетевой протокол.
- **Безопасное редактирование** — во время настройки NPC замирает
  (`isImmobile`) и становится неуязвимым; блокировка снимается сама, если
  редактирующий отключился, умер, ушёл далеко или мир вышел с паузы.
- **Проверка на сервере** — все данные от клиента (пакеты, настройки)
  обрезаются и проверяются на сервере по лимитам из `Config`.

## Требования

- Minecraft 1.21.1
- NeoForge `21.1.250`+

## Для разработчиков аддонов

```java
// Регистрация нового типа NPC на основе базовой сущности или своего наследника
public static final DeferredHolder<EntityType<?>, EntityType<NpcEntity>> MY_NPC =
        NexusNPCApi.registerType(ENTITY_TYPES, "my_npc", NpcEntity::new,
                NpcTypeProperties.create()
                        .category(MobCategory.CREATURE)
                        .size(0.6F, 1.8F)
                        .eyeHeight(1.62F));

// Регистрация своего поведения
public static final DeferredRegister<NpcBehaviorType> BEHAVIORS =
        DeferredRegister.create(NpcRegistries.BEHAVIORS, "mymod");
static {
  BEHAVIORS.register("patrol", PatrolBehavior::new);
}
```

Подробности — в Javadoc классов `NexusNPCApi`, `NpcRegistries`,
`NpcInteractEvent` и `NpcGoalsEvent`.

## NPC на любом мобе

NPC больше не отдельный класс-прослойка: логика NPC живёт на ванильных `Mob` / `PathfinderMob`.
Любого моба (ванильного или из другого мода) можно превратить в NPC — корова, зомби, житель, что угодно.

- Данные NPC (диалоги, квесты, торговля, отношение, поведение) хранятся в data attachment `nexusnpc:npc_data`.
  Моб «является NPC», если этот attachment есть. Остальные мобы не затрагиваются и ничего не стоят по производительности.
- `Npcs` — единая точка входа: `Npcs.enable(mob)`, `Npcs.disable(mob)`, `Npcs.dialogue(mob)`, `Npcs.merchant(mob)` и т. д.
- Режим ИИ (`NpcAiMode`): `VANILLA` — моб сохраняет свой ИИ, поверх только диалоги/квесты/торговля (по умолчанию для ванильных мобов);
  `OVERRIDE` — цели отношения/поведения заменяют ИИ (ванильные цели сохраняются и возвращаются при откате).
  `OVERRIDE` работает только для `PathfinderMob` на goal-ИИ; мобы на Brain (жители, пиглины, Warden…) остаются `VANILLA`.
- Mixin-ы минимальны: `MobMixin` (временное состояние + доступ к селекторам целей) и `LivingEntityMixin`
  (`isImmobile` во время редактирования). Остальное — события NeoForge.
- `NpcEntity` остался тонкой оболочкой только для внешнего вида (модель, скин, хитбокс) — для аддонов и ресурспаков.
- Конфиг: `allow_vanilla_mobs`, `blocked_entity_types` (по умолчанию дракон и иссушитель).

## Конфигурация

Серверный конфиг (`config/nexusnpc-server.toml`):

| Ключ | По умолчанию | Описание |
|---|---|---|
| `maxHealthLimit` | 100.0 | Максимальное здоровье NPC |
| `maxDamageLimit` | 20.0 | Максимальный урон атаки NPC |
| `allowHostileNpc` | true | Можно ли выбрать враждебное отношение |
| `maxNpcsPerLevel` | 200 | Лимит NPC на измерение (0 = без лимита) |
| `requireOpPermission` | true | Требовать права оператора (уровень 2) для создания/редактирования |

## Статус

Ранняя разработка. API ещё не стабилен и может меняться между версиями до
релиза 1.0.

## Лицензия

См. `LICENSE.txt`.

## Интеграция с NexusRPG (необязательная)

NexusNPC работает и сам по себе. Если установлен [NexusRPG](https://github.com/TalesForge/NexusRPG), у NPC появляются
RPG-настройки, которые хранятся в профиле NexusRPG, поэтому все моды видят одни и те же данные:

- поля редактора `nexusnpc:faction`, `nexusnpc:classes`, `nexusnpc:team` (и компактная секция в редакторе);
- отношение `nexusnpc:faction_based`: отвечает ударом на удар и нападает на тех, чья фракция враждебна фракции NPC.

Без NexusRPG этого нет (NPC, сохранённый с таким отношением, вернётся к стандартному).
Весь связанный код лежит в пакете `compat.rpg`; больше нигде в NexusNPC классы NexusRPG использовать нельзя.

Аддоны могут добавлять свои действия диалога (например, «Нанять») через `DialogueActionHandlers.register(id, handler)`
и `DialogueAction.Custom`; сервер сводит результат к странице успеха или неудачи.
