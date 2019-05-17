package blockchain;

import java.net.*;
import java.io.*;

public class ThreadBlockchain implements Runnable {

    String name;

    public ThreadBlockchain(String name) {
        this.name = name;
    }

    public void run() {
        while (true) {
            try {
                //Faire un verrou pour blocker tant que le processus nést pas finis
                Thread.sleep(15000);
                Blockchain.verrouTorneo.lock();
                //if (Blockchain.blockAdded == true) {
                    //lockchain.blockAdded = false;
                    
                    System.out.println("\nTORNEO\n");
                    for (int i = 0; i < Blockchain.connectedList.size(); i++) {
                        try {
                            InetSocketAddress adr = new InetSocketAddress(Blockchain.connectedList.get(i), 6000);
                            Socket socket = new Socket();

                            socket.connect(adr, 100);

                            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                            out.flush();

                            String tour = "torneo";
                            out.writeObject(tour);
                            out.close();
                            socket.close();
                        } catch (SocketTimeoutException ste) {
                            System.err.println("Délai de connexion expire thread blockchain");
                        }finally{
                            
                        }

                    }
               // }else{
                 //   System.out.println("le block nést pas encore ajouté jattends encore");
                //}

            } catch (java.lang.InterruptedException e) {
                System.out.println("Interrupted !");
            } catch (UnknownHostException e) {
                System.out.println("Problemesssss");
            } catch (IOException a) {
                System.out.println("exception ThreadBlockchain");
                a.printStackTrace();
            } finally {
                Blockchain.verrouTorneo.unlock();
            }
        }
    }
}
