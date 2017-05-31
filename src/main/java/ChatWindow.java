import Shared.Messages.*;
import akka.actor.*;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;
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
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;


public class ChatWindow extends Application {

    ActorSystem system;
    ActorRef clientUserActor;
    String userName;
    String currentChannel;

    private Stage stage;
    private Scene loginScene, chatScene;
    private Button connectButton, sendButton;
    private Label nameLabel, serverLabel, portLabel;
    private TextField nameInput, serverInput, portInput, chatInput;
    private TextArea chatBox;
    private ChoiceBox<ChannelScene> channelList;
    private ListView<String> userList;

    public static void main(String[] args) {
        launch(args);
//        EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                try {
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        paintLoginWindow();
    }

    private void paintLoginWindow() {
        // Login Window
        stage.setTitle("Login");
        GridPane loginGrid = new GridPane();
        loginGrid.setPadding(new Insets(10, 10, 10, 10));
        loginGrid.setVgap(8);
        loginGrid.setHgap(10);

        // User Name Label
        nameLabel = new Label("UserName:");
        GridPane.setConstraints(nameLabel, 0, 0);

        // User Name TextField
        nameInput = new TextField("John_Doe");
        GridPane.setConstraints(nameInput, 1, 0);

        // Server Label
        serverLabel = new Label("Server Name/Ip:");
        GridPane.setConstraints(serverLabel, 0, 1);

        // Server TextField
        serverInput = new TextField("10.0.0.127");
        GridPane.setConstraints(serverInput, 1, 1);

        // Port Label
        portLabel = new Label("Port:");
        GridPane.setConstraints(portLabel, 0, 2);

        // Port TextField
        portInput = new TextField("2555");
        GridPane.setConstraints(portInput, 1, 2);

        // Connect Button
        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> login(nameInput.getText(), serverInput.getText(), portInput.getText()));
        GridPane.setConstraints(connectButton, 1, 3);

        // Set Scene and paint Stage
        loginGrid.getChildren().addAll(nameLabel, nameInput, serverLabel, serverInput, portLabel, portInput, connectButton);
        loginScene = new Scene(loginGrid, 300, 200);
        stage.setScene(loginScene);
        stage.show();
    }

    private void paintChatWindow() {
        stage.setTitle("UserName: " + this.userName);
        GridPane chatGrid = new GridPane();
        chatGrid.setPadding(new Insets(10, 10, 10, 10));
        chatGrid.setVgap(8);
        chatGrid.setHgap(10);

        // Chat Box
        chatBox = new TextArea();
        GridPane.setConstraints(chatBox, 0, 0);

        // Channels ChoiceBox
        channelList = new ChoiceBox<>();
        channelList.setOnAction(e -> {
            if(currentChannel != null) {
                ChannelScene channelScene = channelList.getValue();
                //channelList.getItems().indexOf();
                channelList.getItems().forEach(cs -> {
                    if (currentChannel.equals(cs.channel)) {
                        cs.userList = this.userList.getItems();
                        cs.chatBox = this.chatBox.getText();
                    }
                });
                currentChannel = channelScene.channel;
                this.userList.getItems().clear();
                this.userList.getItems().addAll(channelScene.userList);
                this.chatBox.clear();
                this.chatBox.appendText(channelScene.chatBox);
            }
        });
        GridPane.setConstraints(channelList, 1, 0);

        // Users ListView
        userList = new ListView<>();
        GridPane.setConstraints(userList, 1, 1);

        // Chat Input TextField
        chatInput = new TextField();
        chatInput.setPromptText("Type a message here");
        chatInput.setOnAction(e -> {
            sendInput(chatInput.getText());
            chatInput.clear();
        });
        GridPane.setConstraints(chatInput, 0, 1);

        sendButton = new Button();
        sendButton.setOnAction(e -> sendInput(chatInput.getText()));

        // Set Scene and paint Stage
        chatGrid.getChildren().addAll(chatBox, channelList, userList, chatInput);
        chatScene = new Scene(chatGrid, 800, 600);
        stage.setScene(chatScene);
        stage.show();
    }

    private void sendInput(String text) {
        clientUserActor.tell(new GUIMessage(text), null);
        System.out.println("$ [" + LocalTime.now().toString() + "] Sent:" + text);
    }

    protected void printText(String text) {
        String message = "[" + LocalTime.now().toString() + "] " + text + "\n";
        Platform.runLater(() -> this.chatBox.appendText(message));
    }

    protected void printText(String channel, String text) {
        String message = "[" + LocalTime.now().toString() + "] " + text + "\n";
        if(channel == null ){
            Platform.runLater(() -> this.chatBox.appendText(message));
        } else {
            Platform.runLater(() -> channelList.getItems().forEach(channelScene -> {
                if (channel.equals(channelScene.channel)) {
                    channelScene.chatBox += text;
                }
            }));
        }
    }

    protected  void invalidSyntax(String text){
        printText("Invalid Syntax: " + text);
    }

    public void setTitle(String title) {
        Platform.runLater(() -> this.stage.setTitle(title));
    }

    public void setUserList(String channel, List<String> userList) {
        Platform.runLater(() ->{
            if(currentChannel.equals(channel)){
                this.userList.getItems().clear();
                this.userList.getItems().addAll(userList);
            } else {
                channelList.getItems().forEach(channelScene->{
                    if(channel.equals(channelScene.channel)){
                        channelScene.userList = userList;
                    }
                });
            }
        });
    }

    public void addUser(String channel, String userName){
        Platform.runLater(() ->{
            if(currentChannel.equals(channel)){
                this.userList.getItems().add(userName);
            } else {
                channelList.getItems().forEach(channelScene->{
                    if(channel.equals(channelScene.channel)){
                        channelScene.userList.add(userName);
                    }
                });
            }
        });
    }

    public void removeUser(String channel, String userName){
        Platform.runLater(() ->{
            if(currentChannel.equals(channel)){
                Platform.runLater(() -> this.userList.getItems().remove(userName));
            } else {
                channelList.getItems().forEach(channelScene->{
                    if(channel.equals(channelScene.channel)){
                        channelScene.userList.remove(userName);
                    }
                });
            }
        });
    }

    public void addChannel(String channel, List<String> userList){
        currentChannel = channel;
        Platform.runLater(() ->{
            this.channelList.getItems().add(new ChannelScene(channel,"", userList));
        });
    }

    private void login(String name, String server, String port) {// TODO: add server add Error support
        this.userName = name;
        Config configWithPort = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load());
        ConfigFactory.invalidateCaches();
        ///Config actualConfig = configWithPort.withFallback(ConfigFactory.load());

        //system = ActorSystem.create("IRCClient");

        system = ActorSystem.create("IRCClient", configWithPort);
        clientUserActor = system.actorOf(Props.create(ClientUserActor.class, userName, this), "ClientUserActor");
        //system.actorOf(Props.create(Main.Terminator.class, a), "terminator");
        paintChatWindow();
    }
}
