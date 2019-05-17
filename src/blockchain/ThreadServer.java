package blockchain;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.*;
import java.io.*;
import java.util.Map;

public class ThreadServer implements Runnable {

    Socket soc;

    public ThreadServer(Socket soc) {
        this.soc = soc;

    }

    public void run() {
        try {

            ObjectInputStream in = new ObjectInputStream(soc.getInputStream());

            Object objet;
            objet = in.readObject();

            Blockchain.verrouTorneo.lock();
            if (objet instanceof Transaction) {
                /*
                     * if the object received is a transaction, add it to the waiting list
                 */

                Transaction transacs = (Transaction) objet;

                //System.out.println("Transaction recu : " + transacs.trans);
                addTransaction(transacs);

            } else if (objet instanceof String) {

                String tour = (String) objet;

                if (tour.equals("torneo")) {
                    /*	If the String received is "torneo"
                         * 
                         * Choose a random number and broadcast it to every node of the blockchain
                     */
                    double random = Math.random();

                    Blockchain.randomArray.put("localhost", random);

                    for (int i = 0; i < Blockchain.connectedList.size(); i++) {
                        /*
                             * Send the random number to every node
                         */

                        try {
                            if (!Blockchain.connectedList.get(i).equals("localhost")) {

                                InetSocketAddress adr = new InetSocketAddress(Blockchain.connectedList.get(i), 6000);
                                Socket socket = new Socket();
                                socket.connect(adr, 100);
                                ObjectOutputStream outa = new ObjectOutputStream(socket.getOutputStream());
                                outa.flush();
                                outa.writeObject(random);
                                outa.close();
                                socket.close();
                            }
                        } catch (SocketTimeoutException ste) {
                            System.err.println("Délai de connexion expire envoie du random");
                        }

                    }

                    pickWinner(); //if i am the last one to choose a random number i have to check if randomarray==conenectedlist

                } else if (tour.equals("Winner")) {
                    /*
                         * if the String received is Winner
                     */
                    try {

                        Blockchain.verrouNumWin.lock();
                        Blockchain.numWin++; //add 1 to number of vote

                    } finally {
                        Blockchain.verrouNumWin.unlock();
                    }
                    chooseWinner();
                    //endIf received a string winner
                } else if (tour.equals("Connected")) {
                    //New node wants to join

                    //get the ip address of the node which wants to join
                    SocketAddress remote = soc.getRemoteSocketAddress();
                    String ipAddress = remote.toString();
                    int deuxPoints = ipAddress.indexOf(":");
                    ipAddress = ipAddress.substring(1, deuxPoints);

                    try {
                        Blockchain.verrouConnectedList.lock();

                        //test if the node is not currently in the connectedList
                        if (Blockchain.connectedList.contains(ipAddress)) {

                        } else {
                            boolean canJoin = false;
                            //test if the node is authorizated
                            for (int i = 0; i < Blockchain.ipAddress.size(); i++) {
                                if (ipAddress.equals(Blockchain.ipAddress.get(i))) {
                                    canJoin = true;
                                    break;
                                }
                            }
                            //if it's an authorizated node
                            if (canJoin == true) {
                                try {
                                    Blockchain.verrouNumberWaitingNode.lock();
                                    Blockchain.verrouWaitingNode.lock();
                                    Blockchain.numberOfWaitingNode++;
                                    Blockchain.waitingNode.add(ipAddress);
                                    System.out.println("Authorizated node " + ipAddress + " wants to join");
                                } finally {
                                    Blockchain.verrouNumberWaitingNode.unlock();
                                    Blockchain.verrouWaitingNode.unlock();
                                }

                            }
                        }
                    } finally {
                        Blockchain.verrouConnectedList.unlock();
                    }

                }

                //end if i received a string
            } else if (objet instanceof Double) {
                /*
                     * if a received a double --> the random number of a node which want to add the block
                 */

                double random = (double) objet;
               

                //get the ip address of the random number
                SocketAddress remote = soc.getRemoteSocketAddress();
                String ipAddress = remote.toString();
                int deuxPoints = ipAddress.indexOf(":");
                ipAddress = ipAddress.substring(1, deuxPoints);

                //put the random number in the list with his associate ipaddress
                Blockchain.randomArray.put(ipAddress, random);

                //if i've received every random number of every node -->pick the winner and send it a string winner
                pickWinner();

            } else if (objet instanceof Block) {
                /*
                     * If I receive an ArrayList (the new blockchain)
                     * Check if all the transaction in the waiting list are in the new block
                     * 
                 */

                @SuppressWarnings("unchecked")
                Block newBlock = (Block) objet;
                
                int sizeBlock = newBlock.transactions.size();

                //parcours le dernier block pour mettre a jour le UTXO blockchain
                for (int i = 0; i < sizeBlock; i++) {
                    newBlock.transactions.get(i).processTransaction2();
                }

                //if new autorizated nodes want to join the blockchain, add it to connected node
                try {
                    Blockchain.verrouNumberWaitingNode.lock();
                    Blockchain.verrouWaitingNode.lock();
                    if (Blockchain.numberOfWaitingNode != 0) {
                        for (int i = 0; i < Blockchain.waitingNode.size(); i++) {
                            try {
                                Blockchain.verrouConnectedList.lock();
                                Blockchain.connectedList.add(Blockchain.waitingNode.get(i));

                            } finally {
                                Blockchain.verrouConnectedList.unlock();

                            }

                        }
                        //reset the variables
                        Blockchain.numberOfWaitingNode = 0;
                        Blockchain.waitingNode = new ArrayList<String>();
                    }
                } finally {

                    Blockchain.verrouNumberWaitingNode.unlock();
                    Blockchain.verrouWaitingNode.unlock();
                }

                

                if (waitingEqualsToTransactionBlock(newBlock)) {
                    //if all the transactions of the blocks are equals of the waiting list
                    //Reset the waiting transaction

                    try {
                        Blockchain.verrouWaitingTransaction.lock();
                        Blockchain.waitingTransaction = new ArrayList<Transaction>();
                    } finally {
                        Blockchain.verrouWaitingTransaction.unlock();
                    }

                } else {
                    //If there is a difference between them
                    try {
                        Blockchain.verrouWaitingTransaction.lock();
                        Blockchain.verrouBlockchain.lock();
                        Blockchain.verrouConnectedList.lock();
                        Transaction trans;

                       

                        //remove every transactions of the waiting list that are in the new block
                        for (int i = 0; i < Blockchain.waitingTransaction.size(); i++) {
                            trans = Blockchain.waitingTransaction.get(i);

                            for (int j = 0; j < newBlock.transactions.size(); j++) {
                                if (equalsTransactions(Blockchain.waitingTransaction.get(i), newBlock.transactions.get(j))) {
                                  
                                    Blockchain.waitingTransaction.remove(i);
                                    i--;
                                    break;
                                }
                            }

                        }

                        //Send to every node the transactions of the transaction list that have not been added to the new block 
                        for (int j = 0; j < Blockchain.connectedList.size(); j++) {
                            if (Blockchain.connectedList.get(j).equals("localhost")) {

                                continue;
                            }
                            for (int i = 0; i < Blockchain.waitingTransaction.size(); i++) {
                                try {

                                    InetSocketAddress adr = new InetSocketAddress(Blockchain.connectedList.get(j), 6000);
                                    Socket socket = new Socket();
                                    socket.connect(adr, 100);

                                    ObjectOutputStream outa = new ObjectOutputStream(socket.getOutputStream());
                                    outa.flush();

                                    outa.writeObject(Blockchain.waitingTransaction.get(i));
                                    outa.close();

                                    socket.close();
                                } catch (SocketTimeoutException ste) {
                                    System.err.println("Délai de connexion expire envoie waiting transaction");
                                }

                            }
                        }
                       // System.out.println("Hash of the Block : "+newBlock.hash);
                        //System.out.println("Hash of the previous Block : "+newBlock.previousHash);
                        
                        
                        //System.out.println(newBlock.toString());
                    } finally {
                        Blockchain.verrouWaitingTransaction.unlock();
                        Blockchain.verrouBlockchain.unlock();
                        Blockchain.verrouConnectedList.unlock();
                    }

                }
                try{
                            FileOutputStream fileOut=new FileOutputStream("tamano.txt",true);
                            ObjectOutputStream out= new ObjectOutputStream(fileOut);
                            out.writeObject(newBlock);
                            out.close();
                            fileOut.close();
                            System.out.println("ecrit");
                        }catch(FileNotFoundException e){
                        }catch(IOException e){
                        
                        }
                Blockchain.blockchain.add(newBlock);
                /*System.out.println("block recu");
                System.out.println(newBlock.toString());
                System.out.println("calcul du hash : "+newBlock.calculateHash());*/

                
                Blockchain.blockAdded=true;
                Blockchain.isChainValid2();
               
                System.out.println("Personne A state : " + ThreadCreateTransaction.getPersonFromId(2).getState() + "\n\n");
                System.out.println("Personne B state : " + ThreadCreateTransaction.getPersonFromId(3).getState() + "\n\n");

            } else {
                System.out.println("Object received that not match with the consensus algorithm");
            }
            

            in.close();

            soc.close();
        } catch (ClassNotFoundException e) {
            System.out.println("Erreur dans la reception");
            e.printStackTrace();
            System.out.println("Problemesssss");
        } catch (IOException a) {
            System.out.println("Exception Thread Blockchain");
            a.printStackTrace();
        }finally{
            Blockchain.verrouTorneo.unlock();
        }

    }

    /*
	 * 
	 * Return true if the last block added to the blockchain have the same transactions than the waiting transaction list of the node
     */
    public boolean waitingEqualsToTransactionBlock(Block block) {
        try {
            Blockchain.verrouBlockchain.lock();
            Blockchain.verrouWaitingTransaction.lock();
            
            int tailleBlockTransac = block.transactions.size();
            int tailleWaitingTransac = Blockchain.waitingTransaction.size();
            if (tailleBlockTransac != tailleWaitingTransac) {
                return false;
            } else {
                for (int i = 0; i < tailleBlockTransac; i++) {

                    if (equalsTransactions(block.transactions.get(i), Blockchain.waitingTransaction.get(i))) {

                    } else {
                        return false;
                    }

                }
            }
        } finally {
            Blockchain.verrouBlockchain.unlock();
            Blockchain.verrouWaitingTransaction.unlock();
        }

        return true;
    }

    /*
	 * 
	 * Return true if the two transactions are equals
     */
    public boolean equalsTransactions(Transaction a, Transaction b) {

        if (a.trans.equals(b.trans)) {

        } else {

            return false;
        }

        if (a.transactionId.equals(b.transactionId)) {

        } else {

            return false;
        }

        if (a.sender.equals(b.sender)) {

        } else {

            return false;
        }

        if (a.reciepient.equals(b.reciepient)) {

        } else {

            return false;
        }

        if (Arrays.equals(a.signature, b.signature)) {

        } else {

            return false;
        }

        return true;
    }

    /* Return true if the transaction received is not in the waiting transaction list
	 * Add the transaction received if it's not in the waiting transaction list
     */
    public boolean addTransaction(Transaction transacs) {

        boolean here = true;
        try {
            Blockchain.verrouWaitingTransaction.lock();
            Transaction trans;
            for (int i = 0; i < Blockchain.waitingTransaction.size(); i++) {
                trans = Blockchain.waitingTransaction.get(i);
                if (equalsTransactions(transacs, trans)) {
                    here = false;

                }
            }
            if (here == true) {
                Blockchain.waitingTransaction.add(transacs);
            }
        } finally {
            Blockchain.verrouWaitingTransaction.unlock();

        }

        return here;
    }

    public void pickWinner() throws IOException {
        try {
            Blockchain.verrourandomArray.lock();
            
            if (Blockchain.randomArray.size() == (Blockchain.connectedList.size())) {

                double inter = 0;
                String ipWinner = "";
                for (HashMap.Entry<String, Double> entry : Blockchain.randomArray.entrySet()) {
                    if (inter < (double) entry.getValue()) {
                        inter = (double) entry.getValue();
                        ipWinner = (String) entry.getKey();
                    }

                }

                //send winner to the ipwinner
                System.out.println("The winner is" + ipWinner);
                if (ipWinner.equals("localhost")) {
                    Blockchain.numWin++;

                    chooseWinner();
                } else {
                    InetSocketAddress adr = new InetSocketAddress(ipWinner, 6000);
                    Socket socket = new Socket();
                    socket.connect(adr, 100);
                    ObjectOutputStream outa = new ObjectOutputStream(socket.getOutputStream());
                    outa.flush();
                    outa.writeObject("Winner");
                    outa.close();
                    socket.close();
                }
                Blockchain.randomArray = new HashMap<String, Double>();
            }

        } catch (SocketTimeoutException ste) {
            System.err.println("Délai de connexion expire envoie winner");
        } finally {
            Blockchain.verrourandomArray.unlock();
        }

    }

    public void chooseWinner() throws IOException {

        if (Blockchain.numWin == (Blockchain.connectedList.size())) { //If i have 100% of the vote 
            try {
                System.out.println("I am the winner");
                long startTime=System.nanoTime();
                Blockchain.verrouNumWin.lock();
                
                Blockchain.verrouBlockchain.lock();
                Blockchain.verrouWaitingTransaction.lock();
                Blockchain.numWin = 0; //reset the number of vote to 0 for the next block
                //reset les array de random 
                if (Blockchain.waitingTransaction.size() == 0) {
                    /*
                         * if there is no transaction in the waiting list
                     */
                    System.out.println("0 transaction have been made, no block is added");
                    Blockchain.blockAdded=true;
                } else {
                    /*
                         * if there are transactions
                     */
                    //Get the lastblock hash for the next block
                    int taille = Blockchain.blockchain.size();
                    Block lastBlock = Blockchain.blockchain.get(taille - 1);
                    String prevHash = lastBlock.hash;

                    Block block = new Block(prevHash);

                    Transaction trans;
                    for (int i = 0; i < Blockchain.waitingTransaction.size(); i++) { //add every transaction of the waiting list in the block

                        trans = Blockchain.waitingTransaction.get(i);
                        Blockchain.verrouUTXOs.lock();
                        block.addTransaction(trans);
                        Blockchain.verrouUTXOs.unlock();

                    }
                    Blockchain.waitingTransaction = new ArrayList<Transaction>(); //a garder quand en reseaux mais pas en localhost car peut pas vérifier la transactionlist			//iciiiii					//Blockchain.waitingTransaction=new ArrayList<Transaction>(); // I remove every transaction of the waiting list
                    
                    Blockchain.addBlock(block); //mine the block
                    //System.out.println(block.toString());
                    //broadcast the blockchain to every nodes
                    for (int i = 0; i < Blockchain.connectedList.size(); i++) {
                        try {
                            if (Blockchain.connectedList.get(i).equals("localhost")) {
                                continue;
                            }
                            InetSocketAddress adr = new InetSocketAddress(Blockchain.connectedList.get(i), 6000);
                            Socket socket = new Socket();
                            socket.connect(adr, 100);
                            ObjectOutputStream outa = new ObjectOutputStream(socket.getOutputStream());
                            outa.flush();
                            outa.writeObject(block);
                            outa.flush();
                           // outa.writeObject(Blockchain.UTXOs);
                            outa.close();
                            socket.close();
                            /*
                            InetSocketAddress adre = new InetSocketAddress(Blockchain.connectedList.get(i), 6000);
                            Socket sockete = new Socket();
                            sockete.connect(adre, 100);
                            ObjectOutputStream outae = new ObjectOutputStream(sockete.getOutputStream());
                            outae.flush();
                            outae.writeObject(Blockchain.UTXOs);
                            outae.close();
                            sockete.close();
                            */
                            
                        } catch (SocketTimeoutException ste) {
                            System.err.println("Délai de connexion expire envoie blockchain");
                        }

                    }
                    long endTime=System.nanoTime();
                    long timeElapsed= endTime-startTime;
                    System.out.println("time to add the new block and mine it in milliseconds:" + timeElapsed/1000000);
                    
                    try{
                        FileOutputStream fileOut=new FileOutputStream("tamano.txt",true);
                        ObjectOutputStream out= new ObjectOutputStream(fileOut);
                        out.writeObject(block);
                        out.close();
                        fileOut.close();
                    }catch(FileNotFoundException e){
                    }catch(IOException e){
                        
                    }
                    //if new autorizated nodes want to join the blockchain, add it to connected node
                    try {
                        Blockchain.verrouNumberWaitingNode.lock();
                        Blockchain.verrouWaitingNode.lock();

                        if (Blockchain.numberOfWaitingNode != 0) {

                            for (int i = 0; i < Blockchain.waitingNode.size(); i++) {
                                try {
                                    Blockchain.verrouConnectedList.lock();
                                    InetSocketAddress adr = new InetSocketAddress(Blockchain.waitingNode.get(i), 6000);
                                    Socket socket = new Socket();
                                    socket.connect(adr, 100);
                                    ObjectOutputStream outa = new ObjectOutputStream(socket.getOutputStream());
                                    outa.flush();
                                    outa.writeObject(Blockchain.listPerson);

                                    outa.writeObject(Blockchain.blockchain);
                                    outa.flush();

                                    outa.writeObject(Blockchain.UTXOs);

                                    Blockchain.connectedList.add(Blockchain.waitingNode.get(i));

                                    outa.close();
                                    socket.close();
                                } finally {
                                    Blockchain.verrouConnectedList.unlock();

                                }

                            }
                            //reset the variables
                            Blockchain.numberOfWaitingNode = 0;
                            Blockchain.waitingNode = new ArrayList<String>();
                        }
                    } finally {
                        Blockchain.verrouNumberWaitingNode.unlock();
                        Blockchain.verrouWaitingNode.unlock();
                    }
                    Blockchain.isChainValid2();
                   
                    System.out.println("Personne A state : " + ThreadCreateTransaction.getPersonFromId(2).getState() + "\n\n");
                    System.out.println("Personne B state : " + ThreadCreateTransaction.getPersonFromId(3).getState() + "\n\n");

                } //enf if there are transaction
            } finally {
                Blockchain.verrouNumWin.unlock();
                Blockchain.verrouBlockchain.unlock();
                Blockchain.verrouWaitingTransaction.unlock();
              
                Blockchain.blockAdded=true;
            }

        }//end if i have 100% of the vote?

    }

}
