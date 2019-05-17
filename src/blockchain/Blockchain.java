package blockchain;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.security.Security;

import java.util.HashMap;
import java.util.LinkedHashMap;
import org.bouncycastle.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Blockchain {

    public static ArrayList<Block> blockchain = new ArrayList<Block>();
    public static LinkedHashMap<String, TransactionOutput> UTXOs = new LinkedHashMap<String, TransactionOutput>(); //list of all the state.
    public static int difficulty = 1;
    public static ArrayList<String> connectedList = new ArrayList<String>();
    public static Person mairie;
    public static Person personneA;
    public static Person personneB;
    public static boolean cont = false;
    public static ArrayList<Person> listPerson = new ArrayList<Person>();
    public static ArrayList<Transaction> waitingTransaction = new ArrayList<Transaction>();
    public static ArrayList<String> ipAddress = new ArrayList<String>();
    public static HashMap<String, Double> randomArray = new HashMap<String, Double>();
    public static int numWin = 0;
    public static int numberOfWaitingNode = 0;
    public static ArrayList<String> waitingNode = new ArrayList<String>();
    public static Lock verrouCont = new ReentrantLock();
    public static Lock verrouBlockchain = new ReentrantLock();
    public static Lock verrouUTXOs = new ReentrantLock();
    public static Lock verrouUTXOs2 = new ReentrantLock();
    public static Lock verrouWaitingTransaction = new ReentrantLock();
    public static Lock verrourandomArray = new ReentrantLock();
    public static Lock verrouNumWin = new ReentrantLock();
    public static Lock verrouConnectedList = new ReentrantLock();
    public static Lock verrouNumberWaitingNode = new ReentrantLock();
    public static Lock verrouWaitingNode = new ReentrantLock();
    public static Lock verrouTorneo = new ReentrantLock();
    public static boolean blockAdded=true;

    // initialiser une liste d'ip addresse valide
    //tant que 0 connected on lance pas les threads
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        System.out.println(args[0]);
        System.out.println(args[1]);

        if (args[0].equals("0")) {
            System.out.println("Opening Blockchain");

            for (int i = 1; i < args.length; i++) {
                ipAddress.add(args[i]);

            }

            //Setup Bouncey castle as a Security Provider
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            //create genesis transaction, which make personA married: 
            mairie = new Person();
            personneA = new Person();

            personneB = new Person();

            genesisTransaction = new Transaction(mairie.publicKey, personneA.publicKey, "Maried", null);
            genesisTransaction.generateSignature(mairie.privateKey);	 //manually sign the genesis transaction	
            genesisTransaction.transactionId = "0"; //manually set the transactionid, the Hash
            genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.trans, genesisTransaction.transactionId)); //manually add the Transactions Output
            UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //its important to store our first transaction in the UTXOs list.
            listPerson.add(mairie);
            listPerson.add(personneA);
            listPerson.add(personneB);

            System.out.println("Creating and Mining Genesis block... ");
            Block genesis = new Block("0");
            genesis.addTransaction(genesisTransaction);
            addBlock(genesis);
            try {
                ServerSocket s = new ServerSocket(6000);

                while (connectedList.size() == 0) {

                    boolean conteur = false;
                    while (conteur == false) {
                        System.out.println("Waiting connected nodes");
                        Socket soc = s.accept();
                        ObjectInputStream in = new ObjectInputStream(soc.getInputStream());
                        //ObjectOutputStream outa = new ObjectOutputStream(soc.getOutputStream());
                        //outa.flush()
                        Object objet;
                        objet = in.readObject();

                        if (objet instanceof String) {
                            String node = (String) objet;
                            if (node.equals("Connected")) {
                                conteur = true;
                                //get the ip address of the node which wants to join
                                SocketAddress remote = soc.getRemoteSocketAddress();
                                s.close();
                                soc.close();
                                String ipAddress = remote.toString();
                                int deuxPoints = ipAddress.indexOf(":");
                                ipAddress = ipAddress.substring(1, deuxPoints);

                                try {
                                    Blockchain.verrouConnectedList.lock();

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
                                        connectedList.add(ipAddress);

                                        InetSocketAddress adr = new InetSocketAddress(ipAddress, 6000);
                                        Socket socket = new Socket();
                                        socket.connect(adr, 100);
                                        ObjectOutputStream outa = new ObjectOutputStream(socket.getOutputStream());
                                        outa.flush();

                                        outa.writeObject(blockchain);
                                        outa.writeObject(listPerson);
                                        outa.writeObject(UTXOs);

                                        outa.close();
                                        soc.close();
                                        socket.close();

                                    }

                                } finally {
                                    Blockchain.verrouConnectedList.unlock();
                                }
                            }
                        }
                    }
                }
                s.close();
            } catch (IOException a) {
                a.printStackTrace();
                //System.out.println("io exception addtrans");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {

            }
            connectedList.add("localhost");
            Thread t = new Thread(new ThreadBlockchain("Thread2"));
            t.start();
            Thread t2 = new Thread(new ThreadAddTransaction("Thread"));
            t2.start();
            Thread t1 = new Thread(new ThreadCreateTransaction("Create"));
            t1.start();
        } else {
            System.out.println("Joining the Blockchain");

            for (int i = 1; i < args.length; i++) {
                ipAddress.add(args[i]);
            }
            connectedList.add("localhost");

            //Setup Bouncey castle as a Security Provider
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            Thread connectedList = new Thread(new ThreadConnectedList());
            connectedList.start();
            Thread waitingBlockchain = new Thread(new ThreadWaitingBlockchain());
            waitingBlockchain.start();

            boolean man = false;
            while (man == false) {
                verrouCont.lock();
                if (cont == true) {
                    man = true;
                }
                verrouCont.unlock();
            }

            Thread t2 = new Thread(new ThreadAddTransaction("Thread"));
            t2.start();
            Thread t1 = new Thread(new ThreadCreateTransaction("Create"));
            t1.start();

        }

    }

    public static void addBlock(Block newBlock) {
        
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }


    public static Boolean isChainValid2() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String, TransactionOutput> tempUTXOs = new HashMap<String, TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(blockchain.get(0).transactions.get(0).outputs.get(0).id, blockchain.get(0).transactions.get(0).outputs.get(0));

        //loop through blockchain to check hashes:
        for (int i = 1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i - 1);
            //compare registered hash and calculated hash:
            if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
                System.out.println("current block hash : "+currentBlock.hash);
                System.out.println("calculated hash : "+currentBlock.calculateHash());
               // System.out.println("\n \n tostring : \n"+currentBlock.toString());
                System.out.println("#Current Hashes not equal");
                return false;
            }

            //compare previous hash and registered previous hash
            if (!previousBlock.hash.equals(currentBlock.previousHash)) {
                System.out.println("previousBlock hash: " + previousBlock.hash);
                System.out.println("Current Block previous hash: " + currentBlock.previousHash);
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if (!currentBlock.hash.substring(0, difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }

        }
        System.out.println("Blockchain is valid");
        return true;
    }

}
