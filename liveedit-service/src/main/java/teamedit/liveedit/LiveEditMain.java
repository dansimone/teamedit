package teamedit.liveedit;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Starts the Live Edit Service's Grizzly server.
 */
public class LiveEditMain {
    private static Logger logger = Logger.getLogger(LiveEditMain.class.getSimpleName());
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        HttpServer server = HttpServer.createSimpleServer("", getPort());
        final WebSocketAddOn addon = new WebSocketAddOn();
        for (NetworkListener listener : server.getListeners()) {
            listener.registerAddOn(addon);
        }
        final WebSocketApplication chatApplication = new LiveEditWebSocketApp();
        WebSocketEngine.getEngine().register("", "/edit", chatApplication);

        try {
            server.start();
        } catch (IOException e) {
            logger.info("Server failed to start: " + e.getMessage());
        }
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
        }
    }

    private static int getPort() {
        if (System.getenv("PORT") != null) {
            return Integer.valueOf(System.getenv("PORT"));
        }
        return DEFAULT_PORT;
    }
}