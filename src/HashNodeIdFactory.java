
import rice.pastry.Id;
import rice.pastry.NodeIdFactory;


/**
 *      HashNodeIdFactory
 *
 * Generates a node Id based on a feed supplied during the construction of this
 * class. It therefore generates the same Id every single time. It is useful
 * when we need a factory with consistent output based on input data.
 * 
 * @author Dejvino
 */
public class HashNodeIdFactory implements NodeIdFactory
{
    private String feed;
    
    /**
     * Constructs a factory which generates an Id associated with the given feed.
     * Feed could be any name or identifier, for example a nickname.
     * 
     * @param feed 
     */
    public HashNodeIdFactory(String feed)
    {
        this.feed = feed;
    }

    @Override
    public Id generateNodeId()
    {
        // generate hash material based on the feed
        int[] material = new int[Id.IdBitLength >> 5];
        for (int i = 0; i < material.length; i++) {
            material[i] = feed.hashCode();
        }
        return Id.build(material);
    }
    
}
