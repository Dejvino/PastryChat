import java.io.IOException;
import java.net.*;

import java.util.Arrays;
import java.util.Collections;
import rice.environment.Environment;
import rice.pastry.*;
import rice.pastry.socket.SocketPastryNodeFactory;

/**
 * Abstract app starter.
 * Contains the Pastry booting code.
 * 
 * @author Dejvino
 */
public abstract class AbstractMain
{
    /**
     * Pastry node
     */
    protected PastryNode node;
    
    /**
     * Chat application layer
     */
    protected ChatApp chat;
            
    // =====================================================================

    /**
     * Prepares the Pastry infrastructure and boots the chat application.
     * 
     * @param args
     * @throws Exception 
     */
    protected void boot(String[] args) throws Exception
    {
        if (args.length <= 0 || args.length > 3) {
            printUsage();
            throw new IllegalArgumentException("Invalid input count.");
        }
        
        // ask for a nickname
        String nickname = obtainNickname();
        
        // Loads pastry settings
        Environment env = new Environment();

        // disable the UPnP setting (in case you are testing this on a NATted LAN)
        //env.getParameters().setString("nat_search_policy", "never");

        // the port to use locally
        int bindport = Integer.parseInt(args[0]);

        // === BOOT NODE ===
        InetSocketAddress[] bootAddrs = null;
        // OPTION 1: command line
        // build the bootaddress from the command line args
        if (args.length >= 3) {
            InetAddress bootaddr = InetAddress.getByName(args[1]);
            int bootport = Integer.parseInt(args[2]);
            bootAddrs = new InetSocketAddress[] { new InetSocketAddress(bootaddr, bootport) };
        } else {
        // OPTION 2: rendez-vous
        // use a centralized boot-discovery service
            // TODO ...
            //bootAddrs = requestBootNodeAddresses();
        }

        // launch our node!

        // Generate the NodeId based on a given nickname
        NodeIdFactory nidFactory = new HashNodeIdFactory(nickname);

        // construct the PastryNodeFactory
        PastryNodeFactory factory = new SocketPastryNodeFactory(nidFactory, bindport, env);
        //PastryNodeFactory factory = new InternetPastryNodeFactory(nidFactory, bindport, env);

        // construct a node, but this does not cause it to boot
        node = factory.newNode();

        // construct a new ChatApp
        chat = new ChatApp(node, nickname);
        chat.activate();
        
        // boot the node
        node.boot(bootAddrs == null ? Collections.emptyList() : Arrays.asList(bootAddrs));

        // the node may require sending several messages to fully boot into the ring
        synchronized (node) {
            while (!node.isReady() && !node.joinFailed()) {
                // delay so we don't busy-wait
                node.wait(500);

                // abort if can't join
                if (node.joinFailed()) {
                    throw new IOException("Could not join the FreePastry ring.  Reason:" + node.joinFailedReason());
                }
            }
        }

        System.out.println("Finished creating new node " + node);

        // publish this node as the new boot
        // TODO ...
        //publishBootNodeAddress(...);
        
    }

    // =====================================================================

    /**
     * Performs a shutdown of the previously started system.
     */
    protected void shutdown()
    {
        // TODO: any real shutting-down
        
        node.destroy();
            
        // force terminate (to kill all the deamons)
        System.exit(0);
    }
    
    // =====================================================================

    /**
     * Called to ask the user for a nickname.
     * The result is used for the node id factory.
     * 
     * @return Nickname
     */
    public abstract String obtainNickname();
    
    // =====================================================================

    /**
     * Prints a short info about how to run this.
     */
    public void printUsage()
    {
        System.out.println("Usage:");
        System.out.println("java [-cp FreePastry-<version>.jar] Main localbindport [bootIP bootPort]");
        System.out.println("example java Main 9001 example.net 9001");
    }
}
