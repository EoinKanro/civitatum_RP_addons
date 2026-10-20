package team.civitatum.books.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import team.civitatum.books.document.BookDocument;
import team.civitatum.books.server.BookStore;

/** Recovery files are local drafts; they never overwrite a newer server revision automatically. */
final class DraftStore {
    private final Path file;

    DraftStore(Path gameDirectory, UUID book) {
        file = gameDirectory.resolve("civitatum_books/drafts").resolve(book + ".json");
    }

    BookDocument recover(BookDocument serverCopy) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) > BookDocument.MAX_BYTES || serverCopy.signed) return null;
        BookDocument draft = BookDocument.parse(Files.readAllBytes(file));
        return !draft.signed && draft.revision == serverCopy.revision && !draft.json().equals(serverCopy.json()) ? draft : null;
    }

    void write(BookDocument document) throws IOException {
        byte[] bytes = document.bytes();
        BookDocument.parse(bytes);
        BookStore.writeAtomically(file, bytes);
    }
}
