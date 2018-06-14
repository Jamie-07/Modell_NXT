
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import lejos.nxt.Button;
import lejos.nxt.ColorSensor;
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Jan-Peter Schmidt
 */
public class Program {
    
    private static int colorId = -1;
    private static boolean passed;
    private static boolean colored;    
    
    private static int counterGrey = 0;
    private static int counterColored = 0;
        
    private static UltrasonicSensor sonic;
    private static ColorSensor cSensor;
    private static TouchSensor ts;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        Motor.B.rotate(180);
        
        Writer writer = null;
        try {
            String connected = "Connected";
            String waiting = "Waiting";
            String strength = "Signal: ";
            String disconnected = "Disconnected";
            LCD.drawString(waiting,0,0);
            LCD.refresh();
            BTConnection btc = Bluetooth.waitForConnection();
            LCD.clear();
            LCD.drawString(connected,0,0);
            LCD.refresh();
            DataOutputStream out = btc.openDataOutputStream();
            writer = new OutputStreamWriter(out, "UTF-8");
            
            //Hardware Config
            sonic = new UltrasonicSensor(SensorPort.S1);
            cSensor = new ColorSensor(SensorPort.S2);            
            ts = new TouchSensor(SensorPort.S3);
            Motor.A.setSpeed(400);
            Motor.B.setSpeed(400);
            
            Thread t1 = new Thread(new Runnable() {
                
                int i=0;
                
                public void run() {
                    
                    while(!Button.ESCAPE.isDown() && !Thread.currentThread().isInterrupted()) {
                        LCD.drawString(String.valueOf(i), 0, 1);
                        LCD.drawString(strength + ": " + btc.getSignalStrength(), 0, 2);
                        LCD.clear(4);
                        
                        int speedA = Motor.A.getSpeed();
                        int tcA = Motor.A.getTachoCount();
                        boolean isMovingA = Motor.A.isMoving();                        
                        
                        int speedB = Motor.B.getSpeed();
                        int tcB = Motor.B.getTachoCount();
                        boolean isMovingB = Motor.B.isMoving();
                        
                        int distance = sonic.getDistance();
                        String color = "";
                        
                        switch(colorId) {
                            
                            case 0: 
                                color = "red";
                                break;
                                
                            case 2:
                                color = "blue";
                                break;
                                
                            case 3:
                                color = "yellow";
                                break;
                                
                            case 6:
                                color = "grey";
                                break;
                                
                            case 7:
                                color = "black";
                                break;
                        }
                        
                        String s = "" + speedA + ";" + tcA + ";" + isMovingA + ";" + speedB + ";" + tcB + ";" + isMovingB + ";" + distance + ";" + color + ";" + counterColored + ";" + counterGrey;
                        
                        try {
                            out.writeUTF(s);
                            out.flush();
                        } catch(Exception e) {
                            LCD.clear(0);
                            LCD.drawString("ERROR writing", 0, 0);
                        }

                        /*String s = String.valueOf(btc.getSignalStrength());
                        out.writeUTF(s);
                        out.flush();*/

                        LCD.drawString("FLUSHED", 0, 3);
                        LCD.refresh();

                        if(btc.getSignalStrength()==-1) {
                            LCD.drawString(disconnected, 0, 0);
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            LCD.drawString("ErrI " + ex.getMessage(), 0, 7);
                            break;
                        }
                    }                    
                }
                
            });
            
            t1.setName("Data");
            
            Thread t2 = new Thread(new Runnable() {
                public void run() {
                    
                    while(!Button.ESCAPE.isDown() && !Thread.currentThread().isInterrupted()) {
                        passed=false;                    
                        Motor.A.backward();

                        //Something is on band
                        while (!Button.ESCAPE.isDown()) {
                            if(sonic.getDistance() < 10) {
                                Sound.twoBeeps();
                                passed=true;
                                break;
                            }
                        }
                        
                        colorId = -1;

                        while(!Button.ESCAPE.isDown()) {

                            ColorSensor cSensor = new ColorSensor(SensorPort.S2);
                            colorId = cSensor.getColorID();
                            LCD.drawString("Color is: " + String.valueOf(colorId), 0, 4);
                            LCD.refresh();

                            if(colorId!=7) {
                                break;
                            }
                        }    

                        if(colorId > 4) {
                            counterGrey++;
                        } else {
                            Motor.B.rotate(-50);                    
                            Motor.B.rotate(360);
                            counterColored++;
                        }
                    }
                }
            });
            
            t2.setName("Action");
            
            t1.start();
            t2.start();
            
            Button.ESCAPE.waitForPress();
            LCD.clearDisplay();
            LCD.drawString("STOPPING", 0, 0);
            
            t1.interrupt();
            t2.interrupt();
            
            Motor.A.stop();
            Motor.B.stop();           
            
            btc.close();
        } catch (Exception ex) {
            LCD.drawString("Err" + ex.getMessage(), 0, 7);
        }
    }       
}
