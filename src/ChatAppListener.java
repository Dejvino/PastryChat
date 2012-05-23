
import rice.p2p.commonapi.Node;


/**
 *      ChatAppListener
 * 
 * Interface based on EventListener pattern. The implementing class is notified
 * every time something happens in the associated ChatApp.
 * 
 * This interface should be implemented by some kind of a user interface class.
 *
 * All of these methods are called synchronously, i.e. they are protected by a
 * designated lock maintained by the ChatApp.
 * 
 * @author Dejvino
 */
public interface ChatAppListener
{
    /**
     * Used for ChatApp instance injection.
     * 
     * @param app 
     */
    public void setChatApp(ChatApp app);
    
    // =====================================================================

    /**
     * Node connected to the ring.
     * 
     * This method represents only this node's view of the world.
     * 
     * @param node 
     */
    public void onNodeConnected(Node node);
    
    /**
     * Node disconnected from the ring.
     * 
     * This method represents only this node's view of the world.
     * 
     * @param node 
     */
    public void onNodeDisconnected(Node node);
 
    // =====================================================================

    /**
     * New private message for this node has arrived.
     * 
     * @param msg 
     */
    public void onMessageDelivered(PrivateMsg msg);
    
    /**
     * New channel message for this node has arrived.
     * 
     * @param msg 
     */
    public void onMessageDelivered(ChannelMsg msg);
    
    // =====================================================================

    /**
     * Called when {@see ChatApp::handleCommand()} evaluates the user command
     * as "quit" instruction.
     */
    public void onQuitCommand();
    
    /**
     * Called when the {@see ChatApp} wants to say something to the user.
     * These are usually system messages, such as "channel joined", "wrong
     * command entered", etc.
     * 
     * @param text 
     */
    public void onPrintln(String text);
}
