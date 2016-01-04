/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.*;

/**
 * This file is used as the server side of the application.
 * @author Lakhan Bhojwani
 * @author Ankita Sambhare
 */


/*
 * This class is used to generate the message format.
 */
class MessageFormat implements Serializable
{
    int messageType;    //register = 0, registerack = 1, listOfUsersRequest = 2, listOfUsers = 3, closeConnection = 4, connectionRemoved = 5
                        // future scope: broadcast = 6
    
    HashMap<String, String> userList;
    int broadcastPort;
    String name;
    byte[] message;
    HashMap<String, BigInteger> publicKeyn;
    HashMap<String, BigInteger> publicKeye;
    
    
    public MessageFormat(int mt, HashMap l, int bp, String n, byte[] msg, HashMap pkn, HashMap pke)
    {
        this.messageType = mt;
        this.userList = l;
        this.broadcastPort = bp;
        this.name = n;
        this.message = msg;
        this.publicKeyn = new HashMap(pkn);
        this.publicKeye = new HashMap(pke);
        
    }
    
}
/*
 * An instance of this class is created when a new user is registered
 */
class User extends Thread
{
    
    public User()
    {
        
    }
    public void run()
    {
        System.out.println("thread started");
        
        ConnectUser();
    }
    
    public void ConnectUser()
    {
        
        
        try
        {
            String clientSentence;
            String capitalizedSentence;
            ServerSocket welcomeSocket = new ServerSocket(1234);
            
            while(true)
            {
                Socket connectionSocket = welcomeSocket.accept();
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

                clientSentence = inFromClient.readLine();
                System.out.println("Received: " + clientSentence);
                capitalizedSentence = clientSentence.toUpperCase() + '\n';
                outToClient.writeBytes(capitalizedSentence);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
}

public class FCNTermProject_Server{

	
	 //static variables
    static HashMap<String, String> userCredentials = new HashMap<String, String>();  
    static HashMap<String, Integer> broadcastList = new HashMap<String, Integer>();  
    
    static HashMap<String, BigInteger> publicKeyn = new HashMap<String, BigInteger>();
    static HashMap<String, BigInteger> publicKeye = new HashMap<String, BigInteger>();
    
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}
    
    public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject(obj);
		return out.toByteArray();
	}
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception
    {
        // TODO code application logic here
        
//    	portNumbers.add(1234);
//        portNumbers.add(5678);
        
//        ServerSocket welcomeSocket = new ServerSocket(6789);
        
        DatagramSocket serverSocket = new DatagramSocket(9876);
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        
        while(true)
        {
        	
        	System.out.println("userCredentials: " + userCredentials);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            MessageFormat message = (MessageFormat) deserialize(receiveData);
            System.out.println("message type: " + message.messageType);
            
            
            if(message.messageType == 0)
            {
            	InetAddress IPAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();
                
                userCredentials.put(message.name, IPAddress.getHostAddress()+":"+port);
                broadcastList.put(message.name, message.broadcastPort);

//                Iterator it = broadcastList.entrySet().iterator();
//                while (it.hasNext()) 
//                {
//                	Map.Entry pair = (Map.Entry)it.next();
////                	publicKeyn.put(message.name, pair.);
//                }
//                publicKeye.put(message.name, message.publicKeye.get(message.name));
                
                System.out.println("Public Key n: " + message.publicKeyn.get(message.name));
                System.out.println("Public Key e: " + message.publicKeye.get(message.name));
                
                publicKeyn.put(message.name, message.publicKeyn.get(message.name));
                publicKeye.put(message.name, message.publicKeye.get(message.name));
                
                MessageFormat reply = new MessageFormat(1, null, 0, "Server", null, publicKeyn, publicKeye);
                
                sendData = serialize(reply);
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);
                
                HashMap<String, String> newUser = new HashMap<String, String>();
                newUser.put(message.name, IPAddress.getHostAddress()+":"+port);
                
                
                Iterator it = broadcastList.entrySet().iterator();
                while (it.hasNext()) 
                {
                    Map.Entry pair = (Map.Entry)it.next();

                    String temp[] = userCredentials.get(message.name).split(":");
                    InetAddress ip = InetAddress.getByName(temp[0]);
                    int p = (int)pair.getValue();
                    MessageFormat broadcast = new MessageFormat(6, newUser, 0, "ServerBroadcast", null, publicKeyn, publicKeye);  
                    sendData = serialize(broadcast);
                    
                    
                    sendPacket = new DatagramPacket(sendData, sendData.length, ip, p);
                    if(!message.name.equals(pair.getKey()))
                	{
                		sendData = serialize(broadcast);
                		sendPacket = new DatagramPacket(sendData, sendData.length, ip, p);
                		serverSocket.send(sendPacket);
                	}
            
                }
            	
            }
            else
            {
            	if(message.messageType == 2)
                {
                	InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();
                    
                    HashMap<String, String> liveUsers = new HashMap<String, String>(userCredentials);
//                    liveUsers = userCredentials;
                    liveUsers.remove(message.name);
                    System.out.println("userCred: " + userCredentials);
                    System.out.println("live" + liveUsers);
                    
                    MessageFormat reply = new MessageFormat(3, liveUsers, 0, "Server", null, publicKeyn, publicKeye);
                    
                    sendData = serialize(reply);
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                    serverSocket.send(sendPacket);
                    
                	
                }
            	else
            	{
            		if(message.messageType == 4)
                    {
                    	InetAddress IPAddress = receivePacket.getAddress();
                        int port = receivePacket.getPort();
                        
//                        System.out.println("IPAddress.getHostAddress(): " + IPAddress.getHostAddress());
                        System.out.println("broadcastList: " + broadcastList);
                        int p = broadcastList.get(message.name);
                        
                        MessageFormat broadcastTerminate = new MessageFormat(5, null, 0, "Server", null, publicKeyn, publicKeye);
                        
                        sendData = serialize(broadcastTerminate);
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, p);
                        serverSocket.send(sendPacket);
                        
                        userCredentials.remove(message.name);
                        broadcastList.remove(message.name);
                        publicKeyn.remove(message.name);
                        publicKeye.remove(message.name);
                        
                        MessageFormat reply = new MessageFormat(5, null, 0, "Server", null, publicKeyn, publicKeye);
                        
                        sendData = serialize(reply);
                        sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                        serverSocket.send(sendPacket);
                        
                        

                        
                    	
                    }
            	}
            }
               
        }
    }
    
}
