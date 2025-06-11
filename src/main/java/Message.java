import Util.LogLevel;
import Util.Logger;

import java.io.IOException;
import java.util.UUID;

public class Message {

    protected String id;    // unique for all messages
    protected MessageMainType messageMainType;    // REQUEST/REPLY
    protected MessageSubType messageSubType;  // CHAT, ONION, ...
    protected String body;      // body of message
    // for ONION messages it has form: next_address connection_id inner_msg


    // constructors

    // General constructor for setting all fields
    public Message(String id, MessageMainType messageMainType, MessageSubType messageSubType, String body) throws IOException{

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

        // ONION messages should be made with the OnionMessage class
        if(messageSubType == MessageSubType.ONION){
            Logger.log("ONION messages should be made with the OnionMessage constructor", LogLevel.ERROR);
            throw new IOException();
        }

        this.messageSubType = messageSubType;
        this.body = body;
    }


    // from a string - mainly when reading a raw string, to get a Message object
    public Message(String rawMessage) throws IOException{
        String[] tokens = rawMessage.split(" ", 4);

        // not enough fields
        if(tokens.length != 4){
            Logger.log("Message has way too few fields", LogLevel.ERROR);
            throw new IOException();
        }

        this.id = tokens[0];
        this.messageMainType = MessageMainType.valueOf(tokens[1]);
        this.messageSubType = MessageSubType.valueOf(tokens[2]);
        this.body = tokens[3];
    }

    // for OnionMessage constructor
    protected Message(String id, MessageMainType messageMainType) throws IOException{
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
        this.messageSubType = MessageSubType.ONION;
    }


    // I don't know why, but every constructor in inheriting class needs to call super
    // and if super isn't called in the first line it's an error
    protected Message(){}

    // - static methods for more easily creating messages - like createCHAT

    // CHAT
    public static Message createCHAT(String body){
        return createCHAT_REQUEST(body);
    }

    public static Message createCHAT_REQUEST(String body){
        try {
            return new Message(null, MessageMainType.REQUEST, MessageSubType.CHAT, body);
        } catch (IOException e) {
            // redundant for this kind of messages
            return null;
        }
    }

    public static Message createCHAT_REPLY(String id){
        try {
            return new Message(id, MessageMainType.REPLY, MessageSubType.CHAT, "Chat message successfully received");
        } catch (IOException e) {
            return null;
        }
    }
    // end CHAT

    // PEER_DISCOVERY
    public static Message createPEER_DISCOVERY_REQUEST(){
        try {
            return new Message(null, MessageMainType.REQUEST, MessageSubType.PEER_DISCOVERY, ".");
        } catch (IOException e) {
            return null;
        }
    }


    public static Message createPEER_DISCOVERY_REPLY(String id, String senderAddress){
        // -  make a list of addresses excluding the sender's address
        // addresses are separated by ';'
        String addresses = PeerList.getAddressList(senderAddress);

        try {
            return new Message(id, MessageMainType.REPLY, MessageSubType.PEER_DISCOVERY, addresses);
        } catch (IOException e) {
            return null;
        }
    }
    // end PEER_DISCOVERY


    // KEY_EXCHANGE

    // REQUEST KEY_EXCHANGE contains a public key, with which the symmetric key in the REPLY is encrypted with
    public static Message createKEY_EXCHANGE_REQUEST(AsymmetricKeyPair keyPair){
        try {
            return new Message(null, MessageMainType.REQUEST, MessageSubType.KEY_EXCHANGE, keyPair.encodePublicKey_toString());
        } catch (IOException e) {
            // redundant
            return null;
        }
    }

    // in REPLY KEY_EXCHANGE, the symmetric key is encrypted with the public key from the request
    public static Message createKEY_EXCHANGE_REPLY(String id, AsymmetricKeyPair publicKey, SymmetricKey symmetricKey){
        String body = publicKey.encryptPublic(symmetricKey.encodeKey_toString());
        try {
            return new Message(id, MessageMainType.REPLY, MessageSubType.KEY_EXCHANGE, body);
        } catch (IOException e) {
            return null;
        }
    }
    // end KEY_EXCHANGE


    // NOTE - ONION is done in a separate class, inheriting message



    // end constructors


    // toString conversion
    public String toString(){
        return id + " " + messageMainType.name() + " " + messageSubType.name() + " " + body;
    }


    // getters
    public String getId() {
        return id;
    }

    public MessageMainType getType() {
        return messageMainType;
    }

    public MessageSubType getMessageType() {
        return messageSubType;
    }

    public String getBody() {
        return body;
    }
    // end getters

}