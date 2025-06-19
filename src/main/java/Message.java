import Keys.AsymmetricKeyPair;
import Keys.SymmetricKey;
import Util.LogLevel;
import Util.Logger;

import java.io.IOException;
import java.util.UUID;

public class Message {

    private String id;    // unique for all messages
    private MessageMainType messageMainType;    // REQUEST/REPLY
    private MessageSubType messageSubType;  // CHAT, ONION, ...
    private String connection_id;
    private String body;      // body of message
    // for ONION messages it has form: next_address connection_id inner_msg


    // constructors

    // General constructor for setting all fields
    public Message(String id, MessageMainType messageMainType, MessageSubType messageSubType, String connection_id, String body) throws IOException{

        // REPLY messages need to have the same id as the corresponding REQUEST
        if(messageMainType == MessageMainType.REPLY){
            if(id == null) {
                Logger.log("REPLY messages need to have the same id as the corresponding REQUEST message", LogLevel.WARN);
                throw new IOException();
            }
            this.id = id;
        }else{
            // type == Type.REQUEST
            this.id = UUID.randomUUID().toString();
        }
        this.messageMainType = messageMainType;
        this.messageSubType = messageSubType;
        this.connection_id = (connection_id == null ) ? UUID.randomUUID().toString() : connection_id;
        this.body = body;
    }


    // from a string - mainly when reading a raw string, to get a Message object
    public Message(String rawMessage) throws IOException{
        String[] tokens = rawMessage.split(" ", 5);

        // not enough fields
        if(tokens.length != 5){
            Logger.log("Message has way too few fields", LogLevel.ERROR);
            throw new IOException();
        }

        this.id = tokens[0];
        this.messageMainType = MessageMainType.valueOf(tokens[1]);
        this.messageSubType = MessageSubType.valueOf(tokens[2]);
        this.connection_id = tokens[3];
        this.body = tokens[4];
    }

    // - static methods for more easily creating messages - like createCHAT

    // CHAT
    public static Message createCHAT(String body){
        return createCHAT_REQUEST(body);
    }

    public static Message createCHAT_REQUEST(String body){
        try {
            return new Message(null, MessageMainType.REQUEST, MessageSubType.CHAT, null, body);
        } catch (IOException e) {
            // redundant for this kind of messages
            return null;
        }
    }

    public static Message createCHAT_REPLY(String id){
        try {
            return new Message(id, MessageMainType.REPLY, MessageSubType.CHAT, null, "Chat message successfully received by " + System.getenv("PEER_ID"));
        } catch (IOException e) {
            return null;
        }
    }
    // end CHAT

    // PEER_DISCOVERY
    public static Message createPEER_DISCOVERY_REQUEST(){
        try {
            return new Message(null, MessageMainType.REQUEST, MessageSubType.PEER_DISCOVERY, null,".");
        } catch (IOException e) {
            return null;
        }
    }


    public static Message createPEER_DISCOVERY_REPLY(String id, String senderAddress){
        // -  make a list of addresses excluding the sender's address
        // addresses are separated by ';'
        String addresses = PeerList.getAddressList(senderAddress, PeerList.getSize(), true);

        try {
            return new Message(id, MessageMainType.REPLY, MessageSubType.PEER_DISCOVERY, null, addresses);
        } catch (IOException e) {
            return null;
        }
    }
    // end PEER_DISCOVERY


    // KEY_EXCHANGE

    // REQUEST KEY_EXCHANGE contains a public key, with which the symmetric key in the REPLY is encrypted with
    public static Message createKEY_EXCHANGE_REQUEST(AsymmetricKeyPair keyPair){
        try {
            Message message = new Message(null, MessageMainType.REQUEST, MessageSubType.KEY_EXCHANGE, null, keyPair.encodePublicKey());
            MessageHandling.addKeyPair(message.getId(), keyPair);
            // storing the keyPair for when the REPLY is received
            // one keyPair is used per one KEY_EXCHANGE request/reply messages
            return message;
        } catch (IOException e) {
            // redundant
            return null;
        }
    }
    public static Message createKEY_EXCHANGE_REQUEST(){
        return createKEY_EXCHANGE_REQUEST(new AsymmetricKeyPair());
    }

    // in REPLY KEY_EXCHANGE, the symmetric key is encrypted with the public key from the request
    public static Message createKEY_EXCHANGE_REPLY(Message request, AsymmetricKeyPair publicKey, SymmetricKey symmetricKey){
        String body = publicKey.encrypt(symmetricKey.encodeToString());
        try {
            return new Message(request.getId(), MessageMainType.REPLY, MessageSubType.KEY_EXCHANGE, request.getConnection_id(), body);
        } catch (IOException e) {
            return null;
        }
    }
    // end KEY_EXCHANGE

    // ONION
    public static Message createONION_REQUEST(String connection_id, String body){
        // body is already encrypted with key corresponding to connection_id
        try {
            return new Message(null, MessageMainType.REQUEST, MessageSubType.ONION, connection_id, body);
        } catch (IOException e) {
            // redundant
            return null;
        }
    }

    public static Message createONION_REPLY(Message request, String body){
        try {
            return new Message(request.getId(), MessageMainType.REPLY, MessageSubType.ONION, request.getConnection_id(), body);
        } catch (IOException e) {
            return null;
        }
    }
    // end ONION


    // GET_VARIABLE
    public static Message createGET_REQUEST(String variable_name){
        try {
            return new Message(null, MessageMainType.REQUEST, MessageSubType.GET, null, variable_name);
        } catch (IOException e) {
            // redundant
            return null;
        }
    }

    public static Message createGET_REPLY(Message request){
        try {
            String value = Global.getVariable(request.getBody());
            String body = request.getBody() + " == \"" + (value != null ? value : "null") + "\"";
            return new Message(request.getId(), MessageMainType.REPLY, MessageSubType.GET, null, body);
        } catch (IOException e) {
            return null;
        }
    }
    // end GET_VARIABLE

    // end constructors


    // toString conversion
    public String toString(){
        return id + " " + messageMainType.name() + " " + messageSubType.name() + " " + connection_id  + " " + body;
    }


    // getters
    public String getId() {
        return id;
    }

    public MessageMainType getMessageMainType() {
        return messageMainType;
    }

    public MessageSubType getMessageSubType() {
        return messageSubType;
    }

    public String getBody() {
        return body;
    }

    public String getConnection_id() {
        return connection_id;
    }

    // end getters

}