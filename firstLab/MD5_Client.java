import java.io.*;
import java.net.*;
import java.lang.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;   
import sun.misc.*;

public class Client
{
	private Socket ClientSock;
	private BufferedReader din=null;
	private BufferedWriter dout=null;
	private String Name;
	
	public Client()
	{
		try
		{
			ClientSock = new Socket("csg21-01.ucc.ie",3001);		// open a new Socket
			din = 	new BufferedReader(								// create buffers
					new InputStreamReader(ClientSock.getInputStream()));
									
			dout = 	new BufferedWriter(
					new OutputStreamWriter(ClientSock.getOutputStream()));
			setName(null);											// initialise the name
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}		
	}
	
	public void close()
	{
		try
		{
			din.close();			// Close the input buffer
			dout.close();			// close the output buffer
			ClientSock.close();		// close the socket
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}		
	}
	
	
	public void setName(String username)
	{
		Name = username;			// set the name
	}
	
	public String getName()
	{
		return(Name);			// get the name
	}	
	
	public String readLine()		// read a line in from the Buffered reader in the socket
	{
		String tmp = null;
		try
		{
			tmp = din.readLine();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}
		return(tmp);
	}
	
	public boolean readStatus()		// check if information is waiting on the input buffer
	{
		boolean tmp;
		try
		{
			tmp = din.ready();
		}
		catch(IOException ioe)
		{
			tmp = false;
			ioe.printStackTrace();
			System.exit(1);
		}
		return(tmp);
	}
	
	public void writeLine(String mesg)	// write a string out to the server
	{
		try
		{
			dout.write(mesg,0,mesg.length());
			dout.newLine();
			dout.flush();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public Key readInKey(String Name)	// read in a secret key
	{
		File clientSecKey = new File(Name + ".sec");
		if(clientSecKey.exists())					// There is a Secret Key file...
		{
			try
			{
				ObjectInputStream oin = new ObjectInputStream( 
										new FileInputStream(clientSecKey));
				Key key = (Key)oin.readObject();	// read in the key
				oin.close();						// close the file
				return(key);						// return the key
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
				e.printStackTrace();
				return(null);
			}
		}
		else
			return(null);
	}

	public String decryptChallenge(String Challenge, Key clientkey)
	{
		try
		{
			String decrypted;
			byte[] decode;
			byte[] decrypt;

			Challenge = Challenge.replace("\n", "").replace("\r", "");
			byte[] iv = new byte[8];
			Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
			IvParameterSpec spec = new IvParameterSpec(iv);

			cipher.init(Cipher.DECRYPT_MODE, clientkey, spec);
			decode = Base64.getDecoder().decode(Challenge);
			decrypt = cipher.doFinal(decode);
			decrypted = new String(decrypt, "UTF-8");
			
			return(decrypted);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return(null);
		}
	}

	public static void main(String[] args) throws IOException 
	{
		try
		{
			Client thisClient 	= new Client();			// create a new Client object
			thisClient.setName("jom3");				// set the client name - should be YOUR username
			thisClient.writeLine(thisClient.getName());	// write out name to server
			System.out.println("Client Started...");
				
			String challenge = thisClient.readLine();	// read in encrypted fortune
			while(thisClient.readStatus())				// until all lines are read in
			{
				challenge = challenge +"\n";			// append new line character
				challenge = challenge + thisClient.readLine();						// append the next line
			}
		
			Key clientkey = thisClient.readInKey(thisClient.getName());				// read in YOUR secret Key
			String decrypted = thisClient.decryptChallenge(challenge, clientkey);	// decrypt mesg		
			StringTokenizer stk = new StringTokenizer(decrypted, "::");				// tokenize mesg
			String nonce = null;						// initialize the nonce object
			MessageDigest md = null;

			if(stk.countTokens() == 3)					// if it is correctly formatted
			{
				nonce = stk.nextToken();				// set the nonce
				String fortune = stk.nextToken();		// set the fortune
				String Status = stk.nextToken();		// set the status

				try{
					md = MessageDigest.getInstance("MD5");
				}catch(Exception e){
					System.out.println(e);
					System.exit(1);
				}

				String message = nonce + "::" + fortune;
				byte[] messageByte = message.getBytes("UTF-8");
				byte[] result = md.digest(messageByte);

				BASE64Encoder encoder = new BASE64Encoder();
				String baseMessage = encoder.encode(result);

				System.out.println(baseMessage);
				
				if(Status.compareTo("OK") == 0)			// check the status
					System.out.println("The Computer believes this Message is : UNCORRUPTED");
				else
					System.out.println("The Computer believes this Message is : CORRUPTED");
			}
			else if(stk.countTokens() == 2)				// maybe a '::' has been corrupted???
			{
				System.out.println("The Computer believes this Message is : CORRUPTED");
				nonce = stk.nextToken();				// set the nonce anyway.
			}
			else										// message is too corrupted to handle.
			{
				System.out.println("Error receiving message, exiting...");
				System.exit(1);
			}
			
			System.out.println("Message Received : " + decrypted);	// print out decrypted Message
			String resp = null;							// initialize the resp string
			
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); // read in from Console
			System.out.print("Was it Corrupted (YES/NO/MAYBE) ?: ");
			resp = br.readLine();						// Check Answer
			br.close();									// Stop reading from console
			
			if(resp != null)							// no answer
			{					
				String response = nonce+"::"+resp.toUpperCase(); // make sure its in uppercase as expected
				thisClient.writeLine(response);			// write out to the server
				System.out.println("Server Response: " + thisClient.readLine() + "\n\n"); // check response
			}
			thisClient.close();							// close buffers and the Socket.
		}
		catch(IOException ioe)
		{
			System.out.println(ioe.getMessage());
			ioe.printStackTrace();
		}	
		
	}
}

