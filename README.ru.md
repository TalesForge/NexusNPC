# MasterNPC

**[English](README.md) | [Русский](README.ru.md)**

Фреймворк для NeoForge 1.21.1, упрощающий создание кастомных NPC. MasterNPC
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
- **`MasterNpcApi`** — регистрация новых типов NPC (`registerType`), скинов
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
        MasterNpcApi.registerType(ENTITY_TYPES, "my_npc", NpcEntity::new,
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

Подробности — в Javadoc классов `MasterNpcApi`, `NpcRegistries`,
`NpcInteractEvent` и `NpcGoalsEvent`.

## Конфигурация

Серверный конфиг (`config/masternpc-server.toml`):

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