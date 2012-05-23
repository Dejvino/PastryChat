
import javax.swing.JOptionPane;

/**
 * Window app starter.
 * 
 * Prepares the Pastry environment and then starts the actual window.
 * 
 * @author Dejvino
 */
public class MainWindow extends AbstractMain
{   
    public void startWindow()
    {
        ChatFrame frame = new ChatFrame();
        chat.registerChatAppListener(frame);
        frame.start();
    }
    
    @Override
    public String obtainNickname()
    {
        String nickname = null;
        
        // nickname loading
        do {
            String input = JOptionPane.showInputDialog("Nickname?\n(must be 3+ characters)");
            
            if (input == null) {
                continue;
            }
            if (input.length() < 3) {
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
        MainWindow inst = new MainWindow();
        try {
            inst.boot(args);
            inst.startWindow();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
