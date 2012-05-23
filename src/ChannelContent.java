
import java.util.HashSet;
import java.util.Set;
import rice.p2p.commonapi.Id;
import rice.p2p.past.ContentHashPastContent;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastException;

/**
 *      ChannelContent
 *
 * Channel information container used for the Past system. This is the value
 * associated with the channel name.
 * The channel owner is responsible for keeping the content of this object
 * up-to-date and distributed along the ring.
 * 
 * @author Dejvino
 */
public class ChannelContent extends ContentHashPastContent
{

    private static final long serialVersionUID = 1L;
    
    /**
     * Channel name.
     */
    private String name;
    
    /**
     * List of clients.
     */
    private Set<String> clients = new HashSet<String>();

    /**
     * Constructor. 
     */
    public ChannelContent(Id id, String name)
    {
        super(id);
        this.name = name;
    }
    
    // =====================================================================
    
    public String getName()
    {
        return name;
    }

    public Set<String> getClients()
    {
        return clients;
    }

    // =====================================================================

    @Override
    public PastContent checkInsert(Id id, PastContent existingContent) throws PastException
    {
        // allow overwrite
        return this;
    }

    @Override
    public boolean isMutable()
    {
        // yes, the content CAN and WILL change
        return true;
    }
    
    // =====================================================================

    @Override
    public String toString()
    {
        return "ChannelContent[" + name + "]";
    }
}
