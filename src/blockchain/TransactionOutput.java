package blockchain;
import java.io.Serializable;


import java.security.PublicKey;

public class TransactionOutput implements Serializable {
	public String id;
	public PublicKey reciepient; //also known as the new owner of these coins.
	public String trans;
	public String parentTransactionId; //the id of the transaction this output was created in
	
	//Constructor
	public TransactionOutput(PublicKey reciepient, String trans ,String parentTransactionId) {
		this.reciepient = reciepient;
		this.trans=trans;
		this.parentTransactionId = parentTransactionId;
		this.id = StringUtil.applySha256(StringUtil.getStringFromKey(reciepient)+parentTransactionId);
	}
	
	//Check if trans belongs to you
	public boolean isMine(PublicKey publicKey) {
		return (publicKey.equals(reciepient));
	}
	
}