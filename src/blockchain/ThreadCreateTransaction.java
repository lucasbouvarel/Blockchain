package blockchain;

import java.security.*;

import java.util.Scanner;

import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;

public class ThreadCreateTransaction implements Runnable {

    String name;

    public ThreadCreateTransaction(String name) {
        this.name = name;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        int choix = 0;
        while (choix != 1 || choix != 2 || choix != 3) {
            System.out.println("What do you want to do?\n"
                    + "1.Create a new transaction\n"
                    + "2.Search every tranasctions of one person\n"
                    + "3.Search one transaction"
            );
            choix = Integer.parseInt(sc.nextLine());
            if (choix == 1) {
                faireTransaction(sc);
                choix = 0;
            } else if (choix == 2) {
                System.out.println("Enter the id of the person: ");
                int id = Integer.parseInt(sc.nextLine());

                System.out.println("Transactions historical:");
                for (int i = 0; i < Blockchain.blockchain.size(); i++) {
                    for (int j = 0; j < Blockchain.blockchain.get(i).transactions.size(); j++) {

                        if (getPersonFromPublicKey(Blockchain.blockchain.get(i).transactions.get(j).reciepient).equals(getPersonFromId(id))) {
                            System.out.println(Blockchain.blockchain.get(i).transactions.get(j).trans);
                        }
                    }
                }
                choix = 0;

            } else if (choix == 3) {
                System.out.println("Enter the id of the transaction : ");
                String id = sc.nextLine();
                long startTime = System.nanoTime();
                for (int i = 0; i < Blockchain.blockchain.size(); i++) {
                    for (int j = 0; j < Blockchain.blockchain.get(i).transactions.size(); j++) {
                        if (Blockchain.blockchain.get(i).transactions.get(j).transactionId.equals(id)) {
                            System.out.println("block number : " + i);
                            System.out.println(Blockchain.blockchain.get(i).transactions.get(j).trans);
                        }
                    }
                }
                long endTime = System.nanoTime();
                long timeElapsed = endTime - startTime;

                System.out.println("Time to search one transaction in milliseconds: " + timeElapsed );
                choix = 0;

            }
        }
    }

    public void faireTransaction(Scanner sc) {

        System.out.println("Please enter your id to create a Transaction");

        String str = sc.nextLine();
        int id = Integer.parseInt(str);

        Person personne = getPersonFromId(id);

        System.out.println("What is your transaction?");

        str = sc.nextLine();

        Blockchain.verrouTorneo.lock();
        long startTime = System.nanoTime();
        for (int j = 0; j < 10; j++) {
            String t = Integer.toString(j);

            Transaction newTrans = personne.makeTransaction(personne.publicKey, str);

            newTrans.calulateHash();
            System.out.println("transaction id "+newTrans.transactionId);
            try {
                Blockchain.verrouWaitingTransaction.lock();
                Blockchain.waitingTransaction.add(newTrans);
                for (int i = 0; i < Blockchain.connectedList.size(); i++) {
                    if (Blockchain.connectedList.get(i).equals("localhost")) {
                        continue;
                    }
                    InetSocketAddress adr = new InetSocketAddress(Blockchain.connectedList.get(i), 6000);
                    Socket socket = new Socket();
                    try {
                        socket.connect(adr, 100);
                        //System.out.println("Socket client: " + socket);

                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        out.flush();

                        out.writeObject(newTrans);
                        out.flush();

                        out.close();
                        socket.close();
                    } catch (SocketTimeoutException ste) {
                        System.err.println("Délai de connexion expire create transaction");
                    }

                }
            } catch (UnknownHostException e) {
                System.out.println("Problemesssss");
            } catch (IOException a) {
                System.out.println("exception ThrecreateTr");
            } finally {
                Blockchain.verrouWaitingTransaction.unlock();

            }
        }
        Blockchain.verrouTorneo.unlock();
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;

        System.out.println("Time to send the transactions in milliseconds: " + timeElapsed / 1000000);

    }

    public static Person getPersonFromId(int id) {
        Person retour;
        Person currentPerson;
        for (int i = 0; i < Blockchain.listPerson.size(); i++) {
            currentPerson = Blockchain.listPerson.get(i);
            if (currentPerson.id == id) {
                retour = currentPerson;
                return retour;
            }

        }

        return null;
    }

    public static Person getPersonFromPublicKey(PublicKey key) {
        Person retour;
        Person currentPerson;
        for (int i = 0; i < Blockchain.listPerson.size(); i++) {
            currentPerson = Blockchain.listPerson.get(i);
            if (currentPerson.publicKey.equals(key)) {
                retour = currentPerson;
                return retour;
            }

        }

        return null;
    }
}
