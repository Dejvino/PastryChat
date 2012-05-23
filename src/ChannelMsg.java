
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;

/**
 *      Channel message
 * 
 * Message container used for transferring a message from the channel owner
 * to its subscribers.
 * 
 * @author Dejvino
 */
public class ChannelMsg implements Message
{
    private static final long serialVersionUID = 1L;

    /**
     * Where the Message came from.
     */
    private Id from;
    
    private String fromName;
    
    /**
     * Where the Message is going.
     */
    private Id to;
    
    private String toName;
    
    /**
     * Channel
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
    public ChannelMsg(Id from, String fromName, Id to, String toName, Id channelId, String channelName, String text)
    {
        this.from = from;
        this.fromName = fromName;
        this.to = to;
        this.toName = toName;
        this.channelId = channelId;
        this.channelName = channelName;
        this.text = text;
    }
    
    // =====================================================================

    public Id getFrom()
    {
        return from;
    }

    public String getFromName()
    {
        return fromName;
    }

    public String getText()
    {
        return text;
    }

    public Id getTo()
    {
        return to;
    }

    public String getToName()
    {
        return toName;
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
        return "ChannelMsg from " + fromName + " ("+from+") to " + toName + " ("+to+"): " + text;
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
