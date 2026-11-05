package team.civitatum.starclock.data;

/**
 * Кулдаун измеряется в игровых сутках, поэтому идёт по dayTime: сон и /time двигают только его,
 * а gameTime всегда растёт на 1 за тик. Время копится приращениями, а не сравнением с моментом обряда:
 * /time set возвращает dayTime к первым суткам мира, и сравнение заморозило бы кулдаун на все отмотанные дни.
 */
public record RitualCooldown(long elapsed, long seenDayTime) {
    public static final long DAY_TICKS = 24_000L;

    public static RitualCooldown start(long dayTime) {
        return new RitualCooldown(0, dayTime);
    }

    public RitualCooldown advance(long dayTime) {
        long passed = dayTime - seenDayTime;
        // Отмотка выглядит для игрока как переход вперёд к тому же времени суток.
        if (passed < 0) passed = Math.floorMod(passed, DAY_TICKS);
        return new RitualCooldown(elapsed + passed, dayTime);
    }

    public long remaining(long total) {
        return Math.max(0, total - elapsed);
    }
}
