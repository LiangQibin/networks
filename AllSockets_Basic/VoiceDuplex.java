package AllSockets_Basic;

import java.util.Scanner;

/**
 *
 * @author jimivine
 */
public class VoiceDuplex 
{
    public static void main (String[] args)
    {
        
        Scanner reader = new Scanner(System.in); 
        System.out.println("Enter Datagram Socket Number (1-4): ");
        int socketNum = reader.nextInt(); 
        
        Receiver receiver = new Receiver(socketNum);
        Sender sender = new Sender(socketNum);
        
        receiver.start();
        sender.start();
    }
}
