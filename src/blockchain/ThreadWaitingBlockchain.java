/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blockchain;
//h
import static blockchain.Blockchain.UTXOs;
import static blockchain.Blockchain.blockchain;
import static blockchain.Blockchain.listPerson;
import static blockchain.Blockchain.verrouBlockchain;
import static blockchain.Blockchain.verrouUTXOs;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 *
 * @author labredes
 */
public class ThreadWaitingBlockchain implements Runnable{

    public void run(){
        try {
                ServerSocket s = new ServerSocket(6000);


                System.out.println("Waiting the blockchain, the person list and utxo");

                Socket soc = s.accept();
                ObjectInputStream in = new ObjectInputStream(soc.getInputStream());


                
                int cont3=0;
                while(Blockchain.cont == false){
                    
                    Object objet;
                    ArrayList myArrayList;
                    
                    objet = in.readObject();
                   
                    if (objet instanceof ArrayList){
                        
                        try{
                            verrouBlockchain.lock();
                            
                            myArrayList=(ArrayList) objet;
                            if(myArrayList.get(0) instanceof Block){
                                blockchain=(ArrayList<Block>) myArrayList;
                                cont3++;
                                
                            }else if(myArrayList.get(0) instanceof Person){
                                listPerson=(ArrayList<Person>) myArrayList;
                                cont3++;
                                                           }
                            if(cont3==3){
                                Blockchain.verrouCont.lock();
                                Blockchain.cont=true;
                                Blockchain.verrouCont.unlock();
                                
                            }
                            
                        }finally{
                            verrouBlockchain.unlock();
                        }
                    }else if(objet instanceof HashMap){
                        try{
                            verrouUTXOs.lock();
                            UTXOs=(LinkedHashMap<String,TransactionOutput>) objet;
                            cont3++;
                            if(cont3++==3){
                                Blockchain.verrouCont.lock();
                                Blockchain.cont=true;
                                Blockchain.verrouCont.unlock();
                            }
                            
                        }finally{
                            verrouUTXOs.unlock();
 
                        }
                    }else{
                        System.out.println("pas compris lobjet");
                    }
                }
                in.close();
                soc.close();
                s.close();
            }catch (IOException a) {
                a.printStackTrace();
                System.out.println("class not found");
            } catch (ClassNotFoundException e) {
                System.out.println("Erreur dans la reception");
                e.printStackTrace();          
            }
    }
}
