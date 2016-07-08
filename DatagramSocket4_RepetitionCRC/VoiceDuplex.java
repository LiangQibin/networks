package DatagramSocket4_RepetitionCRC;

import java.util.Scanner;

/**
 *
 * @author jimivine
 */
public class VoiceDuplex 
{
    public static void main (String[] args)
    {
        Receiver_ds4crcr receiver = new Receiver_ds4crcr();
        Sender_ds4crcr sender = new Sender_ds4crcr();
        
        receiver.start();
        sender.start();
    }
}
