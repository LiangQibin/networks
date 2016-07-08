package DatagramSocket4_RepetitionCRC;

/**
 * SENDER FOR DATAGRAM SOCKET 4 - CRC WITH ADDING REPETITION WHERE THERE 
 * IS CORRUPTION
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
import java.util.zip.CRC32;
import javax.sound.sampled.LineUnavailableException;

import uk.ac.uea.cmp.voip.DatagramSocket4;

public class Sender_ds4crcr implements Runnable {

    static DatagramSocket4 sending_socket4;

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        //Port to send to
        int PORT = 8000;
        //IP ADDRESS to send to
        InetAddress clientIP = null;
        try {
            clientIP = InetAddress.getByName("localhost");
        } catch (UnknownHostException e) {
            System.out.println("ERROR: TextSender: Could not find client IP");
            e.printStackTrace();
            System.exit(0);
        }

        try {

            sending_socket4 = new DatagramSocket4();

        } catch (SocketException e) {
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
        }
        try {
            AudioRecorder recorder = new AudioRecorder();
            int seqnum = 0;
            long runningTime = 0;
            int packetsSent = 0;
            //Main loop.
            
            boolean running = true;
            while (running == true) {
                try {
                    byte[] block = new byte[512];
                    while (running == true) {
                        Date startTime = new Date();
                        //record sound
                        block = recorder.getBlock();

                        //get current time
                        Date date = new Date();
                        long time = date.getTime();

                        //create a byte buffer - 512 for sound, 4 bytes for seq num, 8 bytes for timestamp
                        ByteBuffer b = ByteBuffer.allocate(532);

                        //calculate checksum for audio block
                        CRC32 checksum = new CRC32();
                        checksum.update(block, 0, block.length);
                        //convert checksum to long to attach to header
                        long checksumVal = checksum.getValue();

                        b.putInt(seqnum);
                        b.putLong(time);
                        b.putLong(checksumVal);

                        b.put(block);

                        //get the full byte array to send from ByteBuffer
                        byte[] tosend = b.array();

                        //create packet          
                        DatagramPacket packet = new DatagramPacket(tosend, tosend.length, clientIP, PORT);
                        sending_socket4.send(packet);
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
                        seqnum++;
                        if (seqnum == 2000) {
                            running = false;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("ERROR: TextSender: Some random IO error occured!");
                    e.printStackTrace();
                }
            }
            //recorder.close();

            //Close the socket
            sending_socket4.close();
        } catch (LineUnavailableException ex) {
            Logger.getLogger(Sender_ds4crcr.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
