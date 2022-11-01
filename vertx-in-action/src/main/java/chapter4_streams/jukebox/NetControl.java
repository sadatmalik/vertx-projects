package chapter4_streams.jukebox;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes a TCP server on port 3000 for receiving text commands to control what the jukebox plays.
 *
 * The commands in our text protocol are of the following form:
 *
 * /action [argument]
 *
 * The player can be controlled via a tool like netcat, with plain text commands to list all files, schedule a track to
 * be played, and pause or restart the stream.
 *
 * $ netcat localhost 3000
 * /list  - Lists the files available for playback
 * /schedule [song]  - Appends file [song] at the end of the playlist
 * /pause  - Pauses the stream
 * /play  - Ensures the stream plays
 * ^C
 *
 * Each text line can have exactly one command, so the protocol is said to be newline-separated. We need a parser for
 * this, as buffers arrive in chunks that rarely correspond to one line each. For example, a first read buffer could
 * contain the following:
 *
 * ettes.mp3
 * /play
 * /pa
 *
 * The next one may look like this:
 *
 * use
 * /schedule right-here-righ
 *
 * And it may be followed by this:
 *
 * t-now.mp3
 *
 * the solution is to concatenate buffers as they arrive, and split them again on newlines so we have one line per
 * buffer. Vert.x offers a handy parsing helper with the RecordParser class. The parser ingests buffers and emits new
 * buffers with parsed data, either by looking for delimiters or by working with chunks of fixed size.
 *
 * @author sm@creativefusion.net
 */
public class NetControl extends AbstractVerticle {


    private final Logger logger = LoggerFactory.getLogger(NetControl.class);

    @Override
    public void start() {
        logger.info("Start");
        vertx.createNetServer()
                .connectHandler(this::handleClient)
                .listen(3000);
    }

    /*
     * A record parser based on newlines over a TCP server stream
     *
     * The parser is both a read and a write stream, it functions as an adapter between two streams. It
     * ingests intermediate buffers coming from the TCP socket, and it emits parsed data as new buffers.
     *
     */
    private void handleClient(NetSocket socket) {
        logger.info("New connection");
        RecordParser.newDelimited("\n", socket) // Parse by looking for new lines
                .handler(buffer -> handleBuffer(socket, buffer)) // Now buffers are lines
                .endHandler(v -> logger.info("Connection ended"));
    }

    /*
     * Each buffer is known to be a line, so go directly to processing commands
     */
    private void handleBuffer(NetSocket socket, Buffer buffer) {
        String command = buffer.toString();
        switch (command) {
            case "/list":
                listCommand(socket);
                break;
            case "/play":
                vertx.eventBus().send("jukebox.play", "");
                break;
            case "/pause":
                vertx.eventBus().send("jukebox.pause", "");
                break;
            default:
                if (command.startsWith("/schedule ")) {
                    schedule(command);
                } else {
                    socket.write("Unknown command\n");
                }
        }
    }

    // --------------------------------------------------------------------------------- //

    private void schedule(String command) {
        String track = command.substring(10);
        JsonObject json = new JsonObject().put("file", track);
        vertx.eventBus().send("jukebox.schedule", json);
    }

    private void listCommand(NetSocket socket) {
        vertx.eventBus().request("jukebox.list", "", reply -> {
            if (reply.succeeded()) {
                JsonObject data = (JsonObject) reply.result().body();
                data.getJsonArray("files")
                        .stream().forEach(name -> socket.write(name + "\n"));
            } else {
                logger.error("/list error", reply.cause());
            }
        });
    }

}






