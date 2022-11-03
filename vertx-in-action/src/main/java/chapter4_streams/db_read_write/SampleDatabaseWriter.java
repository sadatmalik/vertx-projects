package chapter4_streams.db_read_write;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;

/**
 * A program that writes sample database records to a file with two key/value entries. Demonstrates how
 * to use the Vert.x filesystem APIs to open a file, append data to a buffer, and then write it.
 *
 * @author sm@creativefusion.net
 */
public class SampleDatabaseWriter {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        /*
         * open a file
         */
        AsyncFile file = vertx.fileSystem().openBlocking("sample.db",
                new OpenOptions().setWrite(true).setCreate(true));

        /*
         * append data to a buffer
         */
        Buffer buffer = Buffer.buffer();

        // Magic number
        buffer.appendBytes(new byte[] { 1, 2, 3, 4});

        // Version
        buffer.appendInt(2);

        // DB name
        buffer.appendString("Sample database\n");

        // Entry 1
        String key = "abc";
        String value = "123456-abcdef";
        buffer
                .appendInt(key.length())
                .appendString(key)
                .appendInt(value.length())
                .appendString(value);

        // Entry 2
        key = "foo@bar";
        value = "Foo Bar Baz";
        buffer
                .appendInt(key.length())
                .appendString(key)
                .appendInt(value.length())
                .appendString(value);

        /*
         * write the buffer to file
         */
        file.end(buffer, ar -> vertx.close());
    }
}