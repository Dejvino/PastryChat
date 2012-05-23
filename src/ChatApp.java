
import rice.Continuation;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.past.Past;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastImpl;
import rice.pastry.commonapi.PastryIdFactory;
import rice.persistence.LRUCache;
import rice.persistence.MemoryStorage;
import rice.persistence.Storage;
import rice.persistence.StorageManagerImpl;

/**
 *      Chat Application
 * 
 * Class carrying out the application logic, interacting with the messaging
 * middleware and the user interface.
 * 
 * This is a simple chat application capable of sending and receiving messages
 * based on "channels".
 * 
 * @author Dejvino
 */
public class ChatApp implements Application
{
    /**
     * Username
     */
    private String nickname;
    /**
     * Pastry node.
     */
    private final Node node;
    /**
     * Id factory.
     */
    private final PastryIdFactory idFactory;
    /**
     * Interface used for message exchange.
     */
    private final Endpoint endpoint;
    private final Object endpointLock = new Object();
    /**
     * Application event listener
     */
    private ChatAppListener chatAppListener;
    private final Object chatAppListenerLock = new Object();
    /**
     * Distributed storage for channel info safe-keeping
     */
    private Past pastChannels;

    public ChatApp(Node node, String nickname)
    {
        this.node = node;
        this.nickname = nickname;

        this.endpoint = node.buildEndpoint(this, "chat-endpoint");

        this.idFactory = new rice.pastry.commonapi.PastryIdFactory(node.getEnvironment());

        // channel info is stored in-memory, because it does not need to be persistent
        Storage storage = new MemoryStorage(idFactory);
        pastChannels = new PastImpl(node, new StorageManagerImpl(idFactory, storage,
                new LRUCache(new MemoryStorage(idFactory), 512 * 1024, node.getEnvironment())),
                2, "channels");
    }

    // =====================================================================
    
    public String getNickname()
    {
        return nickname;
    }

    public Id getNodeId()
    {
        return node.getId();
    }

    // =====================================================================
    
    /**
     * Registers a ChatApp event listener.
     * This also results in calling {@ChatAppListener::setChatApp()}.
     * 
     * @param listener 
     */
    public void registerChatAppListener(ChatAppListener listener)
    {
        synchronized (chatAppListenerLock) {
            this.chatAppListener = listener;
            if (listener != null) {
                listener.setChatApp(this);
            }
        }
    }

    // =====================================================================
    
    /**
     * This registers the application endpoint when everything is ready
     * (i.e. instance is fully constructed) and activates the app.
     */
    public void activate()
    {
        // now we can receive messages
        synchronized (endpointLock) {
            this.endpoint.register();
        }
    }

    // =====================================================================
    
    /**
     * Called to route a message to the id.
     * 
     * @param id Recipient ID
     * @param name Recipient name
     * @param text Message text
     */
    public void sendPrivateMsg(String name, String text)
    {
        synchronized (endpointLock) {
            Message msg = new PrivateMsg(endpoint.getId(), nickname, getClientId(name), name, text);
            endpoint.route(getClientId(name), msg, null);
        }
    }

    /**
     * Called to directly send a message to the nh.
     * 
     * @param nh Recipient handle
     * @param name Recipient name
     * @param text Message text
     */
    public void sendPrivateMsgDirect(NodeHandle nh, String name, String text)
    {
        synchronized (endpointLock) {
            Message msg = new PrivateMsg(endpoint.getId(), nickname, nh.getId(), name, text);
            endpoint.route(null, msg, nh);
        }
    }

    /**
     * Called to send a broadcast message to the channel owner.
     * 
     * @param id Channel ID
     * @param name Channel name
     * @param text Message text
     */
    public void sendChannelBroadcastMsg(String name, String text)
    {
        synchronized (endpointLock) {
            Message msg = new ChannelBroadcastMsg(endpoint.getId(), nickname, getChannelId(name), name, text);
            endpoint.route(getChannelId(name), msg, null);
        }
    }

    // =====================================================================
    
    /**
     * Sends a join request to the channel owner.
     * 
     * @param channelName 
     */
    public void sendJoinRequest(String channelName)
    {
        synchronized (endpointLock) {
            Message msg = new ChannelAdminMsg(endpoint.getId(), nickname,
                    getChannelId(channelName), channelName,
                    ChannelAdminMsg.ACTION_TYPE_JOIN, ChannelAdminMsg.ACTION_PHASE_REQUEST);
            endpoint.route(getChannelId(channelName), msg, null);
        }
    }

    /**
     * Sends a leave request to the channel owner.
     * 
     * @param channelName 
     */
    public void sendLeaveRequest(String channelName)
    {
        synchronized (endpointLock) {
            Message msg = new ChannelAdminMsg(endpoint.getId(), nickname,
                    getChannelId(channelName), channelName,
                    ChannelAdminMsg.ACTION_TYPE_LEAVE, ChannelAdminMsg.ACTION_PHASE_REQUEST);
            endpoint.route(getChannelId(channelName), msg, null);
        }
    }

    // =====================================================================
    
    /**
     * Called when we receive a message.
     */
    @Override
    public void deliver(Id id, Message message)
    {
        // Private message for me??
        if (message instanceof PrivateMsg) {
            PrivateMsg msg = (PrivateMsg) message;
            // check recipient
            if (!getNodeId().equals(msg.getTo())) {
                // wrong recipient!
                return;
            }
            synchronized (chatAppListenerLock) {
                if (chatAppListener != null) {
                    chatAppListener.onMessageDelivered(msg);
                }
            }
            return;

        }

        // Channel message for me??
        if (message instanceof ChannelMsg) {
            ChannelMsg msg = (ChannelMsg) message;
            // check recipient
            if (!getNodeId().equals(msg.getTo())) {
                // wrong recipient!
                return;
            }
            synchronized (chatAppListenerLock) {
                if (chatAppListener != null) {
                    chatAppListener.onMessageDelivered(msg);
                }
            }
            return;
        }

        // Channel broadcast for me to announce?
        if (message instanceof ChannelBroadcastMsg) {
            final ChannelBroadcastMsg broadcast = (ChannelBroadcastMsg) message;
            
            // check that the channel info is prepared and only then broadcast
            readyChannelOwnership(broadcast.getChannelName(), new ChannelOwnerRunnable() {

                @Override
                public void run()
                {
                    // ok, broadcast it to all!
                    for (String clientName : content.getClients()) {
                        Message msg = new ChannelMsg(broadcast.getClientId(),
                                broadcast.getClientName(),
                                getClientId(clientName),
                                clientName,
                                broadcast.getChannelId(),
                                broadcast.getChannelName(),
                                broadcast.getText());
                        synchronized (endpointLock) {
                            endpoint.route(getClientId(clientName), msg, null);
                        }
                    }
                }
                
            });
        }

        // Channel administration message?
        if (message instanceof ChannelAdminMsg) {
            final ChannelAdminMsg admin = (ChannelAdminMsg) message;

            // request? --> we own the channel
            if (ChannelAdminMsg.ACTION_PHASE_REQUEST.equals(admin.getActionPhase())) {
                // join / leave request
                if (ChannelAdminMsg.ACTION_TYPE_JOIN.equals(admin.getActionType())) {
                    // client wants to join, ok!
                    // check that the channel info is prepared and only then alter it
                    readyChannelOwnership(admin.getChannelName(), new ChannelOwnerRunnable() {

                        @Override
                        public void run()
                        {
                            // add to the list
                            content.getClients().add(admin.getClientName());
                            // update the content
                            pastChannels.insert(content, new Continuation<Boolean[], Exception>() {
                                
                                @Override
                                public void receiveResult(Boolean[] results) {
                                    // TODO: react to errors
//                                  int numSuccessfulStores = 0;
//                                  for (int ctr = 0; ctr < results.length; ctr++) {
//                                    if (results[ctr].booleanValue()) { 
//                                      numSuccessfulStores++;
//                                    }
//                                  }
//                                  System.out.println(content + " successfully stored at " + 
//                                      numSuccessfulStores + " locations.");
                                }

                                @Override
                                public void receiveException(Exception result) {
                                  System.err.println("Error storing "+content);
                                  result.printStackTrace();
                                }
                            });
                            // inform the client
                            admin.setActionPhase(ChannelAdminMsg.ACTION_PHASE_ACCEPTED);
                            synchronized (endpointLock) {
                                endpoint.route(getClientId(admin.getClientName()), admin, null);
                            }
                        }

                    });
                } else if (ChannelAdminMsg.ACTION_TYPE_LEAVE.equals(admin.getActionType())) {
                    // client wants to leave...ok
                    // check that the channel info is prepared and only then alter it
                    readyChannelOwnership(admin.getChannelName(), new ChannelOwnerRunnable() {

                        @Override
                        public void run()
                        {
                            // remove from the list
                            content.getClients().remove(admin.getClientName());
                            // update the content
                            pastChannels.insert(content, new Continuation<Boolean[], Exception>() {
                                
                                @Override
                                public void receiveResult(Boolean[] results) {
                                    // TODO: react to errors
//                                  int numSuccessfulStores = 0;
//                                  for (int ctr = 0; ctr < results.length; ctr++) {
//                                    if (results[ctr].booleanValue()) {
//                                      numSuccessfulStores++;
//                                    }
//                                  }
//                                  System.out.println(content + " successfully stored at " + 
//                                      numSuccessfulStores + " locations.");
                                }

                                @Override
                                public void receiveException(Exception result) {
                                  System.err.println("Error storing "+content);
                                  result.printStackTrace();
                                }
                            });
                            // inform the client
                            admin.setActionPhase(ChannelAdminMsg.ACTION_PHASE_ACCEPTED);
                            synchronized (endpointLock) {
                                endpoint.route(getClientId(admin.getClientName()), admin, null);
                            }
                        }

                    });
                } else {
                    System.err.println("Unknown admin message type: " + admin);
                }
            } else if (ChannelAdminMsg.ACTION_PHASE_ACCEPTED.equals(admin.getActionPhase())) {
                // was this our message?
                if (!nickname.equals(admin.getClientName())) {
                    // nope, discard it
                    return;
                }
                // join / leave the channel
                if (ChannelAdminMsg.ACTION_TYPE_JOIN.equals(admin.getActionType())) {
                    synchronized (chatAppListenerLock) {
                        if (chatAppListener != null) {
                            chatAppListener.onPrintln("Joined channel '" + admin.getChannelName() + "'.");
                        }
                    }
                } else if (ChannelAdminMsg.ACTION_TYPE_LEAVE.equals(admin.getActionType())) {
                    synchronized (chatAppListenerLock) {
                        if (chatAppListener != null) {
                            chatAppListener.onPrintln("Left channel '" + admin.getChannelName() + "'.");
                        }
                    }
                } else {
                    System.err.println("Unknown admin message type: " + admin);
                }
            }
            return;
        }
    }

    // =====================================================================
    
    /**
     * Called when you hear about a new neighbor.
     */
    @Override
    public void update(NodeHandle handle, boolean joined)
    {
        synchronized (chatAppListenerLock) {
            if (chatAppListener != null) {
                if (joined) {
                    chatAppListener.onNodeConnected(node);
                } else {
                    chatAppListener.onNodeDisconnected(node);
                }
            }
        }
    }

    // =====================================================================
    
    /**
     * Called when a message travels along our path.
     */
    @Override
    public boolean forward(RouteMessage message)
    {
        return true;
    }

    // =====================================================================
    
    /**
     * Returns an Id for the given client.
     * 
     * This method generates an Id using the given feed. The client does NOT
     * need to exist, no checking is performed.
     * 
     * @param clientName
     * @return 
     */
    public Id getClientId(String clientName)
    {
        return new HashNodeIdFactory(clientName).generateNodeId();
    }

    /**
     * Returns an Id for the given channel.
     * 
     * @param channelName
     * @return 
     */
    public Id getChannelId(String channelName)
    {
        return idFactory.buildId(channelName);
    }

    // =====================================================================
    
    /**
     * Interprets the given string input.
     * 
     * @param input 
     */
    public void handleCommand(String input)
    {
        if ("quit".equals(input)) {
            synchronized (chatAppListenerLock) {
                if (chatAppListener != null) {
                    chatAppListener.onQuitCommand();
                }
            }
        } else if ("help".equals(input)) {
            synchronized (chatAppListenerLock) {
                if (chatAppListener != null) {
                    chatAppListener.onPrintln("List of commands:");
                    chatAppListener.onPrintln("quit - terminates the application");
                    chatAppListener.onPrintln("join <channel> - joins the given channel");
                    chatAppListener.onPrintln("leave <channel> - leaves the given channel");
                    chatAppListener.onPrintln("send <channel> <message> - send the message to the given channel");
                    chatAppListener.onPrintln("msg <user> <message> - send the message to the given user");
                }
            }
        } else if (input.startsWith("join ")) {
            String channelName = input.substring("join ".length());
            sendJoinRequest(channelName);
        } else if (input.startsWith("leave ")) {
            String channelName = input.substring("leave ".length());
            sendLeaveRequest(channelName);
        } else if (input.startsWith("msg ")) {
            String[] parts = input.split(" ", 3);
            String clientName = parts[1];
            String msg = parts[2];
            sendPrivateMsg(clientName, msg);
        } else if (input.startsWith("send ")) {
            String[] parts = input.split(" ", 3);
            String channelName = parts[1];
            String msg = parts[2];
            sendChannelBroadcastMsg(channelName, msg);
        } else {
            synchronized (chatAppListenerLock) {
                if (chatAppListener != null) {
                    chatAppListener.onPrintln("Command unknown. Try 'help'.");
                }
            }
        }
    }

    // =====================================================================
    
    /**
     * Retrieves the channel information (list of active clients) and then
     * executes a Runnable with the fresh channel info.
     * If no such data exists, null value is used.
     * 
     * The followup code is ran asynchronously.
     * 
     * @param channelName
     * @param followup Code to be executed when the channel info is ready.
     */
    private void readyChannelOwnership(final String channelName, final ChannelOwnerRunnable followup)
    {
        final Id lookupKey = getChannelId(channelName);

        Past p = pastChannels;

        p.lookup(lookupKey, new Continuation<PastContent, Exception>()
        {
            @Override
            public void receiveResult(PastContent r)
            {
                if (!(r instanceof ChannelContent)) {
                    // not ok
                    r = null;
                }
                // not started yet?
                if (r == null) {
                    r = new ChannelContent(lookupKey, channelName);
                }
                
                // call the rest of the code
                followup.setChannelContent((ChannelContent)r);
                followup.run();
            }
            
            @Override
            public void receiveException(Exception result)
            {
                System.err.println("Error looking up key " + lookupKey);
                result.printStackTrace();
            }
        });
    }

    // =====================================================================

    /**
     *  Channel owner's runnable
     * 
     * Code-holder used for running code after the ChannelContent has been
     * prepared and is ready to be used.
     */
    private abstract class ChannelOwnerRunnable implements Runnable
    {
        protected ChannelContent content;
        
        public void setChannelContent(ChannelContent content) {
            this.content = content;
        }
    }
    
    // =====================================================================
    
    @Override
    public String toString()
    {
        return "ChatApp " + endpoint.getId();
    }
}
