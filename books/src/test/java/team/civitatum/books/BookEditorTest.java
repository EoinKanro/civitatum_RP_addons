package team.civitatum.books;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import team.civitatum.books.client.BookEditor;
import team.civitatum.books.document.BookDocument;
import team.civitatum.books.document.BookPage;
import team.civitatum.books.document.TextLayout;

class BookEditorTest {
    private static final TextLayout.GlyphWidth FIXED_FONT = (codePoint, style) -> (style & BookPage.BOLD) == 0 ? 6 : 7;

    @Test
    void pastedTextFlowsAcrossPagesWithoutLosingCharacters() {
        BookEditor editor = new BookEditor(new BookDocument());
        String text = "Путь на север. Река, лес и старая башня.\n".repeat(150);
        editor.insert(text, FIXED_FONT);
        assertTrue(editor.document.pages.size() > 2);
        assertEquals(text, joined(editor));
        for (BookPage page : editor.document.pages) {
            assertTrue(TextLayout.wrap(page, BookEditor.PAGE_WIDTH,
                    (point, style) -> FIXED_FONT.width(point, style) * page.fontSize / 9f).size()
                    <= BookEditor.textHeight(page) / BookEditor.lineHeight(page));
        }
    }

    @Test
    void undoAndRedoKeepTheLatestServerRevision() {
        BookEditor editor = new BookEditor(new BookDocument());
        editor.insert("Первый абзац", FIXED_FONT);
        editor.document.revision = 8;
        assertTrue(editor.undo());
        assertEquals("", joined(editor));
        assertEquals(8, editor.document.revision);
        assertTrue(editor.redo());
        assertEquals("Первый абзац", joined(editor));
        assertEquals(8, editor.document.revision);
    }

    @Test
    void illustrationReducesTextAreaButKeepsTheEntireText() {
        BookEditor editor = new BookEditor(new BookDocument());
        String original = "Пример текста с форматированием. ".repeat(100);
        editor.insert(original, FIXED_FONT);
        editor.selectPage(0);
        editor.currentPage().format(0, editor.currentPage().text().length(), BookPage.BOLD, null);
        editor.currentPage().image = "a".repeat(64);
        editor.currentPage().imageHeight = 200;
        editor.paginate(FIXED_FONT);
        assertEquals(original, joined(editor));
        assertEquals(BookPage.INK | BookPage.BOLD, editor.document.pages.get(1).styleAt(0));
    }

    @Test
    void tooManyPagesRollBackTheWholeInsertion() {
        BookDocument document = new BookDocument();
        while (document.pages.size() < BookDocument.MAX_PAGES) document.pages.add(new BookPage());
        BookEditor editor = new BookEditor(document);
        editor.selectPage(BookDocument.MAX_PAGES - 1);
        String before = editor.document.json();
        assertThrows(RuntimeException.class, () -> editor.insert("текст ".repeat(2000), FIXED_FONT));
        assertEquals(before, editor.document.json());
        assertEquals(BookDocument.MAX_PAGES - 1, editor.page);
    }

    @Test
    void wrappingAndCursorMovementRespectSupplementaryCharacters() {
        BookPage page = new BookPage();
        page.setText("A😀Б\n");
        assertEquals(3, TextLayout.next(page.text(), 1));
        assertEquals(1, TextLayout.previous(page.text(), 3));
        var lines = TextLayout.wrap(page, 7, FIXED_FONT);
        assertEquals("😀", page.text().substring(lines.get(1).start(), lines.get(1).end()));
        assertEquals(page.text().length(), lines.getLast().start());
    }

    private static String joined(BookEditor editor) {
        return editor.document.pages.stream().map(BookPage::text).collect(Collectors.joining());
    }
}
