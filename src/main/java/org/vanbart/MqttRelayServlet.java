package org.vanbart;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

/**
 * {@inheritDoc}
 * Servlet to relay MQTT messages over WebSocket.
 */
public class MqttRelayServlet extends WebSocketServlet {
    /** log4J logger instance. */
    private static final Logger LOGGER = Logger.getLogger(MqttRelayServlet.class);

    private static final long serialVersionUID = -7289719281366784056L;
    /** the set of connected WebSockets */
    private final Set<RelaySocket> connectedSockets = new CopyOnWriteArraySet<RelaySocket>();
    /** server name we listen to, see web.xml init-param 'host'*/
    private String mqttHost;
    /** topic name to listen to, see web.xml init-param 'topic'*/
    private String[] mqttTopic;

    private MQTT mqtt;

    @Override
    public void init() throws ServletException {
        super.init();
        LOGGER.debug("init");
        String topics = getInitParam("topic","TT");
        mqttTopic = topics.split(",");
        mqttHost = getInitParam("host", "localhost");
        mqtt = new MQTT();
        try {
            mqtt.setHost(mqttHost, 1883);
            CallbackConnection connection = mqtt.callbackConnection();
            connection.listener(new TestConnectionListener());
            connection.connect(new TestConnectionCallback(connection));

        } catch (URISyntaxException e) {
            LOGGER.error("Error setting MQTT host: "+e.getMessage(),e);
            throw new ServletException(e);

        } catch (Exception e) {
            LOGGER.error("Error on MQTT connect: "+e.getMessage(),e);
            throw new ServletException(e);
        }
        LOGGER.info("Init successful, listening to topic '"+mqttTopic+"' on host '"+mqttHost+"'");
    }

    /**
     * {@inheritDoc}
     */
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(request,
                response);
    }

    /**
     * {@inheritDoc}
     */
    public WebSocket doWebSocketConnect(HttpServletRequest request,
            String protocol) {
        LOGGER.debug("doWebSocketConnect");
        return new RelaySocket();
    }

    private String getInitParam(String name, String defaultValue)  {
        String result = getInitParameter(name);
        if (result == null) {
            LOGGER.info("Init parameter '" + name + "' not specified, defaulting to '"+defaultValue+"'");
            return defaultValue;
        }
        return result;
    }

    /**
     * Inner class which listens to MQTT message events.
     */
    class TestConnectionListener implements Listener {
        private Logger LOG = Logger.getLogger(TestConnectionListener.class);

        @Override
        public void onConnected() {
            LOG.debug("onConnected()");
        }

        @Override
        public void onDisconnected() {
            LOG.debug("onDisconnected()");
        }

        @Override
        public void onPublish(UTF8Buffer topic, Buffer payload, Runnable ack) {
            String topicName = topic.toString();
            String message = new String(payload.toByteArray());
            LOG.debug("MQTT receive: topic='" + topicName + "', message='" + message + "'");
            for (RelaySocket relaySocket : connectedSockets) {
                try {
                    relaySocket.sendMessage(message);
                } catch (IOException e) {
                    LOG.error("Error sending message to "+relaySocket+": "+e.getMessage(), e);
                }
            }
            ack.run();
        }

        @Override
        public void onFailure(Throwable throwable) {
            LOG.error("onFailure("+throwable.getClass().getSimpleName()+")",throwable);
        }
    }

    /**
     * Inner class which handles MQTT connection events.
     */
    class TestConnectionCallback implements Callback<Void> {

        private Logger LOG = Logger.getLogger(TestConnectionCallback.class);

        private CallbackConnection connection;

        TestConnectionCallback(CallbackConnection connection) {
            this.connection = connection;
        }

        @Override
        public void onSuccess(Void aVoid) {
            Topic[] topics = createTopics(mqttTopic);
            connection.subscribe(topics, new Callback<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    LOG.info("Successfully subscribed to topics.");
                }

                @Override
                public void onFailure(Throwable throwable) {
                    LOG.error("Subscription failed:" + throwable.getClass().getSimpleName(), throwable);
                }
            });
        }

        @Override
        public void onFailure(Throwable throwable) {
            LOG.error("onFailure("+throwable.getClass().getSimpleName()+")", throwable);
        }

        private Topic[] createTopics(String[] topicNames) {
            ArrayList<Topic> topics = new ArrayList<Topic>();
            for (String name : topicNames) {
                LOG.debug("adding topic for: "+name);
                topics.add(new Topic(name, QoS.AT_LEAST_ONCE));
            }
            return topics.toArray(new Topic[0]);
        }
    }

    /**
     * Inner WebSocket connection handler.
     * {@inheritDoc}
     */
    class RelaySocket implements WebSocket.OnTextMessage {

        private final Logger LOGGER = Logger.getLogger(RelaySocket.class);

        private Connection connection;

        public RelaySocket() {
            LOGGER.debug("create");
        }

        @Override
        public void onClose(int closeCode, String message) {
            LOGGER.debug("onClose("+closeCode+","+message+")");
            connectedSockets.remove(this);
        }

        public void sendMessage(String data) throws IOException {
            connection.sendMessage(data);
        }

        @Override
        public void onMessage(String data) {
            LOGGER.debug("Received: "+data);
        }

        public boolean isOpen() {
            return connection.isOpen();
        }

        @Override
        public void onOpen(Connection connection) {
            LOGGER.debug("onOpen");
            connectedSockets.add(this);
            this.connection = connection;
            try {
                connection.sendMessage(new Date().toString()+" websocket connected and added to receiver list.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}