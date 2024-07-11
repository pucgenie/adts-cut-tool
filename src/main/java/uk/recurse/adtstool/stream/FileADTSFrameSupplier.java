package uk.recurse.adtstool.stream;

import uk.recurse.bitwrapper.BitWrapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FileADTSFrameSupplier implements Supplier<Stream<AdtsFrame>> {

    private final Path input;
    private final BitWrapper bitWrapper;

    public FileADTSFrameSupplier(Path input, BitWrapper bitWrapper) {
        this.input = input;
        this.bitWrapper = bitWrapper;
    }

    @Override
    public Stream<AdtsFrame> get() {
        try (FileChannel in = FileChannel.open(input, StandardOpenOption.READ)) {
            final ByteBuffer buffer = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
            final Spliterator<AdtsFrame> spliterator = new ADTSFrameSpliterator(buffer, bitWrapper);
            return StreamSupport.stream(spliterator, false);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
