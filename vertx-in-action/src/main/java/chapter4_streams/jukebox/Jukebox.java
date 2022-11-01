package chapter4_streams.jukebox;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * To listen to music, the user can connect a player such as VLC (see figure 4.3) or even open a web browser directly
 * at http://localhost:8080/.
 *
 * Download mp3 for which the filename is known:
 * curl -o out.mp3 http://localhost:8080/download/intro.mp3
 *
 * @author sm@creativefusion.net
 */
public class Jukebox extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(Jukebox.class);
    private enum State {PLAYING, PAUSED}
    private State currentMode = State.PAUSED;
    private final Queue<String> playlist = new ArrayDeque<>();

    // streaming
    private final Set<HttpServerResponse> streamers = new HashSet<>();
    private AsyncFile currentFile;
    private long positionInFile;


    @Override
    public void start() {
        logger.info("Start");

        EventBus eventBus = vertx.eventBus();
        eventBus.consumer("jukebox.list", this::list);
        eventBus.consumer("jukebox.schedule", this::schedule);
        eventBus.consumer("jukebox.play", this::play);
        eventBus.consumer("jukebox.pause", this::pause);

        vertx.createHttpServer()
                .requestHandler(this::httpHandler)
                .listen(8080);

        vertx.setPeriodic(100, this::streamAudioChunk); // streamAudioChunk periodically pushes new MP3 data
    }

    /*
     * play, pause and schedule playlist
     */
    private void play(Message<?> request) {
        logger.info("Play");
        currentMode = State.PLAYING;
    }

    private void pause(Message<?> request) {
        logger.info("Pause");
        currentMode = State.PAUSED;
    }

    private void schedule(Message<JsonObject> request) {
        String file = request.body().getString("file");
        logger.info("Scheduling {}", file);
        if (playlist.isEmpty() && currentMode == State.PAUSED) {
            currentMode = State.PLAYING;
        }
        playlist.offer(file);
    }

    /*
     * list available file tracks
     */
    private void list(Message<?> request) {

        // asynchronously get all files ending with .mp3 in the tracks/ folder

        vertx.fileSystem().readDir("tracks", ".*mp3$", ar -> {
            if (ar.succeeded()) {
                List<String> files = ar.result()
                        .stream()
                        .map(File::new)
                        .map(File::getName)
                        .collect(Collectors.toList());
                JsonObject json = new JsonObject().put("files", new JsonArray(files));
                request.reply(json);
            } else {
                logger.error("readDir failed", ar.cause());
                request.fail(500, ar.cause().getMessage());
            }
        });
    }

    /*
     * HTTP request handler and dispatcher
     */
    private void httpHandler(HttpServerRequest request) {
        logger.info("{} '{}' {}", request.method(), request.path(), request.remoteAddress());
        if ("/".equals(request.path())) {
            openAudioStream(request);
            return;
        }
        if (request.path().startsWith("/download/")) {

            // This string substitution prevents malicious attempts to read files from other directories

            String sanitizedPath = request.path().substring(10).replaceAll("/", "");
            download(sanitizedPath, request);
            return;
        }
        request.response().setStatusCode(404).end();
    }

    /*
     * Dealing with new stream players
     */
    private void openAudioStream(HttpServerRequest request) {
        logger.info("New streamer");

        // It is a stream, so the length is unknown - therefore set chunked
        HttpServerResponse response = request.response()
                .putHeader("Content-Type", "audio/mpeg")
                .setChunked(true);

        streamers.add(response);

        // When a stream exits, it is no longer tracked - therefore remove from streamers
        response.endHandler(v -> {
            streamers.remove(response);
            logger.info("A streamer left");
        });
    }

    /*
     * Handling file download requests efficiently
     */
    private void download(String path, HttpServerRequest request) {
        String file = "tracks/" + path;
        // Unless you are on a networked filesystem, the possible blocking time is marginal,
        // so we avoid a nested callback level:
        if (!vertx.fileSystem().existsBlocking(file)) {
            request.response().setStatusCode(404).end();
            return;
        }
        OpenOptions opts = new OpenOptions().setRead(true);
        vertx.fileSystem().open(file, opts, ar -> {
            if (ar.succeeded()) {
                downloadFile(ar.result(), request);
            } else {
                logger.error("Read failed", ar.cause());
                request.response().setStatusCode(500).end();
            }
        });
    }

    private void downloadFile(AsyncFile file, HttpServerRequest request) {
        HttpServerResponse response = request.response();
        response.setStatusCode(200)
                .putHeader("Content-Type", "audio/mpeg")
                .setChunked(true);

        // Back-pressure is taken care of while copying data between the two streams, below. This is so commonly
        // done when the strategy is to pause the source and not lose any data that the same code can be rewritten
        // with a pipe - see downloadFilePipe

        file.handler(buffer -> {
            response.write(buffer);
            if (response.writeQueueFull()) { // Writing too fast!
                file.pause(); // Back-pressure application by pausing the read stream
                response.drainHandler(v -> file.resume()); // Resuming when drained
            }
        });

        file.endHandler(v -> response.end());
    }

    private void downloadFilePipe(AsyncFile file, HttpServerRequest request) {
        HttpServerResponse response = request.response();
        response.setStatusCode(200)
                .putHeader("Content-Type", "audio/mpeg")
                .setChunked(true);

        file.pipeTo(response); // Pipes data from file to response
    }

    /*
     * streaming to connected streamers
     *
     * Read data every 100 milliseconds with read buffers of 4096 bytes - empirically found these values work
     * well for 320 KBps constant bit rate MP3 files on my laptop. They ensured no drops in tests while preventing
     * players from buffering too much data, and thus ending several seconds apart in the audio stream.
     *
     */
    private void streamAudioChunk(long id) {
        if (currentMode == State.PAUSED) {
            return;
        }
        if (currentFile == null && playlist.isEmpty()) {
            currentMode = State.PAUSED;
            return;
        }
        if (currentFile == null) {
            openNextFile();
        }

        // Buffers cannot be reused across I/O operations, so we need a new one each time
        currentFile.read(Buffer.buffer(4096), 0, positionInFile, 4096, ar -> {
            if (ar.succeeded()) {
                processReadBuffer(ar.result()); // This is where data is being copied to all players
            } else {
                logger.error("Read failed", ar.cause());
                closeCurrentFile();
            }
        });
    }

    private void openNextFile() {
        logger.info("Opening {}", playlist.peek());
        OpenOptions opts = new OpenOptions().setRead(true);

        // Again, using the blocking variant - rarely an issue for opening a file
        currentFile = vertx.fileSystem()
                .openBlocking("tracks/" + playlist.poll(), opts);
        positionInFile = 0;
    }

    private void closeCurrentFile() {
        logger.info("Closing file");
        positionInFile = 0;
        currentFile.close();
        currentFile = null;
    }

    private void processReadBuffer(Buffer buffer) {
        logger.info("Read {} bytes from pos {}", buffer.length(), positionInFile);
        positionInFile += buffer.length();
        if (buffer.length() == 0) { // end of file has been reached
            closeCurrentFile();
            return;
        }
        for (HttpServerResponse streamer : streamers) {
            if (!streamer.writeQueueFull()) { // Back-pressure - if write queue full simply discard the data
                streamer.write(buffer.copy()); // Remember, buffers cannot be reused
            }
        }
    }
}



