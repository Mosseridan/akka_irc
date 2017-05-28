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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ChatWindow {

    ActorSystem system;
    ActorRef clientUserActor;

    public JFrame frame;
    public JTextField textFieldInput;
    public JPanel mainPanel;
    public JTextArea textAreaOutput;
    public JList listUsersInChannel;
    public JList listChannels;

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

    ListSelectionListener slChannels = new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            usersInChannel.clear();
            GetUserListInChannelMessage ulChMsg = new GetUserListInChannelMessage();
            ulChMsg.channelName = e.toString();
            clientUserActor.tell(ulChMsg, null);
        }
    };

    String userName;
    java.util.List<String> channels;
    java.util.List<String> usersInChannel;

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

        channels = new LinkedList<>();
        listChannels = new JList(channels.toArray());
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
