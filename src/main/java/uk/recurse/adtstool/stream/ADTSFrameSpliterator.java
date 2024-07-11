package uk.recurse.adtstool.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.recurse.bitwrapper.BitWrapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class ADTSFrameSpliterator extends Spliterators.AbstractSpliterator<AdtsFrame> {

    private static final short SYNC_WORD = (short) 0xFFF0;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(6144);
    private final BitWrapper wrapper;
    private final ReadableByteChannel inChannel;
    private boolean inSync = true;

    public ADTSFrameSpliterator(ReadableByteChannel inChannel, BitWrapper wrapper) {
        super(Long.MAX_VALUE, Spliterator.NONNULL | Spliterator.ORDERED);
        this.inChannel = inChannel;
        this.wrapper = wrapper;
    }

    @Override
    public boolean tryAdvance(Consumer<? super AdtsFrame> action) {
        try {
            AdtsFrame nextFrame = nextFrame();
            if (nextFrame != null) {
                action.accept(nextFrame);
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AdtsFrame nextFrame() throws IOException {
        {
            int nSkipped = 0;
            while (buffer.remaining() > 1 && !atSyncWord()) {
                buffer.get();
                ++nSkipped;
            }
            if (nSkipped != 0) {
                log.info("skipped {} bytes", nSkipped);
            }
        }
        if (buffer.remaining() == 0) {
            buffer.clear();
            switch (inChannel.read(buffer)) {
                case -1:
                    return null;
                case 0:
                    throw new IOException("input stream is blocking operation");
            }
            buffer.flip();
        }
        AdtsFrame nextFrame = wrapper.wrap(buffer, AdtsFrame.class);
        buffer.position(buffer.position() + Math.max(1, nextFrame.length()));
        return nextFrame;
    }

    private boolean atSyncWord() {
        buffer.mark();
        short word = buffer.getShort();
        boolean atSyncWord = (short) (word & SYNC_WORD) == SYNC_WORD;
        buffer.reset();
        if (atSyncWord && !inSync) {
            log.info("ADTS sync word found");
            inSync = true;
        } else if (!atSyncWord && inSync) {
            log.warn("Ignoring non-ADTS data");
            inSync = false;
        }
        return atSyncWord;
    }
}
