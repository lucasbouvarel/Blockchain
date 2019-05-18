package blockchain;

import java.security.*;
import java.util.ArrayList;
import java.util.Map;
import java.security.spec.ECGenParameterSpec;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.io.Serializable;

public class Person implements Serializable {

    public PrivateKey privateKey;
    public PublicKey publicKey;
    public static int sequence = 0;
    public int id;
    public HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>(); //only UTXOs owned by this wallet.

    public Person() {
        generateKeyPair();
        sequence++;
        this.id = sequence;
    }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(ecSpec, random);   //256 bytes provides an acceptable security level
            KeyPair keyPair = keyGen.generateKeyPair();
            // Set the public and private keys from the keyPair
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
	 * returns the state of the person and stores in the UTXO's personal list the last state of the person
	 * in the blockchain.utxo is stored every last transaction output, it means every new state of every person 
	 * through the blockchain.utxo i check every output to get the ones which belong to the person
	 * when i find an output which belongs to the person i add it to the output personal list (utxo personal list)
     */
    public String getState() {

        String trans = "";
        try {
            Blockchain.verrouUTXOs.lock();
            for (Map.Entry<String, TransactionOutput> item : Blockchain.UTXOs.entrySet()) {

                TransactionOutput UTXO = item.getValue();

                if (UTXO.isMine(publicKey)) { //if output belongs to me ( if trans belongs to me )

                    UTXOs.put(UTXO.id, UTXO); //add it to our list of transactions.

                    trans = UTXO.trans;

                }
            }
        } finally {
            Blockchain.verrouUTXOs.unlock();
        }

        return trans;
    }

    /*
		 * Generates and returns a new transaction from this person.
		 * Control the state of the person to see if there is a difference between the new transaction and the current state
		 * In getstate() we also add to the UTXO personal list, the new 
		 * get the input of the person thanks to the UTXOs personal list the last state of the person 
		 * Delete the input that are used
     */
    public Transaction makeTransaction(PublicKey _recipient, String trans) {
        String a = getState(); //gather the output from blockchain.utxo et add them to the personal list of output and get the actual state of the person
        if (a.equals(trans)) { //gather Transaction and the state of the person.
            System.out.println("#No change between transaction and the state. Transaction Discarded.");
            return null;
        }

//create array list of inputs
        ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>();

        for (Map.Entry<String, TransactionOutput> item : UTXOs.entrySet()) { //from the UTXOs personal list
            TransactionOutput UTXO = item.getValue();
            inputs.add(new TransactionInput(UTXO.id)); // add the link to the output 

            //if(newtrans == trans) break;
        }

        Transaction newTransaction = new Transaction(publicKey, _recipient, trans, inputs);
        newTransaction.generateSignature(privateKey);

        for (TransactionInput input : inputs) {
            UTXOs.remove(input.transactionOutputId);
        }
        return newTransaction;
    }

}
