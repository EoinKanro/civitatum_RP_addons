package team.civitatum.starclock.command;

import com.mojang.brigadier.context.CommandContext;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtil;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import team.civitatum.starclock.data.PendingPrediction;
import team.civitatum.starclock.data.StarClockData;
import team.civitatum.starclock.prediction.PredictionService;

public final class StarclockCommands {
    /** Тот же уровень, что у /time: команды меняют состояние всего мира. */
    private static final int ADMIN_LEVEL = 2;
    private static final float TICKS_PER_SECOND = 20;

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var cooldown = Commands.literal("cooldown")
                .then(Commands.literal("reset").executes(StarclockCommands::resetCooldown));
        var prediction = Commands.literal("prediction")
                .then(Commands.literal("list").executes(StarclockCommands::list))
                .then(Commands.literal("cancel").executes(StarclockCommands::cancel))
                .then(Commands.literal("fulfill").executes(StarclockCommands::fulfill));
        event.getDispatcher().register(Commands.literal("starclock")
                .requires(source -> source.hasPermission(ADMIN_LEVEL)).then(cooldown).then(prediction));
    }

    private static int resetCooldown(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var data = data(context);
        if (data.remainingCooldown(source.getServer().overworld().getDayTime()) <= 0) {
            source.sendFailure(Component.translatable("command.civitatum_starclock.cooldown.none"));
            return 0;
        }
        data.resetCooldown();
        source.sendSuccess(() -> Component.translatable("command.civitatum_starclock.cooldown.reset"), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var pending = data(context).pending();
        if (pending.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.civitatum_starclock.prediction.empty"), false);
            return 0;
        }
        long now = source.getServer().overworld().getGameTime();
        for (var prediction : pending) {
            source.sendSuccess(() -> line(source.getServer(), prediction, now), false);
        }
        return pending.size();
    }

    private static Component line(MinecraftServer server, PendingPrediction prediction, long now) {
        long left = prediction.dueGameTime() - now;
        Component due = left > 0
                ? Component.translatable("command.civitatum_starclock.prediction.left",
                        StringUtil.formatTickDuration((int) Math.min(left, Integer.MAX_VALUE), TICKS_PER_SECOND))
                : Component.translatable("command.civitatum_starclock.prediction.due");
        return Component.translatable("command.civitatum_starclock.prediction.line",
                playerName(server, prediction.player()), prediction.plan().eventId(),
                prediction.nextAction(), prediction.plan().actions().size(), due);
    }

    // Игрок может быть вне сети: его предсказание ждёт входа, а имя берётся из кэша профилей.
    private static String playerName(MinecraftServer server, UUID player) {
        var cache = server.getProfileCache();
        if (cache == null) return player.toString();
        return cache.get(player).map(profile -> profile.getName()).orElseGet(player::toString);
    }

    private static int cancel(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        int cancelled = data(context).clearPredictions();
        if (cancelled == 0) {
            source.sendFailure(Component.translatable("command.civitatum_starclock.prediction.empty"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.civitatum_starclock.prediction.cancelled",
                cancelled), true);
        return cancelled;
    }

    private static int fulfill(CommandContext<CommandSourceStack> context) {
        var source = context.getSource();
        var result = PredictionService.fulfillAll(source.getServer());
        if (result.total() == 0) {
            source.sendFailure(Component.translatable("command.civitatum_starclock.prediction.empty"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.civitatum_starclock.prediction.fulfilled",
                result.fulfilled(), result.waiting(), result.retrying()), true);
        return result.fulfilled();
    }

    private static StarClockData data(CommandContext<CommandSourceStack> context) {
        return StarClockData.get(context.getSource().getServer());
    }

    private StarclockCommands() {}
}
