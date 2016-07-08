package DatagramSocket4_EmptyPacketsCRC;

import java.util.Scanner;

/**
 *
 * @author jimivine
 */
public class VoiceDuplex 
{
    public static void main (String[] args)
    {
        Receiver_ds4crcep receiver = new Receiver_ds4crcep();
        Sender_ds4crcep sender = new Sender_ds4crcep();
        
        receiver.start();
        sender.start();
    }
}
