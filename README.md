Experimental servlet which connects to a MQTT topic, and forwards any messages it receives over WebSocket.

### Status

This project should run out of the box, it has been tested running a local Mosquitto test broker (see below).

### Usage

To test with everything running locally, take the following steps.

* You need a MQTT broker, I am testing with [Mosquitto](http://mosquitto.org). Download and install.
* Open a command prompt and start the Mosquitto broker: `mosquitto`.
* Open a second command prompt and start the test servlet: `mvn jetty:run`.
* Wait until maven has finished downloading dependencies (first run only). In the log you should see the following messages
<pre>
    [INFO] Started Jetty Server
    2013-05-27 22:26:02,684 [MqttRelayServlet$TestConnectionListener] DEBUG - onConnected()
    2013-05-27 22:26:02,686 [MqttRelayServlet$TestConnectionCallback] INFO  - Successfully subscribed to topic TT
</pre>
* Open a third command prompt and use the provided mosquitto client to send a test message: `mosquitto_pub -t TT -m "Test"`
* The Jetty log should show messages indicating that the callback was called, and should log the message text.
* Open a browser (with WebSocket support) and go to [http://localhost:8080/relay.html](http://localhost:8080/relay.html)
* Click the "connect" button, the message box should show a message like `Mon May 27 22:26:13 CEST 2013 websocket connected and added to receiver list.`
    The websocket connection to the server is now active.
* Go to the command prompt with the mosquitto client again and send another message: `mosquitto_pub -t TT -m "Test2"`
* The Jetty log should again show a message; if you go to the web page the message you just sent should be in the message box.

### Configuration

The configuration is in src/main/webapp/WEB-INF/web.xml . In this file the following section configures the relay servlet: 

    <servlet>
        <servlet-name>RelayServlet</servlet-name>
        <servlet-class>org.vanbart.MqttRelayServlet</servlet-class>
        <init-param>
            <description>host of the MQTT server (defaults to localhost if omitted)</description>
            <param-name>host</param-name>
            <param-value>localhost</param-value>
        </init-param>
        <init-param>
            <description>Name of the MQTT topic to listen on</description>
            <param-name>topic</param-name>
            <param-value>TT</param-value>
        </init-param>
        <load-on-startup>0</load-on-startup>
    </servlet> 

See the description of the init parameters; you can configure the host where the MQTT broker lives, and the topic to listen
to. The port number is hardcoded to 1883 (the MQTT default port).<br>
If you change the topic name, in the commands above the -t parameter should be adjusted to the new value (obviously).
After you change the values you have to restart the server to make the changes active.

### License

This project is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
