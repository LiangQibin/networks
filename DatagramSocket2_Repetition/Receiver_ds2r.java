package DatagramSocket2_Repetition;

/**
 * RECEIVER FOR DATAGRAM SOCKET 2 - WITH REPETITION
 * 
 * @authors: JV & CC
 */
import CMPC3M06.AudioPlayer;

import java.net.*;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.LineUnavailableException;
import uk.ac.uea.cmp.voip.DatagramSocket2;

public class Receiver_ds2r implements Runnable {

    static DatagramSocket2 receiving_socket2;

    static Timer timer;
    private ArrayList<Integer> burstlengths = new ArrayList<Integer>();
    private ArrayList<Long> delayTimes = new ArrayList<Long>();

    private int burstLength = 0;
    private int numPacketsReceived = 0;
    private int testPackets = 2000;
    private static AudioPlayer player = null;
    private byte[] prevSound = null;
    private byte[] sound = null;

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        //Port to open socket on
        int PORT = 8000;
        try {

            receiving_socket2 = new DatagramSocket2(PORT);

        } catch (SocketException e) {
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }

        boolean running = true;

        try {
            //for repetition

            player = new AudioPlayer();
            int prevSeqnum = 0;
            while (running == true) {

                try {
                    receiving_socket2.setSoTimeout(1000);

                    //Receive a DatagramPacket (note that the string cant be more than 80 chars)
                    byte[] buffer = new byte[524];
                    DatagramPacket packet = new DatagramPacket(buffer, 0, 524);
                    receiving_socket2.receive(packet);
                    ByteBuffer b = ByteBuffer.allocate(524);
                    b.put(buffer);

                    //get packet sequence number
                    int seqnum = b.getInt(0);
                    //calculate bursts
                    calculateBursts(seqnum, prevSeqnum);
                    //save seq num as previous
                    prevSeqnum = seqnum;

                    //get sound to play
                    sound = Arrays.copyOfRange(b.array(), 12, b.array().length);

                    /**
                     * REPETITION
                     */
                    if (burstLength > 0) {
                        repeat();
                    } else {
                        player.playBlock(sound);
                    }
                    
                    prevSound = sound; //save this packets audio as the previous
                    burstLength = 0; //reset burst length for the next packet
                    
                    //print packet information
                    printPacketInformation(packet);
                    //increase number of packets received.
                    numPacketsReceived++;

                    //calculate average packet loss
                    if (numPacketsReceived == testPackets) {
                        calculatePacketEffiency();
                        running = false;
                        break;
                    }

                } catch (SocketTimeoutException e) {
                    calculatePacketEffiency();
                    running = false;
                    e.printStackTrace();
                } catch (Exception e) {
                    calculatePacketEffiency();
                    running = false;
                    e.printStackTrace();
                }
            }

            System.out.println("BURST LENGTHS");
            for (Integer it : burstlengths) {
                System.out.println(it);
            }

            System.out.println("\nDELAY TIMES");
            for (Long ln : delayTimes) {
                System.out.println(ln);
            }

        } catch (LineUnavailableException ex) {
            Logger.getLogger(Receiver_ds2r.class.getName()).log(Level.SEVERE, null, ex);
        }
        player.close();
        //Close the socket
        receiving_socket2.close();
    }

  /**
     * Calculates the bursts lengths if the packet sequence number is greater
     * than 0 only.
     *
     * @param seqnum current packet sequence number
     * @param prevSeqnum previous packet sequence number
     */
    public void calculateBursts(int seqnum, int prevSeqnum) {
        if (seqnum > 1) {
            if (prevSeqnum == 0 && (seqnum > 1)) {
                burstLength = seqnum - 1;
                burstlengths.add(burstLength);
            } else if ((seqnum - prevSeqnum > 1)) {
                burstLength = seqnum - prevSeqnum;
                burstlengths.add(burstLength);
            }
        }
    }

    /**
     * Print general Packet information
     *
     * @param packet
     */
    public void printPacketInformation(DatagramPacket packet) {
        ByteBuffer b = ByteBuffer.allocate(524);
        b.put(packet.getData());
        int sn = b.getInt(0);
        //get the time the packet was sent
        long timeSent = b.getLong(4);

        //Print packet header
        System.out.println("-- PACKET " + (sn) + " --");
        System.out.println("-Sequence Number: " + (sn));
        Timestamp ts = new Timestamp(timeSent);
        System.out.println("-Time Sent: " + ts);

        //get time packet was received
        Date date = new Date();
        long currentTime = date.getTime();
        Timestamp tr = new Timestamp(currentTime);
        System.out.println("-Time Received: " + tr);

        //get delay time
        long delay = (tr.getTime() - ts.getTime());
        Timestamp delaytime = new Timestamp(delay);
        delayTimes.add(delaytime.getTime());
        System.out.println("-Delay: " + delaytime.getTime());

    }

    /**
     * Calculates packet efficiency
     */
    public void calculatePacketEffiency() {
        int packetsLost = testPackets - numPacketsReceived;
        System.out.println("Packets received: " + numPacketsReceived);
        System.out.println("Packets Lost: " + packetsLost);
        
        double sumdelay = 0;
        for (int i = 0; i < delayTimes.size(); i++) {
            sumdelay = sumdelay + delayTimes.get(i);
        }
        System.out.println("Total Delay: " + sumdelay + " ms ");
        double avgdelay =(sumdelay / delayTimes.size());
        System.out.println("Average delay: " + avgdelay + " ms ");

        int sumBursts = 0;
        for (int i = 0; i < burstlengths.size(); i++) {
            sumBursts = sumBursts + burstlengths.get(i);
        }
        System.out.println("Total Burst Length: " + sumBursts + " ms ");
        double avgbursts = (sumBursts / burstlengths.size());
        System.out.println("Average burst lengths : " + avgbursts);

        
    }

    public void repeat() {
        try {
            if (burstLength < 4) {
                for (int i = 0; i < burstLength; i++) {
                    player.playBlock(prevSound);
                }
            } else {
                player.playBlock(prevSound);
                player.playBlock(prevSound);
                player.playBlock(prevSound);
                player.playBlock(prevSound);
            }
            player.playBlock(sound);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
