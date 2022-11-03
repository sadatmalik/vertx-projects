package chapter4_streams.db_read_write;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example uses RecordParser with changing parsing mode switched on the fly. With RecordParser you can start
 * e.g. parsing buffers of fixed size 5, then switch to parsing based on tab characters, then chunks of 12 bytes,
 * and so on.
 *
 * @author sm@creativefusion.net
 */
public class DatabaseReader {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseReader.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        AsyncFile file = vertx.fileSystem().openBlocking("sample.db",
                new OpenOptions().setRead(true));

        // Reading a database stream, step 1
        // first read the magic number
        RecordParser parser = RecordParser.newFixed(4, file);
        parser.handler(header -> readMagicNumber(header, parser)); // read 4 bytes that represent the magic header
        parser.endHandler(v -> vertx.close());
    }

    // Reading a database stream, step 2

    /*
     * The readMagicNumber method extracts the four bytes of the magic number from a buffer. We know that the buffer
     * is exactly four bytes since the parser was in fixed-sized mode.
     */
    private static void readMagicNumber(Buffer header, RecordParser parser) {
        logger.info("Magic number: {}:{}:{}:{}", header.getByte(0), header.getByte(1), header.getByte(2),
                header.getByte(3));
        parser.handler(version -> readVersion(version, parser));
    }

    /*
     * The next entry is the database version, and it is an integer, so we don’t have to change the parser mode
     * because an integer is four bytes.
     *
     * Once the version has been read, the readVersion method switches to delimited mode to extract the database
     * name.
     */
    private static void readVersion(Buffer header, RecordParser parser) {
        logger.info("Version: {}", header.getInt(0));
        parser.delimitedMode("\n");
        parser.handler(name -> readName(name, parser));
    }

    /*
     * Read the name then start looking for a key length, so we need a fixed-sized mode in readName.
     */
    private static void readName(Buffer name, RecordParser parser) {
        logger.info("Name: {}", name.toString());
        parser.fixedSizeMode(4);
        parser.handler(keyLength -> readKey(keyLength, parser));
    }

    /*
     * reads the key name, the value length, and the proper value
     */
    private static void readKey(Buffer keyLength, RecordParser parser) {
        parser.fixedSizeMode(keyLength.getInt(0));
        parser.handler(key -> readValue(key.toString(), parser));
    }

    private static void readValue(String key, RecordParser parser) {
        parser.fixedSizeMode(4);
        parser.handler(valueLength -> finishEntry(key, valueLength, parser));
    }

    /*
     * finishEntry sets the parser to look for an integer and delegates to readKey
     */
    private static void finishEntry(String key, Buffer valueLength, RecordParser parser) {
        parser.fixedSizeMode(valueLength.getInt(0));
        parser.handler(value -> {
            logger.info("Key: {} / Value: {}", key, value);
            parser.fixedSizeMode(4);
            parser.handler(keyLength -> readKey(keyLength, parser));
        });
    }
}