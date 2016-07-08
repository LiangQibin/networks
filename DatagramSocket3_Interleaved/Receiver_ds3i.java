package DatagramSocket3_Interleaved;
/**
 * RECEIVER FOR DATAGRAM SOCKET 3 - INTERLEAVED
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
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.LineUnavailableException;
import uk.ac.uea.cmp.voip.DatagramSocket3;

public class Receiver_ds3i implements Runnable {
    
    static DatagramSocket3 receiving_socket3;
    static Timer timer;
    
    private ArrayList<Integer> burstlengths = new ArrayList<Integer>();
    private ArrayList<Long> delayTimes = new ArrayList<Long>();
    
    private int burstLength = 0;    //size of a burst at a single time
    private int numPacketsReceived = 0; //total number of packets received
    private int testPackets = 2000;     //total of test packets
    private int packetNo = 0;           //packet number - used for printing. 

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }
    
    public void run() {
        //Port to open socket on
        int PORT = 8000;
        try {
            receiving_socket3 = new DatagramSocket3(PORT);
        } catch (SocketException e) {
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }
        
        boolean running = true;
        AudioPlayer player = null;
        try {
            player = new AudioPlayer();

            //for repetition
            byte[] prevSound = null;

            /**
             * Variables required for interleaving
             */
            Vector<DatagramPacket> vect1 = new Vector<DatagramPacket>();
            Vector<DatagramPacket> vect2 = new Vector<DatagramPacket>();
            int d = 3; //dxd vector 
            int totalSize = 9; //size of interleaver
            int count = 0;     //count to keep track of when a whole block has been received
            boolean firstblock = true;  //keep track of when the first block is received

            /**
             * Vect 1 stores the packets in order a& plays the sound Vect 2
             * stores the block getting received.
             */
            for (int i = 0; i < totalSize; i++) {
                vect1.add(null);
                vect2.add(null);
            }
            
            int prevPlaySeqnum = 0; //stores previous packet number (used to calculate bursts)
            receiving_socket3.setSoTimeout(1000);
            
            while (running == true) {
                try {
                    //receive packet
                    byte[] buffer = new byte[524];
                    DatagramPacket packet = new DatagramPacket(buffer, 0, 524);
                    receiving_socket3.receive(packet);
                    numPacketsReceived++;

                    //get the sequence number of received packet
                    ByteBuffer receivingbb = ByteBuffer.allocate(524);
                    receivingbb.put(buffer);
                    int seqnum = receivingbb.getInt(0);

                    /**
                     * INTERLEAVING. Get the first block, store each packet in
                     * vect1 in the correct index by using the sequence number
                     * as a guide. After first block is received, play each
                     * packet in vect1 and store each incoming packet in vect2.
                     * Once a block has been received swap the vectors and
                     * continue.
                     */
                    if (firstblock == true) { //store each packet for first block
                        vect1.set(seqnum, packet);
                        count++; //increase count to determine if a whole block has been received.
                        if (count + 1 > totalSize + 1) { //if count +1 is higher than block size, reset count for the next block
                            firstblock = false;
                            count = 0;
                        }
                    } else if (firstblock == false) {
                        if (vect1.get(count) != null) {/* note. count starts at 0 to totalSize. Id vect1.get(count) is not null (i.e. packet in that space has not been lost), play it */
                            ByteBuffer playbb = ByteBuffer.allocate(524);
                            //put the 1st packet in vect 1 in ByteBuffer
                            playbb.put(vect1.get(count).getData());

                            //extract the sound & play it
                            byte[] sound = Arrays.copyOfRange(playbb.array(), 12, playbb.array().length);
                            player.playBlock(sound);
                            prevSound = sound;

                            /**
                             * Get the packetNo to calculate the burst lengths &
                             * to print to console as seq num are only 0-8
                             */
                            packetNo = (numPacketsReceived - (totalSize + 1));
                            calculateBursts(packetNo, prevPlaySeqnum);
                            prevPlaySeqnum = packetNo;

                            //print packet info
                            printPacketInformation(vect1.get(count));
                        } 
                        
                        vect1.set(count, null); //packet has been played, set that space to null for the next one
                        vect2.set(seqnum, packet);//store incoming packet for current block getting received

                        if (count + 1 == totalSize) {//if increasing count means it will be the size of block, then get the last packet & swap the vectors
                            vect1.set((totalSize - 1), null);
                            count = 0; //reset count

                            //swap vectors
                            Vector<DatagramPacket> temp = new Vector<DatagramPacket>(vect1);
                            vect1 = new Vector<DatagramPacket>(vect2);
                            vect2 = new Vector<DatagramPacket>(temp);
                        } else {
                            count++;
                        }
                    }

                    //play sound normally - no interleaving
                    // player.playBlock(sound);
                    //check if all packets wanted have been received
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

            //Print out all burst lengths
            System.out.println("BURST LENGTHS");
            for (Integer it : burstlengths) {
                System.out.println(it);
            }

            //Print out all delay times
            System.out.println("DELAY TIMES");
            for (Long dt : delayTimes) {
                System.out.println(dt);
            }
            
        } catch (LineUnavailableException ex) {
            Logger.getLogger(Receiver_ds3i.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SocketException ex) {
            Logger.getLogger(Receiver_ds3i.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Close the socket
        receiving_socket3.close();
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
