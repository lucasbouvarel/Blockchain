/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blockchain;

import static blockchain.Blockchain.connectedList;
import static blockchain.Blockchain.verrouConnectedList;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author labredes
 */
public class ThreadConnectedList implements Runnable {
    public void run(){
        for(int i=0;i<Blockchain.ipAddress.size();i++) {
                try{
                    verrouConnectedList.lock();
                    
                    
                    InetSocketAddress adr = new InetSocketAddress(Blockchain.ipAddress.get(i), 6000);
                    
                    Socket socket = new Socket();
                    socket.connect(adr,200);
                    ObjectOutputStream outa = new ObjectOutputStream(socket.getOutputStream());
                    

                    //get the ip address of the node which wants to join
                    SocketAddress remote=socket.getRemoteSocketAddress();
                    String ipAddress=remote.toString();
                    int deuxPoints=ipAddress.indexOf(":");
                    ipAddress=ipAddress.substring(1,deuxPoints);

                    
                    connectedList.add(ipAddress);

                    
                    outa.flush();

                    outa.writeObject("Connected");
                    outa.close();
                    socket.close();
                }catch (SocketTimeoutException ste) {
                    System.err.println("Délai de connexion expire blockchain node");
                   
                } catch (IOException ex) {
                    Logger.getLogger(Blockchain.class.getName()).log(Level.SEVERE, null, ex);
                }finally{
                    verrouConnectedList.unlock();
                   
                }
            }
    }
}
