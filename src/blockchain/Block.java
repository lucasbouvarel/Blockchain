package blockchain;

import java.util.Date;
import java.io.Serializable;

import java.util.ArrayList;

public class Block implements Serializable {

    public long timeStamp;
    public ArrayList<Transaction> transactions = new ArrayList<Transaction>(); //our data will be a simple message.
    public String hash;
    public String previousHash;
    public int nonce;
    public String merkleRoot;

    public Block(String prevHash) {
        this.timeStamp = new Date().getTime();
        this.previousHash = prevHash;
        this.hash = "1";
        this.merkleRoot = StringUtil.getMerkleRoot(transactions);
    }

    public String toString() {
        return "TimeStamp : " + this.timeStamp
                + /*"\nTransactions : "+ this.transactions +*/ "\nPrevious Hash : " + this.previousHash
                + "\nHash : " + this.hash
                + "\n Nonce " + this.nonce
                + "\n merkleRoot : " + this.merkleRoot
                + "\n";
    }

    //METHODS
    //Calculate new hash based on blocks contents
    public String calculateHash() {
        /*System.out.println("\nCalcul du hash du block\n");
        System.out.println("previous hash "+previousHash);
        System.out.println("timeStamp"+Long.toString(timeStamp));
        System.out.println("nonce "+Integer.toString(nonce));
        System.out.println("merkle root "+merkleRoot);*/
        String calculatedhash = StringUtil.applySha256(
                previousHash
                + Long.toString(timeStamp)
                + Integer.toString(nonce)
                + merkleRoot
        );
        this.hash = calculatedhash;
        return hash;
    }

//Increases nonce value until hash target is reached.
    public void mineBlock(int difficulty) {
        merkleRoot = StringUtil.getMerkleRoot(transactions);
        String target = StringUtil.getDificultyString(difficulty); //Create a string with difficulty * "0" 
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + hash);
    }

//Add transactions to this block
    public boolean addTransaction(Transaction transaction) {
        //process transaction and check if valid, unless block is genesis block then ignore.
        try {

            if (transaction == null) {
                return false;
            }
            if ((previousHash != "0")) {
                if ((transaction.processTransaction() != true)) {
                    System.out.println("Transaction failed to process. Discarded.");
                    return false;
                }
            }
            transactions.add(transaction);

            return true;
        } finally {

        }

    }

}
