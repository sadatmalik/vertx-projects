package chapter4_streams.jukebox;

import io.vertx.core.AbstractVerticle;

/**
 * The player can be controlled via a tool like netcat, with plain text commands to list all files, schedule a track to
 * be played, and pause or restart the stream.
 *
 * $ netcat localhost 3000
 * /list
 * /schedule [song]
 * /pause
 * /play
 * ^C
 *
 * @author sm@creativefusion.net
 */
public class NetControl extends AbstractVerticle {
}
