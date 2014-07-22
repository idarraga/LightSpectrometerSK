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

    
}

