package teamedit.liveedit;

import com.rabbitmq.client.*;

import java.io.IOException;

public class MessagingBridge {
    public Channel channel = null;
    private static MessagingBridge messagingBridge = null;
    public String exchangeName = "teameditapi";
    //public String queueName = "queue";
    //public String queueName = null;

    private MessagingBridge() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("guest");
        factory.setPassword("guest");
        //factory.setVirtualHost(virtualHost);
        factory.setHost("192.168.99.100");
        factory.setPort(4040);
        try {
            Connection conn = factory.newConnection();
            channel = conn.createChannel();
            channel.exchangeDeclare(exchangeName, "fanout", true);



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MessagingBridge getMessagingBridge() {
        if (messagingBridge == null) {
            messagingBridge = new MessagingBridge();
        }
        return messagingBridge;
    }

    public void send(String message) throws IOException {
        channel.basicPublish(exchangeName, "", null, message.getBytes());
    }
}
