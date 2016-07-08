package DatagramSocket3_Interleaved_EmptyPackets;

/**
 * SENDER FOR DATAGRAM SOCKET 3 - INTERLEAVED ADDING EMPTY PACKET WHERE THERE
 * IS LOSS
 *
 * @authors: JV & CC
 */
import CMPC3M06.AudioRecorder;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.LineUnavailableException;
import uk.ac.uea.cmp.voip.DatagramSocket3;

public class Sender_ds3iep implements Runnable {

    static DatagramSocket3 sending_socket3;

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
            sending_socket3 = new DatagramSocket3();
        } catch (SocketException e) {
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
        }
        try {
            AudioRecorder recorder = new AudioRecorder();

            Vector<DatagramPacket> uninterleaved = new Vector<DatagramPacket>();
            Vector<DatagramPacket> interleaved = new Vector<DatagramPacket>();

            int d = 3;
            int totalSize = 9;
            int seqnum = 0;
            int interleaveCount = 0;
            int totalNumPacketsSent = 0;
            long runningTime = 0;
            int packetsSent = 0;
            //Main loop.
            boolean running = true;
            while (running == true) {
                try {
                    byte[] block = new byte[512];
                    while (running == true) {
                        Date startTime  = new Date();
                        //record sound
                        block = recorder.getBlock();

                        //get current time
                        Date date = new Date();
                        long time = date.getTime();

                        //create a byte buffer - 512 for sound, 4 bytes for seq num, 8 bytes for timestamp
                        ByteBuffer b = ByteBuffer.allocate(524);

                        b.putInt(seqnum);
                        b.putLong(time);
                        b.put(block);

                        //get the full byte array to send from ByteBuffer
                        byte[] tosend = b.array();

                        //create packet          
                        DatagramPacket packet = new DatagramPacket(tosend, tosend.length, clientIP, PORT);
                        //sending_socket3.send(packet);

                        /**
                         * --INTERLEAVING SECTION-- *
                         */
                        if (uninterleaved.size() < totalSize) {
                            //add packet to uninterleaved block;
                            uninterleaved.add(interleaveCount, packet);
                            interleaveCount++;
                            seqnum++;

                        } else if (uninterleaved.size() == totalSize) {
                            interleaveCount = 0; //reset counter for next batch

                            //do interleaving
                            interleaved = interleave(d, uninterleaved);
                            //send the packets;
                            for (int i = 0; i < totalSize; i++) {
                                sending_socket3.send(interleaved.get(i));
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
                                totalNumPacketsSent++;
                                if (totalNumPacketsSent == 2000) {
                                    running = false;
                                }
                            }

                            //reset vectors for next packet batch
                            interleaved = new Vector<DatagramPacket>();
                            uninterleaved = new Vector<DatagramPacket>();

                            //reset sequence number back to 0
                            seqnum = 0;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("ERROR: TextSender: Some random IO error occured!");
                    e.printStackTrace();
                }
            }
            //recorder.close();

            //Close the socket
            sending_socket3.close();
        } catch (LineUnavailableException ex) {
            Logger.getLogger(Sender_ds3iep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Interleaves a Vector containing Datagram Packets 90 degrees
     * anti-clockwise
     *
     * @param d size of vector
     * @param unInterleaved vector to interleave
     * @return an interleaved vector.
     */
    public Vector<DatagramPacket> interleave(int d, Vector<DatagramPacket> uninterleaved) {
        Vector<DatagramPacket> interleaved = new Vector<DatagramPacket>();
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < d; j++) {
                int index = i * d + j;
                int indexUnInterleaved = j * d + (d - 1 - i);
                interleaved.add(index, uninterleaved.get(indexUnInterleaved));
            }
        }

        return interleaved;
    }

}
