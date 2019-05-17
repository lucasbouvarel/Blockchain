package blockchain;

import java.security.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Transaction implements Serializable {

    //The private key is used to sign the data and the public key can be used to verify its integrity.
    public String transactionId; // this is also the hash of the transaction.
    public PublicKey sender; // senders address/public key.
    public PublicKey reciepient; // Recipients address/public key.
    public String trans; // the transaction
    public byte[] signature;
    public long timeStamp;


    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    public static int sequence = 0; // a rough count of how many transactions have been generated.

    // Constructor:
    public Transaction(PublicKey from, PublicKey to, String trans, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.reciepient = to;
        this.trans = trans;
        this.inputs = inputs;
        this.timeStamp=new Date().getTime();
        sequence++;
    }

    // This Calculates the transaction hash (which will be used as its Id)
    public String calulateHash() {
        //increase the sequence to avoid 2 identical transactions having the same hash
        transactionId = StringUtil.applySha256(
                StringUtil.getStringFromKey(sender)
                + StringUtil.getStringFromKey(reciepient)
                + trans+Long.toString(timeStamp)// + sequence
        );
        return transactionId;
    }

    //Signs all the data we dont wish to be tampered with.
    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + trans;
        signature = StringUtil.applyECDSASig(privateKey, data);
    }
    //Verifies the data we signed hasnt been tampered with

    public boolean verifiySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + trans;
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    /*
	 * 1 check if the one who make the transaction have the right to do it
	 * 2 From the ArrayList of transaction Input,gather the transaction output of the last transactions thanks to the Id
	 * 3 generate the transaction ouput and so change the state of the person 
     */
    public boolean processTransaction() {

        //ThreadCreateTransaction.getPersonFromPublicKey(sender).getState();
        if (verifiySignature() == false) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        //récupère toutes les transactions output grâce a la réference, l'état, pour le moment qu'un seul output car un seul changement par bloc
        for (TransactionInput i : inputs) {

            i.UTXO = Blockchain.UTXOs.get(i.transactionOutputId);
        }

        //generate transaction outputs:
        transactionId = calulateHash();
        outputs.add(new TransactionOutput(this.reciepient, this.trans, transactionId)); //change value estado of reciepient

        try {
            Blockchain.verrouUTXOs.lock();
//            Iterator<TransactionOutput> iter=Blockchain.UTXOs.iterator();
            ArrayList<String> list=new ArrayList<String>();
            
            for (Map.Entry<String, TransactionOutput> item : Blockchain.UTXOs.entrySet()) {
                
                TransactionOutput UTXO = item.getValue();
                //System.out.println("UTXO PUBLIC KEY :"+UTXO.reciepient);
                if (UTXO.isMine(this.reciepient)) { //if output belongs to me ( if trans belongs to me )

                    //System.out.println("remove "+Blockchain.UTXOs.get(UTXO.id).trans);
                    list.add(UTXO.id);
                    //Blockchain.UTXOs.remove(UTXO.id); //add it to our list of transactions.

                }

            }
            
            for(int i=0;i<list.size();i++){
                Blockchain.UTXOs.remove(list.get(i)); //add it to our list of transactions.
            }
            //add outputs to Unspent list
            for (TransactionOutput o : outputs) {
                Blockchain.UTXOs.put(o.id, o);

            }

            for (TransactionInput i : inputs) {
                if (i.UTXO == null) {

                    continue; //if Transaction can't be found skip it 
                }
                Blockchain.UTXOs.remove(i.UTXO.id);
            }
        } finally {
            Blockchain.verrouUTXOs.unlock();
        }
        //je supprime chaque ancienne transaction et à chaque fois et je rajoute els nouvelles
        //remove transaction inputs from UTXO lists as spent:

        return true;
    }

    public boolean processTransaction2() {
        //ThreadCreateTransaction.getPersonFromPublicKey(sender).getState();
        if (verifiySignature() == false) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        //récupère toutes les transactions output grâce a la réference, l'état, pour le moment qu'un seul output car un seul changement par bloc
        for (TransactionInput i : inputs) {

            i.UTXO = Blockchain.UTXOs.get(i.transactionOutputId);
        }

        //generate transaction outputs:
        transactionId = calulateHash();
        //outputs.add(new TransactionOutput( this.reciepient, this.trans,transactionId)); //change value estado of reciepient

        try {
            Blockchain.verrouUTXOs.lock();
            ArrayList<String> list=new ArrayList<String>();
            
            for (Map.Entry<String, TransactionOutput> item : Blockchain.UTXOs.entrySet()) {
                
                TransactionOutput UTXO = item.getValue();
                //System.out.println("UTXO PUBLIC KEY :"+UTXO.reciepient);
                if (UTXO.isMine(this.reciepient)) { //if output belongs to me ( if trans belongs to me )

                    //System.out.println("remove "+Blockchain.UTXOs.get(UTXO.id).trans);
                    list.add(UTXO.id);
                    //Blockchain.UTXOs.remove(UTXO.id); //add it to our list of transactions.

                }

            }
            
            for(int i=0;i<list.size();i++){
                Blockchain.UTXOs.remove(list.get(i)); //add it to our list of transactions.
            }
            //add outputs to Unspent list
            for (TransactionOutput o : outputs) {
                Blockchain.UTXOs.put(o.id, o);

            }

            for (TransactionInput i : inputs) {
                if (i.UTXO == null) {

                    continue; //if Transaction can't be found skip it 
                }
                Blockchain.UTXOs.remove(i.UTXO.id);
            }
        } finally {
            Blockchain.verrouUTXOs.unlock();
        }
        //je supprime chaque ancienne transaction et à chaque fois et je rajoute els nouvelles
        //remove transaction inputs from UTXO lists as spent:

        return true;
    }

}
