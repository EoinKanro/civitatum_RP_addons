package team.civitatum.books.network;

import team.civitatum.books.document.BookDocument;
import team.civitatum.books.document.PngImage;

public enum UploadKind {
    SAVE(BookDocument.MAX_BYTES),
    IMAGE(PngImage.MAX_BYTES),
    SIGN(BookDocument.MAX_BYTES);

    private final int maximumBytes;

    UploadKind(int maximumBytes) {
        this.maximumBytes = maximumBytes;
    }

    public int maximumBytes() {
        return maximumBytes;
    }
}
