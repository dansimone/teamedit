package teamedit.liveedit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.rabbitmq.client.*;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.websockets.*;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implementation of Grizzly's WebSocketApplication for Live Edit Service.  Handles incoming
 * updates to a document, and sends out the appropriate deltas to other clients connected to the
 * same document.  Messaging is used to propagate document updates to *all* running instances of
 * the Live Edit Service.
 */
public class LiveEditWebSocketApp extends WebSocketApplication {

    private static Channel channel = null;
    private static final String EXCHANGE_NAME = "teameditapi";
    // Keep track of open messaging connections (one per WebSocket connection), so we can close them
    // when the WebSockets are closed.
    private Map<WebSocket, String> webSocketToConsumerTags = new HashMap<WebSocket, String>();
    private static final Logger logger = Grizzly.logger(LiveEditWebSocketApp.class);

    static {
        // Initialize RabbitMQ messaging session
        System.out.println("BBBAAAA");
        ConnectionFactory factory = new ConnectionFactory();
        // TODO - pass the following in as environment variabales
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setHost("192.168.99.100");
        factory.setPort(4040);
        System.out.println("BBBAAAA");
        try {
            System.out.println("BBBAAAA");
            Connection conn = factory.newConnection();
            System.out.println("BBBAAAA");
            channel = conn.createChannel();
            System.out.println("BBB " + channel);
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout", true);
        } catch (Exception e) {
            // TODO - handle failures resiliently here
            logger.info("Failed to connect to RabbitMQ: " + e.getMessage());
        }
    }

    @Override
    public WebSocket createSocket(ProtocolHandler handler, HttpRequestPacket request, WebSocketListener... listeners) {
        final WebSocket socket = new DefaultWebSocket(handler, request, listeners);

        // On each incoming connection (one per client), we'll create a distinct queue connection to RabbitMQ
        try {
            System.out.println("AAA " + channel);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, "");
            String consumerTag = channel.basicConsume(queueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                                           byte[] body) throws IOException {
                    String message = new String(body);

                    //
                    // Parse the incoming message from messaging
                    //
                    String clientUUID = null;
                    String change = null;
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(message);
                        clientUUID = jsonObject.getString("client_id");
                        change = jsonObject.getString("change");
                    } catch (JSONException e) {
                        logger.info("Invalid JSON from messaging: " + e.getMessage());
                        return;
                    }
                    logger.info("Received message from messaging, client uuid: " + clientUUID + ", change:" +
                            change);

                    //
                    // Send the message over WebSocket
                    //
                    // TODO:
                    // Handle the document name
                    // Handle the user
                    // Handle the version of the document
                    // To insert: content to insert, and index
                    // To delete: index to delete from, length to delete
                    logger.info("Sending message to WebSocket: " + jsonObject.toString());
                    socket.send(jsonObject.toString());
                }
            });
            webSocketToConsumerTags.put(socket, consumerTag);
        } catch (Exception e) {
            // TODO - handle failures resiliently here
            e.printStackTrace();
            logger.info(e.getMessage());
        }
        return socket;
    }

    @Override
    public void onMessage(WebSocket websocket, String message) {
        //
        // Parse the incoming message from the WebSocket
        //
        logger.info("Received message on WebSocket: " + message);
        String clientUUID = null;
        String change = null;
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(message);
            clientUUID = jsonObject.getString("client_id");
            change = jsonObject.getString("change");
        } catch (JSONException e) {
            logger.info("Invalid JSON from messaging: " + e.getMessage());
            return;
        }
        logger.info("Received message from WebSocket, client uuid: " + clientUUID + ", change:" +
                change);

        //
        // Send the message over messaging
        //
        try {
            channel.basicPublish(EXCHANGE_NAME, "", null, jsonObject.toString().getBytes());
        } catch (IOException e) {
            // TODO - handle failures resiliently here
            logger.info("Failed to send message over messaging: " + e.getMessage());
        }
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        // Close the messaging connection associated with this WebSocket
        String consumerTag = webSocketToConsumerTags.get(socket);
        if (consumerTag != null) {
            try {
                channel.basicCancel(consumerTag);
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }
    }
}
