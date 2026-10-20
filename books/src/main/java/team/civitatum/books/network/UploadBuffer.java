package team.civitatum.books.network;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/** One sequential, time-limited transfer per open book. No partial data reaches storage. */
public final class UploadBuffer {
    public static final int CHUNK_BYTES = 16384;
    public static final long TIMEOUT_MILLIS = 30000;
    private final UUID id;
    private final UploadKind kind;
    private final int total;
    private final long deadline;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    public UploadBuffer(UUID id, UploadKind kind, int total, long now) {
        if (total <= 0 || total > kind.maximumBytes()) throw new IllegalArgumentException("upload_size");
        this.id = id;
        this.kind = kind;
        this.total = total;
        deadline = now + TIMEOUT_MILLIS;
    }

    public boolean expired(long now) {
        return now >= deadline;
    }

    public void append(UUID transfer, UploadKind operation, int declaredTotal, int offset, byte[] bytes, long now) {
        if (!id.equals(transfer) || kind != operation || total != declaredTotal || expired(now)
                || offset != output.size() || bytes.length == 0 || bytes.length > CHUNK_BYTES
                || output.size() + bytes.length > total) {
            throw new IllegalArgumentException("upload_order");
        }
        output.writeBytes(bytes);
    }

    public boolean complete() {
        return output.size() == total;
    }

    public byte[] bytes() {
        if (!complete()) throw new IllegalStateException("upload_incomplete");
        return output.toByteArray();
    }
}
