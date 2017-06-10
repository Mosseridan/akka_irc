	
Authors: Idan Mosseri 305555179 & Liad Ginosar 203335229

In our architecture:
on the client:
- ChatWindow class is the application preccess acountable for the UI.
- ClientUserActor class is the actor on the client side responsible for parsing the users commands and handing the ChatWindow incomming content.
on the ServerSide:
- The ServerActro is responsible for receiving connection requests from client users and then creating a ServerUserActor for each connected Client.
- After the initial connection the ServerUserActor is responsible for the communication with a single client and all other clients and channels.
- The ChannelCreator is apointed on creating new ChannelActors for each newly created channel.
- The ChannelActor is a broadcast router responsible for broadcasting broadcast messages and alerts to all the the users connected to the channel.
- The ServerUserChannelActor is an actor created by the ServerUserActor for each channel the uses joins this actor represents chis clients identity and behavior in the given channel.
  The ServerUserChannelActor has different defined behaviors bor the user,voiced,operator,owner and banned stats.
  these behaviors implement wat the client can or cannot do in his given state in the channel.
  uppon receiving a message impling a state change the ServerUserChannelActorusers the become and unbecome methods to change his behvious acordingly.
  
 Message types: all messages use the fire and forget policy
 
 Outgoing Messages are sent from a ClientUserActor to the to is corrosponding ServerUserActor which then finds the ServerUserChannelActor for the Apropriate Channel and sends him this message
 appon receiving an Outgoing Messages a serverUserChannelActor sends the apropriate Incoming Message either directry to the recipient ServerUserChannelActor or to the channelActor which then routs the message.
	
 
	AddUserNameMessage 	- this message is sent from a ServerUserChannelActor after receiving a GetUserNameMessage or form the Channel Actor after adding anewly joined user. the message is either sent to the requesting ServerUserChannelActor ot broadcast to the entire channel
	AnnouncementMessage - anny message sent from the the Channel Annoncing Changes Like users being apponted voiced banned and so fourth.
	ApointOwnerMessage - a request sent from a channel owners ServerUserChannelActor to the channelActor appon leaveing the channel in order to tell the ChannelActor to apoint a new Owner
	BecomeOwnerMessage - sent from the channel actor to a newly apointer owners ServerUserChannelActoruser.
	ChangeTitleMessage - used to change channels title
	ChannelKilledMessage - telling clints the channel has been kiiled
	ConnectApprovalMessage - returned to the client uppon successful connection to server.
	ConnectMessage - used to request a connecting to server and the creation of a serverUserActor
	ErrorMessage - used for error message passing
	ExitMessage - a request for client termination
	GUIMessage - any given commant passed to the clienUserActor then parsed by him
	GetContentMessage - used to upon changing channels in the UI in order to receive the channels user list title and so fourth without saving it in the user.
	GetUserNameMessage - a request sent to a ServerUserChannelActor to know his user name sent from the ChannelActor after receiving the GetContentMessage.
	IncomingAddOperatorMessage - sent from a sending ServerUserChannelActor to another in order to tell him he is to become an operator.
	IncomingAddVoicedMessage - *** to become voiced
	IncomingBanMessage - *** banned
	IncomingBroadcastMessage - used to broadcast messages.
	IncomingKickMessage -sent from a sending ServerUserChannelActor to another in order to tell him he is being kicked from the channel.
	IncomingKillChannelMessage - used to terminate channel
	IncomingPrivateMessage - 
	IncomingRemoveOperatorMessage
	IncomingRemoveVoicedMessage
	JoinApprovalMessage
	JoinMessage
	LeaveMessage
	Message
	OutgoingAddOperatorMessage -  sent from a ClientActor to A ServerActor that then finds the appropriate ServerUserChannelActor and sends him so the message so he will will send an incommingAddOperatorMessage to the target serverUserChannelActor to tell him he is to become an operator.
	OutgoingAddVoicedMessage -  *** voiced
	OutgoingBanMessage - *** banned
	OutgoingBroadcastMessage 
	OutgoingKickMessage -  *** kicked
	OutgoingKillChannelMessage 
	OutgoingPrivateMessage 
	OutgoingRemoveOperatorMessage
	OutgoingRemoveVoicedMessage 
	TitleChangedMessage - used to tell all users in the channel that the title has chaned
	UserJoinedMessage - used to tell all users in the channel a new user has join and that they should add him to th user list
	UserLeftMessage -  *** remove him from the userLis