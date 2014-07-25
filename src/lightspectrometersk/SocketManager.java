/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package lightspectrometersk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *
 * @author stellarkite
 */

public class SocketManager {
    
    private static volatile SocketManager instance = null;
    private static Socket echoSocket;
    private static PrintWriter out;
    private static BufferedReader in;
    
    
    public final double __LED_VRef = 3.3; // Volts
    public final double __LED_VccResistor = 10; // Ohms
    public final int __ADC_MaxCounts = 4095; // 12 bits ADC

    public SocketManager() {
        
        
    }
    
    
    public static SocketManager getInstance() {
        if (instance == null) {
            synchronized (SocketManager.class) {
                if (instance == null) {
                    instance = new SocketManager();
                }
            }
        }
        return instance;
    }
    
    public String Send(String command) {
        
        out.println(command);
        String response = "";
        
        try {
            response = in.readLine();
            System.out.println("res: " + response);
        } catch (IOException e) {
            System.out.println("[ERR ] couldn't send heartbeat query");            
        }
        
        return response;
    }
    
    public boolean IsSystemUp() {
        
        String command = "heartbeat status\r";
        out.println(command);
        
        try {
            String response = in.readLine();
            System.out.println("res: " + response);
        } catch (IOException e) {
            System.out.println("[ERR ] couldn't send heartbeat query");            
        }
        
        return true;
    }
    
    public void Connect() {

        String hostName = "192.168.1.10";
        int portNumber = 16;

        try {
                echoSocket = new Socket(hostName, portNumber);
                out = new PrintWriter(echoSocket.getOutputStream(), true);
                in = new BufferedReader(
                        new InputStreamReader(echoSocket.getInputStream()) );
            
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to "
                    + hostName);
            System.exit(1);
        }

    }

    public int[] ExtractADCRead(String r, int nbytes, int nvalues) {

        int[] results = new int[nvalues];

        // Get rid of the doc
        int dotIndx = r.indexOf('.');
        String adcread = r.substring(dotIndx + 1);
        // Extract the nbytes.
        // Since it comes in the form of a string there should be
        //  here 2*nbytes characters
        int length = nbytes * 2;
        if (adcread.length() < length) {
            System.err.printf("[ERR ] Not enough data to extract %d bytes\n", nbytes);
            return results;
        }
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {

            // The radix = 16 --> hex
            // Then switch 4
            // Whatch the blanks because it converts it to 0xF
            char MSB = adcread.charAt(i);
            char LSB = adcread.charAt(i + 1);
            if (adcread.charAt(i) == ' ') {
                MSB = '0';
            }
            data[i / 2] = (byte) ((Character.digit(MSB, 16) << 4) + Character.digit(LSB, 16));

            // This is one byte in clear text
            //String hexC = adcread.substring(i*2, (i*2)+2);
            // I need to have them in exadecimal
            //System.out.printf("%x\n", data[i/2] );
        }

        // Now do the conversion
        byte channel = 0;
        int read = 0, readLSB = 0;
        for (int i = 0; i < length / 2; i++) {
            // From the first byte extract channel
            //  and the 4 MSB
            if (i % 2 == 0) {
                channel = (byte) (data[i] & (byte) 0x30);   // mask --> ( 00110000 );
                channel = (byte) (channel >> 4);
                read = (int) ((byte) (data[i] & 0xF));      // mask --> ( 00001111 );
            } else {
                // Switch 8 bits
                read = (read << 8);
                // And OR the 8 LSB
                readLSB = ((int) data[i]) & (int) 0x000000FF; // This mask is necessary here
                read = read | readLSB;
                //System.out.printf("%d, %d | ", channel, read );
                // save
                results[i - 1] = channel;
                results[i] = read;
                // Rewind
                channel = 0;
                read = 0;
            }

        }
        //System.out.printf("\n");

        return results;
    }

}

