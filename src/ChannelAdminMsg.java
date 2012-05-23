
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;

/**
 *      Channel administration message
 * 
 * Message container used for distributing channel actions such as client joining
 * or leaving.
 * 
 * @author Dejvino
 */
public class ChannelAdminMsg implements Message
{
    private static final long serialVersionUID = 1L;

    public static final String ACTION_TYPE_JOIN = "join";
    public static final String ACTION_TYPE_LEAVE = "leave";
    public static final String ACTION_PHASE_REQUEST = "request";
    public static final String ACTION_PHASE_ACCEPTED = "accepted";
    
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
     * What kind of operation it is.
     */
    private String actionType;
    
    private String actionPhase;

    /**
     * Constructor.
     */
    public ChannelAdminMsg(Id clientId, String clientName, Id channelId, String channelName,
            String actionType, String actionPhase)
    {
        this.clientId = clientId;
        this.clientName = clientName;
        this.channelId = channelId;
        this.channelName = channelName;
        this.actionType = actionType;
        this.actionPhase = actionPhase;
    }

    // =====================================================================

    public String getActionPhase()
    {
        return actionPhase;
    }

    public void setActionPhase(String actionPhase)
    {
        this.actionPhase = actionPhase;
    }
    
    public String getActionType()
    {
        return actionType;
    }

    public Id getChannelId()
    {
        return channelId;
    }

    public String getChannelName()
    {
        return channelName;
    }

    public Id getClientId()
    {
        return clientId;
    }

    public String getClientName()
    {
        return clientName;
    }

    // =====================================================================

    @Override
    public String toString()
    {
        return "ChannelAdminMsg from " + clientName + " ("+clientId+") to " + channelName + " ("+channelId+"): " + actionType + " " + actionPhase;
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
