
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;

/**
 *      Channel broadcast message
 * 
 * Message container used for transferring a message from a node to the channel
 * owner. It is then converted to a regular {@see ChannelMsg} and resent to all
 * the channel subscribers.
 * 
 * @author Dejvino
 */
public class ChannelBroadcastMsg implements Message
{
    private static final long serialVersionUID = 1L;

    /**
     * Where the Message came from.
     */
    private Id clientId;
    
    private String clientName;
    
    /**
     * Where the Message is going.
     */
    private Id channelId;
    
    private String channelName;
    
    /**
     * Message content
     */
    private String text;

    /**
     * Constructor.
     */
    public ChannelBroadcastMsg(Id clientId, String clientName, Id channelId, String channelName, String text)
    {
        this.clientId = clientId;
        this.clientName = clientName;
        this.channelId = channelId;
        this.channelName = channelName;
        this.text = text;
    }

    // =====================================================================

    public Id getClientId()
    {
        return clientId;
    }

    public String getClientName()
    {
        return clientName;
    }

    public String getText()
    {
        return text;
    }

    public Id getChannelId()
    {
        return channelId;
    }

    public String getChannelName()
    {
        return channelName;
    }

    // =====================================================================

    @Override
    public String toString()
    {
        return "ChannelBroadcastMsg from " + clientName + " ("+clientId+") to " + channelName + " ("+channelId+"): " + text;
    }

    // =====================================================================

    /**
     * Using low priority to prevent interference with overlay maintenance traffic.
     */
    @Override
    public int getPriority()
    {
        return Message.LOW_PRIORITY;
    }
}
