import Shared.Messages.*;
import akka.actor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalTime;
import java.util.List;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.jboss.netty.channel.ChannelException;


public class ChatWindow extends Application{

    ActorSystem system;
    ActorRef clientUserActor;
    String userName;
    String currentChannelName;
    private Stage stage;
    private Scene loginScene, chatScene;
    private Button connectButton, sendButton;
    private Label nameLabel, serverLabel, portLabel;
    private TextField nameInput, serverInput, portInput, chatInput;
    private TextArea chatBox;
    private ChoiceBox<String> channelList;
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
        loginScene = new Scene(loginGrid,300,200);
        stage.setScene(loginScene);
        stage.show();
    }

    private void paintChatWindow() {
        stage.setTitle("UserName: " + this.userName);
        stage.setOnCloseRequest(e -> doExit());
        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(20, 20, 20, 20));

        // Chat Box
        chatBox = new TextArea();
        borderPane.setCenter(chatBox);

        // Users ListView
        userList = new ListView<>();
        borderPane.setRight(userList);

        // Chat Input TextField
        chatInput = new TextField();
        chatInput.setPromptText("Type a message here");
        chatInput.setOnAction(e -> {
            sendInput(chatInput.getText());
            chatInput.clear();
        });

        sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendInput(chatInput.getText()));


        // Channels ChoiceBox
        channelList = new ChoiceBox<>();
        channelList.setOnAction(e -> {
            //currentChannelName =  channelList.getValue();
            clientUserActor.tell(new GUIMessage("/to "+channelList.getValue()) , clientUserActor);
        });

        HBox bottomHBox = new HBox();
        HBox.setHgrow(chatInput, Priority.ALWAYS);
        bottomHBox.getChildren().addAll(chatInput,sendButton,channelList);
        borderPane.setBottom(bottomHBox);

        // Set Scene and paint Stage
        chatScene = new Scene(borderPane, 800, 600);
        stage.setScene(chatScene);
        stage.show();
    }

    private void paintErrorWindow(String message) {
        Label label = new Label(message);
        Button button = new Button("OK");
        button.setOnAction(e -> paintLoginWindow());
        VBox vb = new VBox();
        vb.getChildren().addAll(label, button);
        vb.setAlignment(Pos.CENTER);
        Scene badPort = new Scene(vb, 800, 150);
        stage.setScene(badPort);
    }

    private void sendInput(String text) {
        clientUserActor.tell(new GUIMessage(text), null);
        System.out.println("$ [" + LocalTime.now().toString() + "] Sent: " +text);
    }

    protected void printText(String text) {
        Platform.runLater(() -> this.chatBox.appendText(text+"\n"));
    }

    protected  void invalidSyntax(String text){
        printText("["+LocalTime.now().toString()+"] *** Invalid Syntax: " + text);
    }

    public void setTitle(String title) {
        Platform.runLater(() -> this.stage.setTitle(title));
    }

    public void setChatBox(String text) {
        Platform.runLater(() ->{
            chatBox.clear();
            chatBox.appendText(text);
        });
    }

    public void addUser(String userName){
        Platform.runLater(() -> this.userList.getItems().add(userName));
    }

    public void removeUser(String userName){
        Platform.runLater(() -> this.userList.getItems().remove(userName));
    }

    public void addChannel(String channelName){
        clearContext();
        Platform.runLater(() ->
            channelList.getItems().add(channelName));
        currentChannelName = channelName;
        setTitle("User: "+userName+", Channel: "+channelName);
    }

    public void removeChannel(String channelName){
        if(currentChannelName == channelName)
            clearContext();
        Platform.runLater(() ->
            this.channelList.getItems().remove(channelName));
    }

    public void clearContext(){
        Platform.runLater(() -> {
            currentChannelName = null;
            setTitle("User: " + userName);
            userList.getItems().clear();
            chatBox.clear();
        });
    }

    private void login(String userName, String serverName, String port) {// TODO: add server add Error support
        if(userName.startsWith("+") || userName.startsWith("@") || userName.startsWith("$")){
           paintErrorWindow("User name can not start with +, @ or $\ntry another name");
           return;
        }
        try {
            this.userName = userName;
            Config configWithPort = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load());
            ConfigFactory.invalidateCaches();
            ///Config actualConfig = configWithPort.withFallback(ConfigFactory.load());
            system = ActorSystem.create("IRCClient", configWithPort);
            //system.actorOf(Props.create(Main.Terminator.class, a), "terminator");
            clientUserActor = system.actorOf(Props.create(ClientUserActor.class, userName,serverName, this), "ClientUserActor");
            paintChatWindow();
        }catch (ChannelException ex){
            paintErrorWindow("Could not coonect with the given Server and IP\n " +
                    "make sure that you are using a vlid server name/IP and try a new port");
        }
    }

    public void doExit(){
        clientUserActor.tell(new ExitMessage(),clientUserActor);
    }
}
