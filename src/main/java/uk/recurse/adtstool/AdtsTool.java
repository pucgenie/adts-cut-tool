package uk.recurse.adtstool;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.recurse.adtstool.stream.*;
import uk.recurse.bitwrapper.BitWrapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AdtsTool {

    private static final Logger log = LoggerFactory.getLogger(AdtsTool.class);

    public static void main(String[] args) {
        Arguments arguments = new Arguments();
        try {
            JCommander jCommander = new JCommander(arguments);
            if (args.length == 0) {
                jCommander.setProgramName("adts-tool");
                jCommander.usage();
            } else {
                jCommander.parse(args);

                run(arguments);
            }
        } catch (UncheckedIOException e) {
            log.error(e.getCause().toString());
        } catch (ParameterException | IOException e) {
            log.error(e.toString());
        }
    }

    private static void run(Arguments arguments) throws IOException {
        final Supplier<Stream<AdtsFrame>> supplier;
        final BitWrapper bitWrapper = BitWrapper.create();
        if (arguments.input != null) {
            supplier = new FileADTSFrameSupplier(arguments.input, bitWrapper);
        } else {
            //new ReadableByteChannel();
            final ReadableByteChannel stdin = Channels.newChannel(System.in);
            supplier = () -> {return StreamSupport.stream(new ADTSFrameSpliterator(new MappedByteBuffer() {

            }, bitWrapper), false);};
        }
        try (IADTSFrameConsumer consumer = new FileADTSFrameConsumer(arguments.output)) {
            Cutter cutter = new Cutter(supplier, consumer);
            cutter.write(arguments.start, arguments.end);
            log.info("Finished writing to {}", arguments.output);
        }
    }

    private static class Arguments {
        @Parameter(
                names = {"-i", "--input"},
                description = "Input file",
                required = false
        )
        Path input;

        @Parameter(
                names = {"-o", "--output"},
                description = "Output file",
                required = false
        )
        Path output;

        @Parameter(
                names = {"-s", "--start"},
                description = "Start time (hh:mm:ss.xxx)",
                required = true,
                converter = LocalTimeConverter.class
        )
        LocalTime start;

        @Parameter(
                names = {"-e", "--end"},
                description = "End time (hh:mm:ss.xx)",
                required = true,
                converter = LocalTimeConverter.class
        )
        LocalTime end;
    }
}
