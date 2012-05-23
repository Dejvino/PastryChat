import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.logging.Level;
import java.util.logging.Logger;
import rice.p2p.commonapi.Node;

/**
 * Console app starter.
 * 
 * @author Dejvino
 */
public class MainConsole extends AbstractMain implements ChatAppListener
{   
    private boolean end = false;
    
    public synchronized void setEnd(boolean end)
    {
        this.end = end;
    }
    
    private synchronized boolean isEnd()
    {
        return end;
    }
    
    // =====================================================================
    
    public void loop()
    {
        chat.registerChatAppListener(this);
        
        System.out.println();
        System.out.println(" === Pastry Chat === ");
        System.out.println();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = null;
        setEnd(false);
        
        System.out.println("Hello "+chat.getNickname()+"!");
        
        // controlling loop
        do {
            input = null;
            try {
                System.out.print("\n#>");
                input = reader.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ChatApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (input == null) {
                continue;
            }
            
            chat.handleCommand(input);
        } while (!isEnd());
        System.out.println("Exiting...");
    }
    
    // =====================================================================
    
    @Override
    public String obtainNickname()
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = null;
        
        String nickname = null;
        
        // nickname loading
        do {
            try {
                System.out.print("\nNickname?>");
                input = reader.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ChatApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (input == null) {
                continue;
            }
            
            if (input.length() < 3) {
                System.out.println("Nickname must be at least 3 characters long.");
                continue;
            }
            
            nickname = input;
        } while (nickname == null);
        
        return nickname;
    }
    
    // =====================================================================
    // =====================================================================

    public static void main(String[] args)
    {
        MainConsole inst = new MainConsole();
        try {
            inst.boot(args);
            inst.loop();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            inst.shutdown();
        }
    }

    // =====================================================================
    // =====================================================================

    @Override
    public void setChatApp(ChatApp app)
    {
    }

    @Override
    public void onNodeConnected(Node node)
    {
    }

    @Override
    public void onNodeDisconnected(Node node)
    {
    }

    @Override
    public void onMessageDelivered(PrivateMsg msg)
    {
        System.out.println(msg.getFromName() + " --> " + msg.getToName() + ": " + msg.getText());
    }
    
    @Override
    public void onMessageDelivered(ChannelMsg msg)
    {
        System.out.println(msg.getFromName() + " @ " + msg.getToName() + ": " + msg.getText());
    }

    @Override
    public void onQuitCommand()
    {
        setEnd(true);
    }

    @Override
    public void onPrintln(String text)
    {
        System.out.println(text);
    }
}
