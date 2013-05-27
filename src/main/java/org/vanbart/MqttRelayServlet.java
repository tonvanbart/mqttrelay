package org.vanbart;

import java.io.IOException;
import java.net.URISyntaxException;
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
import org.fusesource.mqtt.client.BlockingConnection;
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

    private static final Logger LOGGER = Logger.getLogger(MqttRelayServlet.class);

    private static final long serialVersionUID = -7289719281366784056L;
    public static String newLine = System.getProperty("line.separator");

    private final Set<RelaySocket> _members = new CopyOnWriteArraySet<RelaySocket>();

    private MQTT mqtt;

    private BlockingConnection blockingConnection;

    @Override
    public void init() throws ServletException {
        super.init();
        LOGGER.debug("init");
        String topic = getInitParameter("topic");
        String host = getInitParam("host", "localhost");
        mqtt = new MQTT();
        try {
            mqtt.setHost(host, 1883);
            CallbackConnection connection = mqtt.callbackConnection();
            connection.listener(new TestConnectionListener());
            connection.connect(new TestConnectionCallback(connection));
/*
//          Initial test: final code should *not* use blocking I/O but callback style!

            blockingConnection = mqtt.blockingConnection();
            LOGGER.debug("Connecting to "+host+"....");
            blockingConnection.connect();
            LOGGER.debug("Connected.");
            Topic[] topics = { new Topic(topic, QoS.AT_LEAST_ONCE)};
            byte[] qOses = blockingConnection.subscribe(topics);
            LOGGER.debug("MQTT connected and subscribed to topic '"+topic+"'.");
            Message message = blockingConnection.receive();
            LOGGER.info("Received message ["+new String(message.getPayload())+"]");
            message.ack();
*/

        } catch (URISyntaxException e) {
            LOGGER.error("Error setting MQTT host: "+e.getMessage(),e);
            throw new ServletException(e);
        } catch (Exception e) {
            LOGGER.error("Error on MQTT connect: "+e.getMessage(),e);
            throw new ServletException(e);
        }
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
        RelaySocket tailorSocket = new RelaySocket();
        return tailorSocket;
    }

    private String getInitParam(String name, String defaultValue)  {
        String result = getInitParameter(name);
        if (result == null) {
            LOGGER.info("Init parameter '" + name + "' not specified, defaulting to '"+defaultValue+"'");
            return defaultValue;
        }
        return result;
    }

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
            LOG.debug("onPublish(...)");
            LOG.info("topic=["+new String(topic.getData())+"]");
            LOG.info("payload=[" + new String(payload.toByteArray()) + "]");
            ack.run();
        }

        @Override
        public void onFailure(Throwable throwable) {
            LOG.error("onFailure("+throwable.getClass().getSimpleName()+")",throwable);
        }
    }

    class TestConnectionCallback implements Callback<Void> {

        private Logger LOG = Logger.getLogger(TestConnectionCallback.class);

        private CallbackConnection connection;

        TestConnectionCallback(CallbackConnection connection) {
            this.connection = connection;
        }

        @Override
        public void onSuccess(Void aVoid) {
            Topic[] topics = { new Topic("TT", QoS.AT_LEAST_ONCE)};
            connection.subscribe(topics, new Callback<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    LOG.info("Successfully subscribed to topics");
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
    }

    /**
     * {@inheritDoc}
     */
    class RelaySocket implements WebSocket.OnTextMessage {

        private final Logger LOGGER = Logger.getLogger(RelaySocket.class);

        private Connection _connection;

        public RelaySocket() {
            LOGGER.debug("create");
        }

        @Override
        public void onClose(int closeCode, String message) {
            _members.remove(this);
        }

        public void sendMessage(String data) throws IOException {
            _connection.sendMessage(data);
        }

        @Override
        public void onMessage(String data) {
            LOGGER.debug("Received: "+data);
        }

        public boolean isOpen() {
            return _connection.isOpen();
        }

        @Override
        public void onOpen(Connection connection) {
            LOGGER.debug("onOpen");
            _members.add(this);
            _connection = connection;
            try {
                connection.sendMessage("Server received Web Socket upgrade and added it to Receiver List.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}