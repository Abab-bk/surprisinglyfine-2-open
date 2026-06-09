# 重新发布 Dungeon 101: v1.101.3

之前撤回的 Dungeon 101 版本现在重新发布了！借着这个机会，我还对游戏进行了彻底的检修，修复了所有 Dungeon 101 导致的 bug 和原来就存在的 bug！

---

### 中文

这个版本修了一堆 bug，尤其是战斗和存档相关的。

#### 🐛 战斗相关

- **修复了击杀敌人不算进任务的问题**。之前战斗里打死的敌人不会被 quest 系统记录，导致"击杀 X 只怪"的任务永远完不成。
- **修复了命中率计算反了的问题**。`calculateHitChance` 里读错对象了，导致玩家攻击和敌人攻击的命中率都不对。
- **修复了需要消耗品的战斗动不了的问题**。弹药、箭矢这类消耗品在战斗开始就被扣掉了，然后每 tick 又检查一次库存，发现不够就直接停了——战斗根本没法打。
- **修复了 `executeModifiers` 没效果的问题**。默认 multiplier 是 0，传进去的参数乘以 0 全没了。
- **生命药水现在是瞬时使用了**。之前是 180 秒的计时增益，喝下去不加血。现在拖进槽位立刻按最大生命值百分比回血，用完自动清空槽位。
- 给战斗计算加了几个防除零保护（攻击速度、波数、生命值比例），不会再出现 NaN 或者 Infinity 把战斗搞崩的情况。

#### 🐛 存档相关

- **修复了保存失败后永远无法再保存的问题**。`_isSaving` 标志在异常时没有被重置，导致后续所有保存请求都被忽略。现在异常时也能正常重置。
- **修复了读坏档直接崩溃的问题**。存档文件损坏或者格式不对的话，游戏会直接闪退。现在会优雅地跳过坏档，不会崩了。
- **修复了加载游戏时某个系统加载失败就整个加载中断的问题**。现在某个 `doLoad` 抛异常也不会阻止玩家进游戏。
- **修复了自动保存崩溃后静默停止的问题**。自动保存协程里加了异常处理，出错了会记日志然后继续。
- 存档 IO 层现在 catch 所有异常而非只 catch `FileNotFoundException`。

#### 🐛 数据丢失相关

- **修复了附魔装备重登后变白板的问题**。`PlayerLoadouts.doLoad` 构造物品时丢了 `customModifiers` 和 `enchantmentLevel`，每次重新加载存档附魔就没了。
- **修复了大量获得经验时溢出经验被清零的问题**。一次获得足够升多级的经验时，升完级剩余的经验全被归零了。

#### 🐛 其他

- **修复了任务进度数字不刷新的问题**。UI 上任务进度（比如"3/10"）只会在页面重新进入时才更新，因为读的是普通变量而不是 Compose state。
- **修复了加载存档后已完成的任务不自动变成完成状态的问题**。`doLoad` 恢复了任务进度，但是没调用 `checkQuests()`，所以已经满足条件的任务不会自动完成。
- **修复了领取任务奖励后下一个任务不自动完成的问题**。如果下一个任务的条件已经满足（比如等级要求），之前不会自动标完成。
- **修复了 `removeItem` 传 count=0 时把整叠物品删掉的问题**。
- **修复了长时间离线后奖励丢失的问题**。`rawCompleted` 用 `Int` 来算，溢出了就变成负数，循环直接跳过了。
- **修复了道具槽 tick 的竞态问题**。可能因为并发操作导致 NPE。
- 提升了一些稳定性相关的小问题。

---

### English

The previously recalled Dungeon 101 version is now re-released! I took this opportunity to do a thorough audit of the game and fixed every bug caused by Dungeon 101, along with many long-standing issues.

This release focuses on bug fixes, especially around combat and save/load.

#### 🐛 Combat

- **Fixed enemy kills not counting toward quests**. Previously, enemies killed in combat were never reported to the quest system, making "kill X" quests impossible to complete.
- **Fixed hit chance calculation reading the wrong actor**. `calculateHitChance` checked the defender instead of the attacker, making accuracy completely wrong for both player and enemy attacks.
- **Fixed combat tasks with consumables getting stuck forever**. Consumables (arrows, ammo, etc.) were deducted at task start, then `canConsume()` checked again on every tick and immediately returned false since inventory was already depleted.
- **Fixed `executeModifiers` being a silent no-op**. The default multiplier was 0, so every modifier value got multiplied by zero.
- **Health potions are now instant-use**. Previously they were 180-second timed buffs that didn't actually heal. Now they heal a percentage of max HP immediately when placed in the slot, then the slot clears.
- Added divide-by-zero guards for attack speed, waves, and health ratio in combat calculations.

#### 🛡️ Save System

- **Fixed saves getting permanently stuck after a failure**. The `_isSaving` flag was never reset on exception, blocking all future saves. Now the flag resets in `finally`.
- **Fixed corrupted save files causing a hard crash**. The read layer only caught `FileNotFoundException`; any other error (corrupted JSON, migration failure) crashed the game. Now all exceptions are caught gracefully.
- **Fixed a single `doLoad` failure aborting the entire game load**. If any system failed to load its state, the player couldn't enter the game at all.
- **Fixed auto-save silently dying after an exception**. Added error handling so the auto-save loop continues after failures.
- All file readers now catch `Exception` instead of just `FileNotFoundException`.

#### 🐛 Data Loss

- **Fixed enchanted items losing their enchantments on reload**. `PlayerLoadouts.doLoad` constructed items with only `template` and `count`, dropping `customModifiers` and `enchantmentLevel`.
- **Fixed excess XP being zeroed out on multi-level-up**. After leveling up multiple times at once, the remainder XP was unconditionally set to 0.

#### 🐛 Other

- **Fixed quest progress text not updating in real-time**. Progress display (e.g. "3/10") only refreshed when the screen was re-entered, because it read a plain field instead of a Compose state.
- **Fixed quests not auto-completing after loading a save**. `doLoad` restored condition counts but never called `checkQuests()`, so already-satisfied quests stayed stuck at "In Progress".
- **Fixed the next journey quest not auto-completing after claiming rewards**. If the next quest's conditions were already met, it wouldn't trigger completion.
- **Fixed `removeItem` with count=0 deleting the entire stack**.
- **Fixed offline reward loss from Int overflow**. `rawCompleted` used `Int`, which overflowed to negative for long offline periods, skipping all reward processing.
- **Fixed a TOCTOU race condition in `PropsContainer.tick()`**.
- Various small stability improvements.
