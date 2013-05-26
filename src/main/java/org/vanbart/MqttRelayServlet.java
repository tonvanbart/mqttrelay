package org.vanbart;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;
import org.fusesource.mqtt.client.MQTT;

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

    @Override
    public void init() throws ServletException {
        super.init();
        LOGGER.debug("init");

    }

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        getServletContext().getNamedDispatcher("default").forward(request,
                response);
    }

    public WebSocket doWebSocketConnect(HttpServletRequest request,
            String protocol) {
        LOGGER.debug("doWebSocketConnect");
        RelaySocket tailorSocket = new RelaySocket();
        return tailorSocket;
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