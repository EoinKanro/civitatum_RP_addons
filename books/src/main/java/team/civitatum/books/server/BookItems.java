package team.civitatum.books.server;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import team.civitatum.books.document.BookDocument;
import team.civitatum.books.document.BookPage;

/** Vanilla book components remain intact on import. The custom component only holds a world-local UUID. */
public final class BookItems {
    private static final String BOOK_ID = "CivitatumBookId";

    public static boolean supported(ItemStack stack) {
        return stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK);
    }

    public static UUID id(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(BOOK_ID) ? tag.getUUID(BOOK_ID) : null;
    }

    public static void identify(ItemStack stack, UUID id) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putUUID(BOOK_ID, id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static BookDocument importVanilla(ItemStack stack, ServerPlayer player) {
        BookDocument document = new BookDocument();
        document.pages.clear();
        var written = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        var writable = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
        if (written != null) {
            document.title = written.title().get(player.isTextFilteringEnabled());
            document.author = written.author();
            document.signed = true;
            for (var page : written.pages()) addPage(document, page.get(player.isTextFilteringEnabled()).getString());
        } else if (writable != null) {
            for (var page : writable.pages()) addPage(document, page.get(player.isTextFilteringEnabled()));
        }
        if (document.pages.isEmpty()) document.pages.add(new BookPage());
        return document;
    }

    private static void addPage(BookDocument document, String text) {
        BookPage page = new BookPage();
        page.setText(text);
        document.pages.add(page);
    }

    public static ItemStack sign(ItemStack original, UUID id, BookDocument document) {
        ItemStack signed = new ItemStack(Items.WRITTEN_BOOK, original.getCount());
        signed.applyComponents(original.getComponentsPatch());
        signed.remove(DataComponents.WRITABLE_BOOK_CONTENT);
        String title = document.title.substring(0, Math.min(document.title.length(), 32));
        signed.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(title), document.author, 0,
                List.of(Filterable.passThrough(Component.translatable("books.vanilla_placeholder"))), true));
        identify(signed, id);
        return signed;
    }

    private BookItems() {}
}
