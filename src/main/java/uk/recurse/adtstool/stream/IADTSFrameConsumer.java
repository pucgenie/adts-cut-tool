package uk.recurse.adtstool.stream;

import java.io.Closeable;
import java.util.function.Consumer;

public interface IADTSFrameConsumer extends Closeable, Consumer<AdtsFrame> {
}
