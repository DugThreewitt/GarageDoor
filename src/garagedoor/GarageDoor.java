/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package garagedoor;

/**
 *
 * @author Dug Threewitt
 * Uses pi4j to trigger garage door opener when MQTT gets a message.
 * Feb 28, 2019
 */

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import java.sql.Timestamp;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.LogRecord;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Locale;
import java.text.SimpleDateFormat;



public class GarageDoor {

    /**
     * @param args the command line arguments
     */
    
    private static MqttClient mqttClient;
    
    public static void main(String[] args) {
        
        GarageDoor repeater = new GarageDoor();
        repeater.start();

        
       // TimeZone tz = TimeZone.getTimeZone("America/Chicago");
       // Calendar currTime = Calendar.getInstance(tz, Locale.US);

        Logger logger = Logger.getLogger("GarageDoor");

        
        try {
            FileHandler fh = new FileHandler("/home/pi/garageDoor.log", true);
            logger.addHandler(fh);
            fh.setFormatter(new SimpleFormatter());
             
            System.out.println("Print: " + currTime.getTime().toString());
        }
        catch (Exception e) {
            
        }
        System.out.println("MQTT Garage Door Opener ... started.");
        logger.info("Garage Door Opener Started");
    

        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();

        // provision gpio pin #08 as an output pin and turn on
        final GpioPinDigitalOutput garageOpenerPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "GarageOpener", PinState.LOW);

        // set shutdown state for this pin
        garageOpenerPin.setShutdownOptions(true, PinState.LOW);
        

        
        // create topic 	
        String openerPublishTopic = "garage/opener";       
        	
        // connect to mqtt server
        try {
            mqttClient = GarageDoor.getMyClient();
            String logMsg;

            
            mqttClient.setCallback(new MqttCallback() { 
                @Override
                public void connectionLost(Throwable throwable) {
                System.out.println("Connection to MQTT is lost!");
                logger.info("MQTT Connection Lost");
                try {
                    mqttClient.reconnect();
                }
                catch (Exception e) {
                    System.out.println("Connection Exception thrown");
                    logger.log(Level.WARNING, "Connection Exception Thrown", e);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                String msg = new String(mqttMessage.getPayload());
                logger.info("New message on topic: " + topic);
                logger.info("Message: " + msg);
                
                if (openerPublishTopic.equals(topic)) {
                    if (msg.equals("true")) {
                        garageOpenerPin.pulse(500);
                    } else {
                        garageOpenerPin.low();
                    }
                }
            }      
                                                
            @Override
            public void deliveryComplete(final IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("Message was delivered");
                logger.info("Message Delivered");
            }
        });

            mqttClient.subscribe("garage/#", 2);
            logger.info("MQTT Connected");
            logger.info("Subscribing to: " + mqttClient.toString());
            

            System.out.println("Waiting for Message....");
            logger.info("Waiting for message");
        } 
        catch (Exception MqttException) {
                System.out.println("Connection Exception thrown");
                logger.warning("Connection Exception Thrown");
            }
        }
    
    /****************************************************
     * variables for tasks below
     * 
     ***************************************************/
    long delay = 30 * 10000;
    LoopTask task = new LoopTask();
    Timer timer= new Timer("30MinuteSend");
    
    /****************************************************
     * Timer start task to send message every 30 minutes
     * 
     ***************************************************/
    public void start() {
        
        timer.cancel();
        timer = new Timer("30MinuteSend");
        Date executionDate = new Date(); // no params = now
        timer.scheduleAtFixedRate(task, executionDate, delay);
    }
    
    /******************************************************
     * loop task that actually runs every 10 minutes
     * 
     ******************************************************/
    private class LoopTask extends TimerTask { 
       public void run() {
            try {
            String topic = "keepAlive";
            MqttMessage msg = new MqttMessage("test".getBytes());
            mqttClient.publish(topic, msg );
            }
            catch (Exception e) {
                System.out.println("message not sent");
            }
        }
    }
    
    /********************************************************
     * Create and return mqttClient
     * 
     ********************************************************/
    
    public static MqttClient getMyClient() {
        
        // create topic and server address	
        String mqttServer = "tcp://192.168.1.10:1883";
        String clientName = "garageDoorClient";
      
        // connect to mqtt server
        try {

            MqttClient mqttClient = new MqttClient(mqttServer, clientName);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setKeepAliveInterval(65000);
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            mqttClient.connect(connOpts);

        return mqttClient;
        }
        catch (Exception MqttException) {
                System.out.println("Connection Exception thrown");
        }
    return null;   
    }
    
}  


