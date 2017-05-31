import Shared.Messages.*;
import akka.actor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ChatWindow {

    ActorSystem system;
    ActorRef clientUserActor;

    public JFrame frame;
    public JTextField textFieldInput;
    public JPanel mainPanel;
    public JTextArea textAreaOutput;
    public JList listUsersInChannelJList;
    public JList listChannelsJList;

    DefaultListModel<String> channelListModel = new DefaultListModel();
    DefaultListModel<String> usersInChannelListModel = new DefaultListModel();

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
            channelListModel.clear();
            GetUserListInChannelMessage ulChMsg = new GetUserListInChannelMessage();
            ulChMsg.channelName = e.toString();
            clientUserActor.tell(ulChMsg, null);
        }
    };

    String userName;

    public ChatWindow() {
        initialize();
        textFieldInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                {
                    String text = textFieldInput.getText();
                    if (text.startsWith("/login")) {
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

        listChannelsJList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        //listChannelsJList = new JList(channelListModel);
        listChannelsJList.addListSelectionListener(slChannels);

        //listUsersInChannelJList = new JList(usersInChannelListModel);

        channelListModel.addElement("hellop");
    }

    private void login(String[] loginText) {
        this.userName = loginText[1];

        Config configWithPort = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + loginText[2]).withFallback(ConfigFactory.load());
        ConfigFactory.invalidateCaches();

        system = ActorSystem.create("IRCClient", configWithPort);
        clientUserActor = system.actorOf(Props.create(ClientUserActor.class, userName, this), "ClientUserActor");
        //system.actorOf(Props.create(Main.Terminator.class, a), "terminator");
    }
}
