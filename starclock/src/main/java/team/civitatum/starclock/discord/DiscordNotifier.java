package team.civitatum.starclock.discord;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.neoforged.fml.loading.FMLPaths;
import team.civitatum.starclock.CivitatumStarclock;
import team.civitatum.starclock.network.ShowCardPayload;

/** Отправка не должна задерживать тик сервера, а её сбой не влияет на обряд: сообщение просто теряется. */
public final class DiscordNotifier {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "Starclock Discord");
        thread.setDaemon(true);
        return thread;
    });

    public static void predictionRead(String player, ShowCardPayload card, long delayTicks) {
        var text = new PredictionMessage.Card(card.sign(), card.orientation(), card.tier().name(),
                card.category(), card.target());
        WORKER.execute(() -> send(player, text, delayTicks));
    }

    private static void send(String player, PredictionMessage.Card card, long delayTicks) {
        try {
            String url = WebhookAddress.read(FMLPaths.CONFIGDIR.get().resolve(WebhookAddress.FILE_NAME));
            if (url.isEmpty()) return;
            int status = WebhookClient.post(url, PredictionMessage.json(player, card, delayTicks),
                    CardImage.render(card.sign(), card.orientation()), PredictionMessage.IMAGE_NAME);
            if (status / 100 != 2) CivitatumStarclock.LOG.error("Discord отклонил предсказание: HTTP {}", status);
        } catch (Exception error) {
            // В адресе вебхука лежит секретный токен, а текст сетевых ошибок может его содержать.
            CivitatumStarclock.LOG.error("Не удалось отправить предсказание в Discord: {}", error.getClass().getName());
        }
    }

    private DiscordNotifier() {}
}
