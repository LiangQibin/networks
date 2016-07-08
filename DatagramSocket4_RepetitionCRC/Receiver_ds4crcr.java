package DatagramSocket4_RepetitionCRC;

/**
 * RECEIVER FOR DATAGRAM SOCKET 4 - CRC WITH ADDING REPETITION WHERE THERE 
 * IS CORRUPTION
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
import java.util.zip.CRC32;
import javax.sound.sampled.LineUnavailableException;
import uk.ac.uea.cmp.voip.DatagramSocket4;

public class Receiver_ds4crcr implements Runnable {

    static DatagramSocket4 receiving_socket4;
    static Timer timer;
    private ArrayList<Integer> burstlengths = new ArrayList<Integer>();
    private ArrayList<Long> delayTimes = new ArrayList<Long>();

    private int burstLength = 0;
    private int numPacketsReceived = 0;
    private int testPackets = 2000;

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {
        //Port to open socket on
        int PORT = 8000;
        try {

            receiving_socket4 = new DatagramSocket4(PORT);

        } catch (SocketException e) {
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }

        boolean running = true;
        AudioPlayer player = null;
        try {
            player = new AudioPlayer();
            int prevSeqnum = 0;
            byte[] prevSound = new byte[512];
            while (running == true) {
                try {
                    receiving_socket4.setSoTimeout(1000);

                    //Receive a DatagramPacket (note that the string cant be more than 80 chars)
                    byte[] buffer = new byte[532];
                    DatagramPacket packet = new DatagramPacket(buffer, 0, 532);
                    receiving_socket4.receive(packet);
                    ByteBuffer b = ByteBuffer.allocate(532);
                    b.put(buffer);

                    //get packet sequence number
                    int seqnum = b.getInt(0);
                    //calculate bursts
                    calculateBursts(seqnum, prevSeqnum);


                    //get sound to play
                    byte[] sound = Arrays.copyOfRange(b.array(), 20, b.array().length);

                    /**
                     * CRC Calculations
                     * Play previous packet audio if packet is corrupt.
                     */
                    //calculate checksum for received audio
                    CRC32 recChecksumVal = new CRC32();
                    recChecksumVal.update(sound, 0, sound.length);
                    //get checksum sent in packet header from sender
                    long checksumValSent = b.getLong(12);
                    if (recChecksumVal.getValue() != checksumValSent) {
                        System.out.println("-- PACKET " + (prevSeqnum) + " IS CORRUPT --");
                        player.playBlock(prevSound);
                    } else {
                        //print packet information
                        printPacketInformation(packet);
                        player.playBlock(sound);
                        //save packet audio as previous audio
                        prevSound = sound;
                    }
                    
                    
                    //save seq num as previous
                    prevSeqnum = seqnum;
                    
                    //increase number of packets received.
                    numPacketsReceived++;

                    //calculate average packet loss
                    if (numPacketsReceived == testPackets) {
                        calculatePacketEffiency();
                        running = false;

                        break;
                    }

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
            Logger.getLogger(Receiver_ds4crcr.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Close the socket
        receiving_socket4.close();
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

}
