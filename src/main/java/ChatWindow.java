import Shared.Messages.*;
import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ChatWindow {

    ActorSystem system;
    ActorRef clientUserActor;

    public JFrame frame;
    public JTextField textFieldInput;
    public JTextPane textPaneChannelList;
    public JPanel mainPanel;
    public JTextArea textAreaOutput;

    public static void main(String[] args) {


        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ChatWindow chatWindow = new ChatWindow();
                    chatWindow.frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
//
//        JFrame frame = new JFrame("MyForm");
//        frame.setContentPane(new ChatWindow().mainPanel);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.pack();
//        frame.setVisible(true);

    }

    String userName;

    public ChatWindow() {
        initialize();
        textFieldInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    String text = textFieldInput.getText();
                    if (text.startsWith("/login") || text.startsWith("/l")) {
                        String[] loginText = text.split(" ");
                        login(loginText);
                    } else {
                        GUIMessage msg = new GUIMessage();
                        msg.text = text;
                        clientUserActor.tell(msg, null);
                        textAreaOutput.append(text + "\n");
                    }
                    textFieldInput.setText("");
                }
                //ActorRef ;
                super.keyPressed(e);
            }
        });
    }

    private void initialize() {
        frame = new JFrame();
        frame.setBounds(100, 100, 730, 489);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);
        frame.setContentPane(this.mainPanel);

        DefaultCaret caret = (DefaultCaret) textAreaOutput.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    private void login(String[] loginText) {
        this.userName = loginText[1];

        Config configWithPort = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + loginText[2]).withFallback(ConfigFactory.load());
        ConfigFactory.invalidateCaches();
        ///Config actualConfig = configWithPort.withFallback(ConfigFactory.load());

        //system = ActorSystem.create("IRCClient");

        system = ActorSystem.create("IRCClient", configWithPort);
        clientUserActor = system.actorOf(Props.create(ClientUserActor.class, userName, this), "ClientUserActor");
        //system.actorOf(Props.create(Main.Terminator.class, a), "terminator");
    }
}
