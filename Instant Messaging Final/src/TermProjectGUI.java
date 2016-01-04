import java.awt.CardLayout;
import java.awt.Color;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.io.*;
import java.math.*;
import java.net.*;
import java.util.*;


/**
 * This file is used as the client side of the application.
 * @author Lakhan Bhojwani
 * @author Ankita Sambhare
 */

/*
 *  This class represent the message format 
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
 * This class is used to listen the broadcast from the server at the client side
 */
class ListenBroadcast extends Thread
{
	byte[] receiveData = new byte[1024];
    byte[] sendData = new byte[1024];
    DatagramSocket broadcastSocket;
    public ListenBroadcast(int bp)
    {
    	
    	try
    	{
    		broadcastSocket = new DatagramSocket(bp);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
	public void run()
	{
		listen();
	}
	
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

	public void listen()
	{
		try
    	{
    		while(true)
    		{
    			receiveData = new byte[6000];
    	        sendData = new byte[1024];
    			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    			broadcastSocket.receive(receivePacket);
                MessageFormat message = (MessageFormat) deserialize(receiveData);

                if(message.messageType == 5)
                {
                	break;
                }
                System.out.println("broadcast: " + message.userList);
                for ( String key : message.userList.keySet() ) {
                	TermProjectGUI.liveUsers.put(key, message.userList.get(key));
                	TermProjectGUI.model.addElement(key);       //.add(btn);
                	
                	TermProjectGUI.liveUsersPublicKeyn.put(key, message.publicKeyn.get(key));
                    TermProjectGUI.liveUsersPublicKeye.put(key, message.publicKeye.get(key));
                	
                }
                System.out.println("live: " + TermProjectGUI.liveUsers);
                
                
                
                
    		}
    		broadcastSocket.close();
    		
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
	}
}
/*
 * This class is used to listen the messages coming from other clients at the client side
 */
class ListenUnicast extends Thread
{
	byte[] receiveData = new byte[1024];
    byte[] sendData = new byte[1024];
    DatagramSocket unicastSocket;
    public ListenUnicast(DatagramSocket soc)
    {
    	receiveData = new byte[1024];
        sendData = new byte[1024];
    	try
    	{
    		unicastSocket = soc;
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
	public void run()
	{
		listen();
	}
	
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

	public void listen()
	{
		try
    	{
    		while(true)
    		{
    			receiveData = new byte[6000];
    	        sendData = new byte[1024];
    			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    			unicastSocket.receive(receivePacket);
                MessageFormat reply = (MessageFormat) deserialize(receiveData);

                if(reply.messageType == 1)
                {
                	System.out.println(reply.name + "");
                	CardLayout card = (CardLayout) TermProjectGUI.mainPanel.getLayout();
                	TermProjectGUI.outter.setVisible(false);
                    card.show(TermProjectGUI.mainPanel, "card3");
                    
                    TermProjectGUI.model = new DefaultListModel();
                    
                    TermProjectGUI.displayUser.setModel(TermProjectGUI.model);
            		TermProjectGUI.displayUser.setFixedCellHeight(75);
            		TermProjectGUI.displayUser.setFixedCellWidth(100);
                    
                }
                else
                {
                	if(reply.messageType == 3)
                    {
                		System.out.println(reply.name + "\t" + reply.userList);
                		TermProjectGUI.liveUsers = new HashMap(reply.userList);
                	
                		TermProjectGUI.model = new DefaultListModel();
                		
                		TermProjectGUI.displayUser.setModel(TermProjectGUI.model);
                		TermProjectGUI.displayUser.setFixedCellHeight(75);
                		TermProjectGUI.displayUser.setFixedCellWidth(100);
                        
                        Iterator it = TermProjectGUI.liveUsers.entrySet().iterator();
                        while (it.hasNext()) 
                        {
                            Map.Entry pair = (Map.Entry)it.next();
                            System.out.println(pair.getKey());
                            TermProjectGUI.model.addElement(pair.getKey());       //.add(btn);
                            TermProjectGUI.userMessages.put((String) pair.getKey(), new TreeMap<String, EachMessage>());
                            TermProjectGUI.liveUsersPublicKeyn.put((String) pair.getKey(), reply.publicKeyn.get(pair.getKey()));
                            TermProjectGUI.liveUsersPublicKeye.put((String) pair.getKey(), reply.publicKeye.get(pair.getKey()));
                            
                        }
                        
                        System.out.println("check liveusers: " + TermProjectGUI.liveUsers);
                    }
                	else
                	{
                		if(reply.messageType == 5)
                        {
                			System.out.println(reply.name + " terminated");
                        	break;
                        }
                		else
                		{
                			if(reply.messageType == 7)
                			{
                				BigInteger plaintext = TermProjectGUI.decrypt(new BigInteger(reply.message));
                	    		byte[] finaltextmsgReceived = plaintext.toByteArray();
                				
//                				BigInteger plaintext = TermProjectGUI.decrypt(new BigInteger(reply.message.getBytes()));
//                				String finaltextmsgReceived = new String(plaintext.toByteArray());
                				EachMessage m1 = new EachMessage(reply.name, new String(finaltextmsgReceived));
                				TreeMap t1 = TermProjectGUI.userMessages.get(reply.name);
                				t1.put(new Timestamp(new Date().getTime()), m1);
                				System.out.println("Received Message: " + new String(finaltextmsgReceived));
                				
                				TermProjectGUI.updateDisplayMessages(t1);
//                				MessageFormat message = new MessageFormat(2, null, broadcastPort, jTextField1.getText(), null);
//                	            sendData = serialize(message);
//                	            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), 9876);
//                	            serverSocket.send(sendPacket);
                	        	
                			}
                		}
                	}
                	
                }
                
    		}
    		unicastSocket.close();
    		
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
	}
}


/*
 * This class is used to check the sender and the message coming from him.
 */

class EachMessage {

    String sender;
    String msg;

    public EachMessage(String s, String m) {
        this.sender = s;
        this.msg = m;
    }
}

/*
 * This class is used to develope the GUI of the client side.
 */
public class TermProjectGUI extends javax.swing.JFrame {

	static int broadcastPort;
	static int port;
	static String myIP;
	static String myName;
	static DatagramSocket serverSocket;
    static byte[] receiveData = new byte[1024];
    static byte[] sendData = new byte[1024];
    static HashMap<String, String> liveUsers = new HashMap<String, String>();
    static HashMap<String, BigInteger> liveUsersPublicKeyn = new HashMap<String, BigInteger>();
    static HashMap<String, BigInteger> liveUsersPublicKeye = new HashMap<String, BigInteger>();
    
    static HashMap<String, TreeMap> userMessages = new HashMap<String, TreeMap>();
    static BigInteger n, e;	// public key
    static BigInteger d;		// private key
    
    
    
//    static ArrayList<String> liveUsers = new ArrayList<>();
    static ArrayList<JLabel> messagesLable = new ArrayList<>();
//    static TreeMap<Date, EachMessage> u1 = new TreeMap<Date, EachMessage>();
    static String selectedUserValue = "";
    private final JLabel l2 = new JLabel();
    JPanel panel;

    // this method generates public key
    public void generatePublicKey()
    {
    	Random r = new Random();
		BigInteger p = BigInteger.probablePrime(512, r);
//		BigInteger p = new BigInteger("53");
		
		BigInteger q = BigInteger.probablePrime(512, r);
//		BigInteger q = new BigInteger("59");
		
    	e =  BigInteger.probablePrime(256, r);
		n = p.multiply(q);
		
		generatePrivateKey(p, q);
		
    }
    
    // this method generates private key
    public void generatePrivateKey(BigInteger p, BigInteger q)
    {
    	BigInteger one = new BigInteger("1");
		
    	BigInteger phi = p.subtract(BigInteger.ONE). multiply(q.subtract(BigInteger.ONE));
    	
    	while (phi.gcd(e).compareTo(BigInteger.ONE) > 0 && e.compareTo(phi) < 0)
    	{
    		System.out.println("here");
    		e = e.add(one).add(one);
    	}
    	
    	d = e.modInverse(phi);
    }
    
    // this method is used to encrypt the message
    public BigInteger encrypt(BigInteger data)
    {
    	BigInteger ciphertext =  data.modPow(liveUsersPublicKeye.get(selectedUserValue),liveUsersPublicKeyn.get(selectedUserValue)); 
//    			e, n);
    	System.out.println("Ciphertext: " + ciphertext);
    	
    	
    	return ciphertext;
    }
    
    // this message is used to decrypt the message
    public static BigInteger decrypt(BigInteger c)
    {

    	BigInteger plaintext = c.modPow(d, n);
    	System.out.println("Plaintext: " + plaintext);
    	return plaintext;
    }

	
   
    /**
     * Creates new form NewJFrame
     */
    public TermProjectGUI() {
     
    	initComponents();
    	
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
    	
    	
    
    	

        mainPanel = new javax.swing.JPanel();
        loginPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        userNameText = new javax.swing.JTextField();
        registerButton = new javax.swing.JButton();
        terminateButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        ListOfUsers = new javax.swing.JPanel();
        getUserListButton = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        displayUser = new javax.swing.JList<>();
        outter = new javax.swing.JPanel();
        m110 = new javax.swing.JPanel();
        m1 = new javax.swing.JLabel();
        m2 = new javax.swing.JLabel();
        m3 = new javax.swing.JLabel();
        m4 = new javax.swing.JLabel();
        m5 = new javax.swing.JLabel();
        m6 = new javax.swing.JLabel();
        m7 = new javax.swing.JLabel();
        m8 = new javax.swing.JLabel();
        m9 = new javax.swing.JLabel();
        m10 = new javax.swing.JLabel();
        m11 = new javax.swing.JLabel();
        sendMessage = new javax.swing.JPanel();
        sendMessageButton = new javax.swing.JButton();
        sendMessageText = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        mainPanel.setLayout(new java.awt.CardLayout());

        loginPanel.setBackground(new java.awt.Color(255, 255, 204));

        jLabel1.setFont(new java.awt.Font("Tahoma", 3, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 153, 51));
        jLabel1.setText("Name:");

        registerButton.setBackground(new java.awt.Color(0, 153, 51));
        registerButton.setFont(new java.awt.Font("Tahoma", 2, 14)); // NOI18N
        registerButton.setText("REGISTER");
        registerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                registerButtonActionPerformed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 153, 51));
        jLabel2.setText("Instant Messaging");

        javax.swing.GroupLayout loginPanelLayout = new javax.swing.GroupLayout(loginPanel);
        loginPanel.setLayout(loginPanelLayout);
        loginPanelLayout.setHorizontalGroup(
            loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loginPanelLayout.createSequentialGroup()
                .addGroup(loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(loginPanelLayout.createSequentialGroup()
                        .addGap(138, 138, 138)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(registerButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(userNameText, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)))
                    .addGroup(loginPanelLayout.createSequentialGroup()
                        .addGap(173, 173, 173)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 213, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(843, Short.MAX_VALUE))
        );
        loginPanelLayout.setVerticalGroup(
            loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(loginPanelLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(92, 92, 92)
                .addGroup(loginPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(userNameText, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(122, 122, 122)
                .addComponent(registerButton)
                .addContainerGap(144, Short.MAX_VALUE))
        );

        mainPanel.add(loginPanel, "card2");
        
        displayUser.addListSelectionListener(listSelectionListener);

        ListOfUsers.setBackground(new java.awt.Color(255, 255, 204));
        ListOfUsers.setAlignmentY(20.0F);
        ListOfUsers.setPreferredSize(new java.awt.Dimension(500, 500));

        getUserListButton.setBackground(new java.awt.Color(0, 153, 51));
        getUserListButton.setFont(new java.awt.Font("Tahoma", 2, 14)); // NOI18N
        getUserListButton.setText("Get Users List");
        getUserListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                getUserListButtonActionPerformed(evt);
            }
        });

        displayUser.setBackground(new java.awt.Color(255, 255, 204));
        displayUser.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 153, 51)));
        displayUser.setFont(new java.awt.Font("Tahoma", 2, 24)); // NOI18N
        displayUser.setForeground(new java.awt.Color(0, 153, 51));
        displayUser.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        displayUser.setAlignmentY(5.0F);
        displayUser.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        displayUser.setName("userDisplay"); // NOI18N
        displayUser.setSelectionBackground(new java.awt.Color(0, 153, 51));
        displayUser.setSelectionForeground(new java.awt.Color(0, 0, 0));
        displayUser.setVisibleRowCount(0);
        jScrollPane2.setViewportView(displayUser);

        outter.setBackground(new java.awt.Color(255, 255, 204));

        m110.setBackground(new java.awt.Color(230, 255, 200));

        m1.setBackground(new java.awt.Color(153, 153, 153));

        m2.setBackground(new java.awt.Color(153, 153, 153));

        m3.setBackground(new java.awt.Color(153, 153, 153));

        sendMessage.setBackground(new java.awt.Color(255, 255, 204));

        sendMessageButton.setBackground(new java.awt.Color(0, 153, 51));
        sendMessageButton.setFont(new java.awt.Font("Tahoma", 2, 14)); // NOI18N
        sendMessageButton.setText("Send");
        sendMessageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendMessageButtonActionPerformed(evt);
            }
        });

        sendMessageText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendMessageTextActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout sendMessageLayout = new javax.swing.GroupLayout(sendMessage);
        sendMessage.setLayout(sendMessageLayout);
        sendMessageLayout.setHorizontalGroup(
            sendMessageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sendMessageLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(sendMessageText, javax.swing.GroupLayout.PREFERRED_SIZE, 343, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sendMessageButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        sendMessageLayout.setVerticalGroup(
            sendMessageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sendMessageLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(sendMessageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sendMessageText, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sendMessageButton, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        
        m11.setBackground(new java.awt.Color(153, 153, 153));
        m11.setFont(new java.awt.Font("Tahoma", 3, 18)); // NOI18N
        m11.setForeground(new java.awt.Color(255, 102, 0));
        
        
        
        
        javax.swing.GroupLayout m110Layout = new javax.swing.GroupLayout(m110);
        m110.setLayout(m110Layout);
        m110Layout.setHorizontalGroup(
            m110Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(m1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(m3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(m4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(m2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(m5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(m6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(m7, javax.swing.GroupLayout.DEFAULT_SIZE, 441, Short.MAX_VALUE)
            .addComponent(m8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(m9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(m10, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(sendMessage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(m11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        m110Layout.setVerticalGroup(
            m110Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(m110Layout.createSequentialGroup()
        		.addComponent(m11, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(21, Short.MAX_VALUE)
                .addComponent(m1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m2, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m3, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m4, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m5, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m6, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m7, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m8, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m9, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(m10, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(58, 58, 58)
                .addComponent(sendMessage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout outterLayout = new javax.swing.GroupLayout(outter);
        outter.setLayout(outterLayout);
        outterLayout.setHorizontalGroup(
            outterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(m110, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        outterLayout.setVerticalGroup(
            outterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(outterLayout.createSequentialGroup()
                .addComponent(m110, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        
        terminateButton.setBackground(new java.awt.Color(0, 153, 51));
        terminateButton.setFont(new java.awt.Font("Tahoma", 2, 14)); // NOI18N
        terminateButton.setText("Terminate");
        terminateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	terminateButtonActionPerformed(evt);
            }
        });


        javax.swing.GroupLayout ListOfUsersLayout = new javax.swing.GroupLayout(ListOfUsers);
        ListOfUsers.setLayout(ListOfUsersLayout);
        ListOfUsersLayout.setHorizontalGroup(
            ListOfUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ListOfUsersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ListOfUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(getUserListButton, javax.swing.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                    .addComponent(terminateButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 398, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(72, 72, 72)
                .addComponent(outter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(148, Short.MAX_VALUE))
        );
        
        
        ListOfUsersLayout.setVerticalGroup(
                ListOfUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ListOfUsersLayout.createSequentialGroup()
                    .addGroup(ListOfUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(ListOfUsersLayout.createSequentialGroup()
                            .addGap(160, 160, 160)
                            .addComponent(getUserListButton)
                            .addGap(116, 116, 116)
                            .addComponent(terminateButton)
                            .addGap(0, 0, Short.MAX_VALUE))
                        .addGroup(ListOfUsersLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jScrollPane2))
                        .addGroup(ListOfUsersLayout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(outter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGap(105, 105, 105))
            );        mainPanel.add(ListOfUsers, "card3");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void registerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_registerButtonActionPerformed

    	myName = userNameText.getText();
    	myIP = "127.0.0.7";
    	
    	try
    	{
    		port = (int)(Math.random()*9000)+1000;
    		broadcastPort = (int)(Math.random()*9000)+1000;
//    		serverSocket = new DatagramSocket(port);
    		System.out.println("PORT: " + port);
    		System.out.println("BROADCAST PORT: " + broadcastPort);
    		
    		//generate keys
    		generatePublicKey();
    		String abc = "hello";
//    		BigInteger c = encrypt(new BigInteger(abc.getBytes()));
//    		byte[] c1 = c.toByteArray(); 
//    		BigInteger d = decrypt(new BigInteger(c1));
//    		byte[] d1 = d.toByteArray();
//    		System.out.println("Decrypted: " + new String(d1));
    		
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
        
    	System.out.print("Register: ");
        
        try
        {   
        	
        	HashMap<String, BigInteger> tn = new HashMap<String, BigInteger>();
        	tn.put(myName, n);
        	
        	HashMap<String, BigInteger> te = new HashMap<String, BigInteger>();
        	te.put(myName, e);
        	
        	serverSocket = new DatagramSocket(port);
            MessageFormat message = new MessageFormat(0, null, broadcastPort, myName, null, tn, te);
            sendData = serialize(message);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), 9876);
            serverSocket.send(sendPacket);
            
            ListenUnicast lu = new ListenUnicast(serverSocket);
            lu.start();
        	
            ListenBroadcast lb = new ListenBroadcast(broadcastPort);
            lb.start();
            
            userNameText.setText("");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        // TODO add your handling code here:
    }//GEN-LAST:event_registerButtonActionPerformed

    
    public static void updateDisplayMessages(TreeMap t){
        
    	if(t != null)
    	{
        
                        
                        Collection entrySet = t.entrySet();
                        Iterator it = entrySet.iterator();
                       
                        int count = 0;
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            EachMessage each = (EachMessage) pair.getValue();
                            JLabel lable = null;
                            if(count == 0){
                                lable = m1;
                               
                            }
                            if(count == 1){
                                lable = m2;
                               
                            }
                            if(count == 2){
                                lable = m3;
                                
                            }
                            if(count == 3){
                                lable = m4;
                                
                            }
                            if(count == 4){
                                lable = m5;
                               
                            }
                            if(count == 5){
                                lable = m6;
                               
                            }
                            if(count == 6){
                                lable = m7;
                                
                            }
                            if(count == 7){
                                lable = m8;
                                
                            }
                            if(count == 8){
                                lable = m9;
                                
                            }if(count == 9){
                                lable = m10;
                                
                            }
                            count++;
                            if (each.sender.equalsIgnoreCase(myName)) {
                               
                            	  lable.setText(each.msg);
                                  lable.setHorizontalAlignment(SwingConstants.RIGHT);
                                
                            } else {
                                
                                lable.setText(each.msg);
                                lable.setSize(40,40);
                             
                                lable.setHorizontalAlignment(SwingConstants.LEFT);
                                
                            }

                        }
    	}
    }
    
    
    
    private void getUserListButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_getUserListButtonActionPerformed

//        liveUsers.add("Lakhan", );
//        liveUsers.add("Ankita");
//        liveUsers.add("Asad");
    	
    	
    	System.out.print("Get List: ");
    	
    	try
        {
    		HashMap<String, BigInteger> tn = new HashMap<String, BigInteger>();
        	tn.put(myName, n);
        	
        	HashMap<String, BigInteger> te = new HashMap<String, BigInteger>();
        	te.put(myName, e);
        	
            MessageFormat message = new MessageFormat(2, null, broadcastPort, myName, null, tn, te);
            sendData = serialize(message);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), 9876);
            serverSocket.send(sendPacket);
            
        	
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    	
    	

        

    }//GEN-LAST:event_getUserListButtonActionPerformed

    
    ListSelectionListener listSelectionListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent listSelectionEvent) {
        	
            boolean adjust = listSelectionEvent.getValueIsAdjusting();
            if (!adjust) {
            	m1.setText("");
                m2.setText("");
                m3.setText("");
                m4.setText("");
                m5.setText("");
                m6.setText("");
                m7.setText("");
                m8.setText("");
                m9.setText("");
                m10.setText("");
                m11.setText("");

                outter.setVisible(true);
                JList list = (JList) listSelectionEvent.getSource();
                int selections[] = list.getSelectedIndices();
                Object selectionValues[] = list.getSelectedValues();
                for (int i = 0, n = selections.length; i < n; i++) {
                    if (i == 0) {
                        // System.out.print("  Selections: ");
                    }
                    //System.out.print(selectionValues[i] + " ");
                    selectedUserValue = (String) selectionValues[i];
                    m11.setText("                                    "+selectedUserValue);
                    
                    System.out.println("Public Key n: " + liveUsersPublicKeyn.get(selectedUserValue));
                    System.out.println("Public Key e: " + liveUsersPublicKeye.get(selectedUserValue));
                    
                    TreeMap t1 = userMessages.get(selectedUserValue);
                    updateDisplayMessages(t1);
                
                    

                }
            }
        }

    };
    private void sendMessageTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendMessageTextActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sendMessageTextActionPerformed

    private void sendMessageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendMessageButtonActionPerformed
        
        
        TreeMap<Date, EachMessage> u1 = userMessages.get(selectedUserValue);
        String textToSend = sendMessageText.getText();
        
        BigInteger ciphertext = encrypt(new BigInteger(textToSend.getBytes()));
		byte[] finalmsgToSend = ciphertext.toByteArray(); 
        
//        BigInteger ciphertext = encrypt(new BigInteger(textToSend.getBytes()));
        
//        String finalmsgToSend = new String(ciphertext.toByteArray());
        EachMessage msg1 = new EachMessage(myName, textToSend);
        
        u1.put(new Timestamp(new Date().getTime()), msg1);
        
    	sendMessageText.setText("");
        
    
        updateDisplayMessages(u1);
        
        try
        {
        	HashMap<String, BigInteger> tn = new HashMap<String, BigInteger>();
        	tn.put(myName, n);
        	
        	HashMap<String, BigInteger> te = new HashMap<String, BigInteger>();
        	te.put(myName, e);
        	
	    	MessageFormat message = new MessageFormat(7, null, broadcastPort, myName, finalmsgToSend, tn, te);
	        sendData = serialize(message);
	        System.out.println("selectedUserValue: " + selectedUserValue);
	        System.out.println("liveUsers: " + liveUsers);
	        String[] temp = liveUsers.get(selectedUserValue).split(":");
	        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(temp[0]), Integer.parseInt(temp[1]));
	        serverSocket.send(sendPacket);
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }
    	
    	System.out.println("MESSAGES: " + userMessages + " \t " + msg1.msg);
    	
    	
        
        
        // TODO add your handling code here:
    }//GEN-LAST:event_sendMessageButtonActionPerformed
    

    private void terminateButtonActionPerformed(java.awt.event.ActionEvent evt) {  
    	HashMap<String, BigInteger> tn = new HashMap<String, BigInteger>();
    	tn.put(myName, n);
    	
    	HashMap<String, BigInteger> te = new HashMap<String, BigInteger>();
    	te.put(myName, e);
    	
    	try
    	{
	    	MessageFormat message = new MessageFormat(4, null, broadcastPort, myName, null, tn, te);
	    	sendData = serialize(message);
	    	DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), 9876);
	    	serverSocket.send(sendPacket);
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	m1.setText("");
        m2.setText("");
        m3.setText("");
        m4.setText("");
        m5.setText("");
        m6.setText("");
        m7.setText("");
        m8.setText("");
        m9.setText("");
        m10.setText("");
        m11.setText("");
        CardLayout card = (CardLayout) mainPanel.getLayout();
            card.show(mainPanel, "card2");
            //////////reset//////////
        	
//        	serverSocket.close();
            liveUsers = new HashMap<String, String>();
            liveUsersPublicKeyn = new HashMap<String, BigInteger>();
            liveUsersPublicKeye = new HashMap<String, BigInteger>();
            
            userMessages = new HashMap<String, TreeMap>();
            

            
            
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TermProjectGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TermProjectGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TermProjectGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TermProjectGUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TermProjectGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ListOfUsers;
    static javax.swing.JList<String> displayUser;
    private javax.swing.JButton getUserListButton;
    private javax.swing.JButton terminateButton;
    
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel loginPanel;
    static javax.swing.JLabel m1;
    static javax.swing.JLabel m10;
    static javax.swing.JLabel m11;
    static javax.swing.JPanel m110;
    static javax.swing.JLabel m2;
    static javax.swing.JLabel m3;
    static javax.swing.JLabel m4;
    static javax.swing.JLabel m5;
    static javax.swing.JLabel m6;
    static javax.swing.JLabel m7;
    static javax.swing.JLabel m8;
    static javax.swing.JLabel m9;
    static javax.swing.JPanel mainPanel;
    static javax.swing.JPanel outter;
    private javax.swing.JButton registerButton;
    private javax.swing.JPanel sendMessage;
    private javax.swing.JButton sendMessageButton;
    private javax.swing.JTextField sendMessageText;
    private javax.swing.JTextField userNameText;
    
    static DefaultListModel model;
    // End of variables declaration//GEN-END:variables
    
    
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

}
