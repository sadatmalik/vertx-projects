package chapter4_streams.db_read_write;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Introduced in Vert.x 3.6, the fetch mode allows a stream consumer to request a number of data items, rather
 * than the stream pushing data items to the consumer.
 *
 * This works by pausing the stream and then asking for a varying number of items to be fetched later on, as
 * data is needed.
 *
 * The only difference between the two modes is that we need to request elements by calling fetch. You will not
 * likely need to play with fetch mode while writing Vert.x applications, but if you ever need to manually control
 * a read stream, it is a useful tool to have.
 *
 * In many circumstances, having data being pushed is all you need, and the requester can manage the back-pressure
 * by signaling when pausing is needed. If you have a case where it is easier for the requester to let the source
 * know how many items it can handle, then pulling data is a better option for managing the back-pressure. Vert.x
 * streams are quite flexible here.
 *
 * @author sm@creativefusion.net
 */
public class FetchDatabaseReader {

    private static final Logger logger = LoggerFactory.getLogger(FetchDatabaseReader.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        AsyncFile file = vertx.fileSystem().openBlocking("sample.db",
                new OpenOptions().setRead(true));

        // Putting a read stream in fetch mode

        // Remember that the RecordParser decorates the file stream. It is paused, and then the fetch
        // method asks for one element. Since the parser emits buffers of parsed data, asking for one
        // element in this example means asking for a buffer of four bytes (the magic number).

        // Eventually, the parser handler will be called to process the requested buffer, and nothing
        // else will happen until another call to the fetch method is made.

        RecordParser parser = RecordParser.newFixed(4, file);
        parser.pause();
        parser.fetch(1);
        parser.handler(header -> readMagicNumber(header, parser));
        parser.endHandler(v -> vertx.close());
    }

    private static void readMagicNumber(Buffer header, RecordParser parser) {
        logger.info("Magic number: {}:{}:{}:{}", header.getByte(0), header.getByte(1), header.getByte(2),
                header.getByte(3));
        parser.handler(version -> readVersion(version, parser));
        parser.fetch(1); // Here one item is a parser record
    }

    private static void readVersion(Buffer header, RecordParser parser) {
        logger.info("Version: {}", header.getInt(0));
        parser.delimitedMode("\n");
        parser.handler(name -> readName(name, parser));
        parser.fetch(1);
    }

    private static void readName(Buffer name, RecordParser parser) {
        logger.info("Name: {}", name.toString());
        parser.fixedSizeMode(4);
        parser.handler(keyLength -> readKey(keyLength, parser));
        parser.fetch(1);
    }

    private static void readKey(Buffer keyLength, RecordParser parser) {
        parser.fixedSizeMode(keyLength.getInt(0));
        parser.handler(key -> readValue(key.toString(), parser));
        parser.fetch(1);
    }

    private static void readValue(String key, RecordParser parser) {
        parser.fixedSizeMode(4);
        parser.handler(valueLength -> finishEntry(key, valueLength, parser));
        parser.fetch(1);
    }

    private static void finishEntry(String key, Buffer valueLength, RecordParser parser) {
        parser.fixedSizeMode(valueLength.getInt(0));
        parser.handler(value -> {
            logger.info("Key: {} / Value: {}", key, value);
            parser.fixedSizeMode(4);
            parser.handler(keyLength -> readKey(keyLength, parser));
            parser.fetch(1);
        });
        parser.fetch(1);
    }
}