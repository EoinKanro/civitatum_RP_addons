package team.civitatum.starclock.discord;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import team.civitatum.starclock.prediction.Constellation;
import team.civitatum.starclock.prediction.Orientation;

/** Сообщение показывает то же, что карта игрока этой ступени; добавлены только имя и примерное ожидание. */
public final class PredictionMessage {
    /** Содержимое карты без классов Minecraft: сообщение собирается и проверяется отдельно от игры. */
    public record Card(Constellation sign, Orientation orientation, String tier, String category, String target) {}

    public static final String IMAGE_NAME = "card.png";
    private static final int EMBED_COLOR = 0xE3C07B;
    private static final long TICKS_PER_MINUTE = 1200;
    /** Шаг округления: срок остаётся примерным, точная задержка в сообщение не попадает. */
    private static final long WAIT_STEP_MINUTES = 10;
    private static final String TARGET_PLAYER = "player";

    public static String json(String player, Card card, long delayTicks) {
        return json(player, card, delayTicks, language("ru_ru"), language("en_us"));
    }

    static String json(String player, Card card, long delayTicks,
                       Map<String, String> russian, Map<String, String> english) {
        var embed = new JsonObject();
        embed.addProperty("title", text(russian, "ui.civitatum_starclock.card")
                + " / " + text(english, "ui.civitatum_starclock.card"));
        embed.addProperty("color", EMBED_COLOR);
        embed.addProperty("description", section(russian, player, card, delayTicks)
                + "\n\n" + section(english, player, card, delayTicks));
        var image = new JsonObject();
        image.addProperty("url", "attachment://" + IMAGE_NAME);
        embed.add("image", image);
        var embeds = new JsonArray();
        embeds.add(embed);
        // Текст не должен никого упоминать, что бы ни оказалось в имени игрока.
        var mentions = new JsonObject();
        mentions.add("parse", new JsonArray());
        var message = new JsonObject();
        message.add("embeds", embeds);
        message.add("allowed_mentions", mentions);
        return message.toString();
    }

    public static long waitMinutes(long delayTicks) {
        long steps = Math.round((double) delayTicks / TICKS_PER_MINUTE / WAIT_STEP_MINUTES);
        return Math.max(1, steps) * WAIT_STEP_MINUTES;
    }

    private static String section(Map<String, String> language, String player, Card card, long delayTicks) {
        // Подчёркивание в нике иначе становится курсивом Discord.
        String name = player.replace("_", "\\_");
        var lines = new ArrayList<String>();
        String sign = "**" + text(language, card.sign().translationKey()) + "**";
        lines.add(card.orientation() == Orientation.NONE ? sign : sign + " — " + text(language,
                "orientation.civitatum_starclock." + card.orientation().name().toLowerCase(Locale.ROOT)));
        lines.add(text(language, card.sign().meaningKey(card.orientation())));
        lines.add(text(language, "tooltip.civitatum_starclock.tier", card.tier()));
        if (!card.category().isEmpty()) {
            lines.add(text(language, "ui.civitatum_starclock.event_type",
                    text(language, "event_type.civitatum_starclock." + card.category())));
        }
        if (!card.target().isEmpty()) {
            // На карте цель «Вы» обращена к читавшему, а в общем канале нужно его имя.
            lines.add(text(language, "ui.civitatum_starclock.target", TARGET_PLAYER.equals(card.target())
                    ? name : text(language, "target.civitatum_starclock." + card.target())));
        }
        lines.add(text(language, "discord.civitatum_starclock.player", name));
        lines.add(text(language, "discord.civitatum_starclock.wait", waitMinutes(delayTicks)));
        return String.join("\n", lines);
    }

    private static String text(Map<String, String> language, String key, Object... arguments) {
        String value = language.get(key);
        if (value == null) throw new IllegalStateException("Нет перевода " + key);
        return value.formatted(arguments);
    }

    // Выделенный сервер не держит русский перевод, поэтому оба файла читаются напрямую из ресурсов мода.
    private static Map<String, String> language(String code) {
        String path = "/assets/civitatum_starclock/lang/" + code + ".json";
        try (var stream = PredictionMessage.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Нет перевода " + code);
            var values = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            var result = new HashMap<String, String>();
            values.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
            return result;
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private PredictionMessage() {}
}
