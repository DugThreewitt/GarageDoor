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
 * 
 */

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class GarageDoor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("MQTT Garage Door Opener ... started.");
    

        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();

        // provision gpio pin #08 as an output pin and turn on
        final GpioPinDigitalOutput garageOpenerPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "GarageOpener", PinState.LOW);

        // set shutdown state for this pin
        garageOpenerPin.setShutdownOptions(true, PinState.LOW);
        

        
        // create topic and server address	
        String openerPublishTopic = "garage/opener";
        String mqttServer = "tcp://192.168.1.10:1883";
        	
        // connect to mqtt server
        try {
            //System.out.println("MQTT Try");
            MqttClient mqttClient = new MqttClient(mqttServer, MqttClient.generateClientId());

            mqttClient.setCallback(new MqttCallback() { 
                @Override
                public void connectionLost(Throwable throwable) {
                System.out.println("Connection to MQTT is lost!");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                //System.out.println("Topic: " + topic + "\nMessage: " + mqttMessage);
                String msg = new String(mqttMessage.getPayload());

                if (openerPublishTopic.equals(topic)) {

                    if (msg.equals("true")) {
                        garageOpenerPin.pulse(500);
                    //    System.out.println("TRUE");
                    } else {
                        garageOpenerPin.low();
                    }
                }
            }      
                                                
            @Override
            public void deliveryComplete(final IMqttDeliveryToken iMqttDeliveryToken) {
                System.out.println("Message was delivered");
            }
        });
            mqttClient.connect();
            mqttClient.subscribe("garage/#", 2);

            System.out.println("Waiting for Message....");
        } 
        catch (Exception MqttException) {
                System.out.println("Connection Exception thrown");
            //MqttException.printStackTrace();
            }
        }      
    }

    

