Experimental servlet which should connect to a MQTT topic, and forward any messages it receives over WebSocket.

### Status

This is still very much a work in progress. Receiving MQTT messages works, forwarding to WebSocket is yet
to be implemented.

### Usage

This project is tested and runs using a local MQTT broker as described below.
    * You need a MQTT broker, I am testing with [Mosquitto](http://mosquitto.org).
    * Open a command prompt and start the Mosquitto broker: `mosquitto`.
    * Open a second command prompt and start the test servlet: `mvn jetty:run`.
    * Open a third command prompt and use the provided mosquitto client to send a test message: `mosquitto_pub -t TT -m "Test"`
    * The Jetty log should show messages indicating that the callback was called, and should log the message.