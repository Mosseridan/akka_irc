import Shared.Messages.*;
import akka.actor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalTime;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


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
    private ListView<TextFlow> chatBox;
    private ChoiceBox<String> channelList;
    private ListView<Text> userList;
    private Label chatTitle;

    public static final Font ITALIC_FONT = Font.font("Serif", FontPosture.ITALIC, Font.getDefault().getSize());


    public static void main(String[] args) {
        launch(args);
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
        serverInput = new TextField("127.0.0.1");
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
        stage.setTitle("Akka-IRC  "+userName);
        stage.setOnCloseRequest(this::doExit);
        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(20, 20, 20, 20));

        // Chat Label
        chatTitle =  new Label("UserName: "+userName);
        borderPane.setTop(chatTitle);

        // Chat Box
        chatBox = new ListView<>();
        chatBox.setEditable(false);
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
        channelList.setOnAction(e -> clientUserActor.tell(new GUIMessage("/to "+channelList.getValue()) , clientUserActor));

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

    public Text userNameToText(String userName){
        Text textUserName = null;
        if(userName.startsWith("$")){
            textUserName = new Text("@"+userName.substring(1));
            textUserName.setFont(ITALIC_FONT);
        } else{
            textUserName = new Text(userName);
        }
        return textUserName;
    }

    protected void printText(String text) {
        Platform.runLater(() -> this.chatBox.getItems().add(new TextFlow(new Text(text))));    //appendText(text+"\n"));
    }

    protected void printText(TextFlow text) {
        Platform.runLater(() -> this.chatBox.getItems().add(text));       //(text+"\n"));
    }

    public void printAlert(String time, String userName, String action){
        printText(new TextFlow(new Text(time),new Text("*** "),new Text(action),new Text(": "),userNameToText(userName)));
    }

    public void printThisByThat(String time,String to,String action,String by){
        printText(new TextFlow(new Text(time), new Text("*** "),userNameToText(to),new Text(" "),new Text(action),new Text(" by "),userNameToText(by)));
    }

    protected  void invalidSyntax(String text){
        printText("["+LocalTime.now().toString()+"] *** Invalid Syntax: " + text);
    }

    public void setTitle(String title) {
        Platform.runLater(() -> chatTitle.setText(title));
    }

    public void addUser(String userName){
        Platform.runLater(() -> this.userList.getItems().add(userNameToText(userName)));
    }

    public void removeUser(String userName){
        Platform.runLater(() -> this.userList.getItems().remove(userNameToText(userName)));
    }

    public void switchChannel(String channelName){
        clearContext("ChannelName: "+channelName);
        currentChannelName = channelName;

        clientUserActor.tell(new GetContentMessage(userName,channelName),clientUserActor);
    }

    public void addChannel(String channelName){ //TODO add title
        Platform.runLater(() ->
                channelList.getItems().add(channelName));
       switchChannel(channelName);
    }

    public void removeChannel(String channelName){
        if(currentChannelName == channelName)
            clearContext();
        Platform.runLater(() ->
            this.channelList.getItems().remove(channelName));
    }

    public void clearContext(){
        clearContext();
    }

    public void clearContext(String title){
        Platform.runLater(() -> {
            currentChannelName = null;
            setTitle(title);
            userList.getItems().clear();
            chatBox.getItems().clear();
        });
    }

    private void login(String userName, String serverName, String port) {// TODO: add server add Error support
        if(userName.startsWith("+") || userName.startsWith("@") || userName.startsWith("$")){
           paintErrorWindow("User name cannot start with +, @ or $\ntry another name");
           return;
        }
        try {
            this.userName = userName;
            Config configWithPort = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(ConfigFactory.load());
            ConfigFactory.invalidateCaches();
            ///Config actualConfig = configWithPort.withFallback(ConfigFactory.load());
            system = ActorSystem.create("IRCClient", configWithPort);

            clientUserActor = system.actorOf(Props.create(ClientUserActor.class, userName,serverName, this), "ClientUserActor");
            system.actorOf(Props.create(Main.Terminator.class, clientUserActor), "terminator");
            paintChatWindow();
        }catch (Exception ex){
            paintErrorWindow("Could not connect with the given Server and IP\n " +
                    "make sure that you are using a valid server name/IP and try a new port");
        }
    }

    private void doExit(WindowEvent windowEvent) {
        System.out.println("$$$ Closing Client!");
        clientUserActor.tell(new ExitMessage(),clientUserActor);
    }

}
