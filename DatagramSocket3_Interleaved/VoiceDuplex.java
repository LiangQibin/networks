package DatagramSocket3_Interleaved;

import java.util.Scanner;

/**
 *
 * @author jimivine
 */
public class VoiceDuplex 
{
    public static void main (String[] args)
    {
        Receiver_ds3i receiver = new Receiver_ds3i();
        Sender_ds3i sender = new Sender_ds3i();
        
        receiver.start();
        sender.start();
    }
}
