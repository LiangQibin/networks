package AllSockets_Basic
;
/**
 * SENDER FOR ALL SOCKETS - BASIC
 *
 * @authors: JV & CC
 */
import CMPC3M06.AudioRecorder;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.LineUnavailableException;
import uk.ac.uea.cmp.voip.DatagramSocket2;
import uk.ac.uea.cmp.voip.DatagramSocket3;
import uk.ac.uea.cmp.voip.DatagramSocket4;

public class Sender implements Runnable
{    
    static DatagramSocket sending_socket;
    static DatagramSocket2 sending_socket2;
    static DatagramSocket3 sending_socket3;
    static DatagramSocket4 sending_socket4;
    
    static int socketNum;
    
    public Sender(int sn){
        socketNum = sn;
    } 
    
    public void start()
    {
        Thread thread = new Thread(this);
	thread.start();
    }
    
    public void run ()
    {
        //Port to send to
        int PORT = 8000;
        //IP ADDRESS to send to
        InetAddress clientIP = null;
	try 
        {
                clientIP = InetAddress.getByName("localhost");
	} 
        catch (UnknownHostException e) 
        {
            System.out.println("ERROR: TextSender: Could not find client IP");
            e.printStackTrace();
            System.exit(0);
	}

        try
        {
               switch(socketNum){
                   case(1):
                       sending_socket = new DatagramSocket();
                       break;
                   case(2):
                       sending_socket = new DatagramSocket2();
                       break;                       
                   case(3):
                       sending_socket = new DatagramSocket3();
                        break;
                   case(4):
                       sending_socket = new DatagramSocket4();
                       break;
               }
	} 
        catch (SocketException e)
        {
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
	}
        try
        {
            AudioRecorder recorder = new AudioRecorder();
            int seqnum = 0;        
            //Main loop.
            boolean running = true;
            long runningTime = 0;
            int packetsSent = 0;
            while (running == true)
            {
                try
                {
                    byte[] block  = new byte[512];                   
                    while(running ==  true)
                    {
                        Date startTime = new Date();        
                        //record sound
                        block = recorder.getBlock();
                        
                        //get current time
                        Date date= new Date();
                        long time = date.getTime();
                        
                        //create a byte buffer - 512 for sound, 4 bytes for seq num, 8 bytes for timestamp
                        ByteBuffer b = ByteBuffer.allocate(524);
                        b.putInt(seqnum);
                        b.putLong(time);
                        b.put(block);  
                        
                        //get the full byte array to send from ByteBuffer
                        byte [] tosend = b.array();                
                        
                        //create packet          
                        DatagramPacket packet = new DatagramPacket(tosend, tosend.length, clientIP, PORT);  
                        sending_socket.send(packet);
                        //Gets running time
                        Date endTime = new Date();
                        runningTime += endTime.getTime() - startTime.getTime();
                        packetsSent += 1;
                        if(runningTime >= 1000)
                        {
                            System.out.println("=========================");
                            System.out.println("BitRate = " + (packetsSent * tosend.length * 8));
                            System.out.println("=========================");
                            runningTime = 0;
                            packetsSent = 0;
                        }
                        seqnum ++;
                        
                        //2000 for testing
                        if(seqnum == 2000){
                            running = false;
                        }
                    }                   
                } 
                catch (IOException e)
                {
                    System.out.println("ERROR: TextSender: Some random IO error occured!");
                    e.printStackTrace();
                }
            }
            //stop player
            recorder.close();
            
            //Close the socket
            sending_socket.close();
        } 
        catch (LineUnavailableException ex) 
        {
            Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    

} 


