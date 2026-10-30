# Проверка в игре

Команды для чата (нужны права оператора, в одиночной игре — «Разрешить читы»). ID модов сверены с jar-файлами из `~/Downloads/mine-server/mods`.

## Подготовка

Наш инвентарь не открывается в творческом режиме, поэтому проверка — в выживании:

```
/gamemode survival
/effect give @s minecraft:resistance infinite 255 true
/effect give @s minecraft:saturation infinite 255 true
/time set day
/xp add @s 50 levels
```

Отключить эффекты после проверки: `/effect clear @s`.

## Инвентарь игрока (hud)

Порядок сетки — по строкам, как в ванильных экранах. Заполнить инвентарь разными вещами и сравнить с любым сундуком: предмет должен стоять в той же строке и столбце.

```
/clear @s
/give @s minecraft:stone 1
/give @s minecraft:dirt 2
/give @s minecraft:oak_log 3
/give @s minecraft:cobblestone 4
/give @s minecraft:sand 5
/give @s minecraft:gravel 6
/give @s minecraft:glass 7
/give @s minecraft:bricks 8
/give @s minecraft:apple 9
/give @s minecraft:bread 10
/give @s minecraft:iron_sword 1
/give @s minecraft:bow 1
```

Первые 9 стопок (камень … яблоко) ложатся в хотбар, хлеб, меч и лук — в первую строку основной сетки слева направо. Уменьшить окно игры или поднять размер слотов до 200%: строки переносятся, прокрутка колёсиком двигает их целиком.

Рюкзак и аксессуары (тоже по строкам):

```
/give @s camping:small_backpack
/give @s camping:large_backpack
/give @s camping:enderpack
/give @s aether:iron_ring
/give @s aether:golden_ring
/give @s aether:leather_gloves
```

Рюкзак надеть, открыть инвентарь, кнопка рюкзака внизу окна экипировки: сетка рюкзака заполняется по строкам. Надетый эндерпак (`camping:enderpack`), открытый той же кнопкой, показывается нашим окном рюкзака; поставленный на землю — ванильным сундуком в новом стиле.

## Ванильные блоки в новом стиле

Все экраны ниже — родные ванильные, с камнем, золотой рамкой и светлыми заголовками. Проверить: фон тёмный, слоты видны, заголовки «Сундук», «Инвентарь» и т. п. читаются, стрелки и шкалы прогресса работают, предметы перекладываются.

```
/give @s minecraft:chest 2
/give @s minecraft:trapped_chest
/give @s minecraft:barrel
/give @s minecraft:ender_chest
/give @s minecraft:shulker_box
/give @s minecraft:dispenser
/give @s minecraft:dropper
/give @s minecraft:hopper
/give @s minecraft:furnace
/give @s minecraft:blast_furnace
/give @s minecraft:smoker
/give @s minecraft:crafting_table
/give @s minecraft:crafter
/give @s minecraft:brewing_stand
/give @s minecraft:anvil
/give @s minecraft:enchanting_table
/give @s minecraft:grindstone
/give @s minecraft:smithing_table
/give @s minecraft:stonecutter
/give @s minecraft:loom
/give @s minecraft:cartography_table
```

Два сундука рядом — двойной сундук (54 слота).

Материалы для работы блоков:

```
/give @s minecraft:coal 64
/give @s minecraft:raw_iron 64
/give @s minecraft:beef 16
/give @s minecraft:blaze_powder 16
/give @s minecraft:nether_wart 16
/give @s minecraft:potion[potion_contents={potion:"minecraft:water"}] 3
/give @s minecraft:lapis_lazuli 64
/give @s minecraft:book 16
/give @s minecraft:iron_pickaxe
/give @s minecraft:diamond_sword
/give @s minecraft:netherite_upgrade_smithing_template
/give @s minecraft:netherite_ingot
/give @s minecraft:white_banner
/give @s minecraft:red_dye 16
/give @s minecraft:map 4
/give @s minecraft:paper 16
/give @s minecraft:enchanted_book[stored_enchantments={levels:{"minecraft:sharpness":5}}]
```

Что посмотреть отдельно:

- **Печь, плавильная печь, коптильня** — пламя и стрелка заполняются; книга рецептов (зелёная кнопка) открывается, её фон тоже в новом стиле.
- **Варочная стойка** — пузырьки и стрелка при варке воды с адским наростом.
- **Наковальня** — поле имени и цена опыта (зелёная или красная надпись).
- **Стол зачаровывания** — три строки вариантов и лазурит. Книжные полки вокруг не нужны для проверки.
- **Кузнечный стол** — подсказки шаблона и стойка с бронёй (ванильные, сохранились).
- **Крафтер** — щелчок по пустой ячейке сетки отключает её: на её месте золотая плашка с тёмным контуром.
- **Ткацкий станок** — подсказка в пустом слоте знамени (слева сверху) сменяется с подсказкой флага Supplementaries; обе тёмные с бронзовым контуром.
- **Наведение на слот** — в этих экранах и в нашем инвентаре полупрозрачная золотая подсветка вместо белой.
- **Маяк** — нужна пирамида:

  ```
  /fill ~2 ~ ~-1 ~4 ~ ~1 minecraft:iron_block
  /setblock ~3 ~1 ~ minecraft:beacon
  /give @s minecraft:iron_ingot 4
  ```

## Ванильные элементы во всех экранах

Кнопки, ползунки, поля ввода и полосы прокрутки нарисованы в нашем стиле и заменяются везде, где их использует Minecraft или мод:

- Esc → «Настройки»: кнопки и ползунки (FOV, громкость), наведение подсвечивает золотом.
- «Настройки» → «Управление» → «Привязка клавиш»: длинный список с полосой прокрутки.
- «Сетевая игра» → «Добавить»: поля ввода, при фокусе рамка светлее.
- Неактивная кнопка — например, «Подключиться» без выбранного сервера.

## Блоки модов (родные экраны)

Наши окна для блоков модов убраны: у них снова родные экраны. Проверить, что ничего не сломалось, всё открывается и работает как в моде. Фоны этих модов пока в своём стиле; их надписи не перекрашиваются.

```
/give @s farmersdelight:cooking_pot
/give @s farmersdelight:stove
/give @s vinery:apple_press
/give @s vinery:fermentation_barrel
/give @s herbalbrews:cauldron
/give @s herbalbrews:tea_kettle
/give @s beachparty:mini_fridge
/give @s beachparty:palm_bar
/give @s aether:altar
/give @s aether:freezer
/give @s aether:incubator
/give @s quark:crate
/give @s supplementaries:safe
/give @s supplementaries:sack
/give @s create:brown_toolbox
/give @s irons_jewelry:jewelcrafting_station
```

Сундуки модов, которые используют ванильный экран сундука (большинство шкафов и ящиков), получают новый стиль вместе с ним.
