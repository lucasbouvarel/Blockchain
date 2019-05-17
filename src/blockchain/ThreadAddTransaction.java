package blockchain;
import java.security.Security;
import java.security.*;
import java.util.HashMap;
import org.bouncycastle.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.net.*;
import java.io.*;

public class ThreadAddTransaction implements Runnable{
	String name;

	public ThreadAddTransaction(String name){
	    this.name=name;

	  }

	  
	public void run(){
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		try {
			ServerSocket s = new ServerSocket(6000);
                        
                        boolean istrue=true;
                        
			while (istrue) {
							
				Socket soc = s.accept();
				Thread t = new Thread(new ThreadServer(soc));
				t.start();
			}
                        s.close();
                        
		}catch (IOException a) {
                    
		    //System.out.println("ThreadAddTransaction")
		}
			
	}
}
				

				
			  
		  
	  	
	

