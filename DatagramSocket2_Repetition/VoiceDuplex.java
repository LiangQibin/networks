package DatagramSocket2_Repetition;

import java.util.Scanner;

/**
 *
 * @author jimivine
 */
public class VoiceDuplex 
{
    public static void main (String[] args)
    {
        Receiver_ds2r receiver = new Receiver_ds2r();
        Sender_ds2r sender = new Sender_ds2r();
        
        receiver.start();
        sender.start();
    }
}
