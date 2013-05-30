package org.vanbart;

import org.apache.log4j.Logger;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;

/**
 * Very quick and dirty MQTT test message server, sends a hardcoded message to the
 * provided server on port 1883 on topic TT.
 * @author Ton van Bart
 * @since 27-5-13 0:54
 */
public class TestSender {

    private static final Logger LOG = Logger.getLogger(TestSender.class);

    /**
     * Connect to the broker and send one single message on topic TT.
     * @param args the hostname to use, defaults to localhost.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        if (args.length != 1) {
            LOG.warn("Single hostname argument is missing, default to 'localhost'");
        } else {
            host = args[0];
        }
        LOG.info("Using host '" + host + "'");
        MQTT mqtt = new MQTT();
        mqtt.setHost(host, 1883);
        BlockingConnection connection = mqtt.blockingConnection();
        connection.connect();
        LOG.info("publishing");
        connection.publish("TT", "From TestSender".getBytes(),QoS.AT_LEAST_ONCE,false);
        LOG.info("Published.");
        connection.disconnect();
    }

}
