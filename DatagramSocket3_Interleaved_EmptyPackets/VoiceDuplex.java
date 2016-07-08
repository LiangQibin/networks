package DatagramSocket3_Interleaved_EmptyPackets;

import java.util.Scanner;

/**
 *
 * @author jimivine
 */
public class VoiceDuplex 
{
    public static void main (String[] args)
    {
        Receiver_ds3iep receiver = new Receiver_ds3iep();
        Sender_ds3iep sender = new Sender_ds3iep();
        
        receiver.start();
        sender.start();
    }
}
