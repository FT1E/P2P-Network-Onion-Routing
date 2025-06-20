import Keys.*;
import Util.LogLevel;
import Util.Logger;

import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentHashMap;

public class MessageHandling {
    // includes static methods for processing messages
    // though a user can just call handleMessage() passing it a message of any type


    // dictionary for key_exchange requests
    private static ConcurrentHashMap<String, AsymmetricKeyPair> key_exchange_map = new ConcurrentHashMap<>();

    // when you receive a REPLY whether you should process it as a normal REPLY
    // or if you need to wrap it in encryption and send it back
    private static ConcurrentHashMap<String, OnionHandler> onionRequests = new ConcurrentHashMap<>();


    public static void handle(Message message, Peer sender){
        if(message == null){
            return;
        }
        if(message.getMessageMainType() == MessageMainType.REQUEST){
            switch (message.getMessageSubType()){
                case CHAT -> handleCHAT_REQUEST(message, sender);
                case PEER_DISCOVERY -> handlePEER_DISCOVERY_REQUEST(message, sender);
                case KEY_EXCHANGE -> handleKEY_EXCHANGE_REQUEST(message, sender);
                case ONION -> handleONION_REQUEST(message, sender);
                case GET -> handleGET_REQUEST(message, sender);
            }
        }else if (!MyOnionConnectionList.checkReply(message) && !OnionHandlerWaiting(message)){
            // - check if REPLY needs to be processed by an ONION connection
            // - or if it needs to wrapped in REPLY ONION

            // in here only if message doesn't need to be processed by some OnionConnection or OnionHandler
            switch (message.getMessageSubType()){
                case CHAT -> handleCHAT_REPLY(message, sender);
                case PEER_DISCOVERY -> handlePEER_DISCOVERY_REPLY(message);
                case KEY_EXCHANGE -> handleKEY_EXCHANGE_REPLY(message);
                case ONION -> handleONION_REPLY(message);
                case GET -> handleGET_REPLY(message, sender);
            }
        }
    }


    // REQUEST Handlers

    // - CHAT
    private static void handleCHAT_REQUEST(Message message, Peer sender){
        Logger.chat(sender.getAddress(), message.getBody());
        sender.sendMessage(Message.createCHAT_REPLY(message.getId()));
    }

    // - PEER_DISCOVERY
    private static void handlePEER_DISCOVERY_REQUEST(Message message, Peer sender){
        sender.sendMessage(Message.createPEER_DISCOVERY_REPLY(message.getId(), sender.getAddress()));
    }

    // - KEY_EXCHANGE
    private static void handleKEY_EXCHANGE_REQUEST(Message message, Peer sender){
        AsymmetricKeyPair publicKey = null;
        try {
            publicKey = new AsymmetricKeyPair(message.getBody());
        } catch (InvalidKeySpecException e) {
            Logger.log("Invalid public key encoding received in a KEY_EXCHANGE REQUEST", LogLevel.ERROR);
            return;
        }

        SymmetricKey symmetricKey = new SymmetricKey();

        // - store the symmetric key
        OnionKeys.add(message.getConnection_id(), symmetricKey);

        sender.sendMessage(Message.createKEY_EXCHANGE_REPLY(message, publicKey, symmetricKey));
    }

    // - ONION
    private static void handleONION_REQUEST(Message outerMessage, Peer requester){
        //  - getConnectionId
        //  - get corresponding key - if none just send back a REPLY unknown or something
        SymmetricKey symmetricKey = OnionKeys.get(outerMessage.getConnection_id());

        if(symmetricKey == null){
            // no point in doing anything if you don't have the key
            Logger.log("Received a REQUEST ONION, but no corresponding key stored", LogLevel.WARN);
            return;
        }

        //  - decrypt to get inner message which should have body == "<nextAddress> <msg.toString()>"
        String decrypted_body = symmetricKey.decrypt(outerMessage.getBody());

//        Logger.log("Decrypted body: " + decrypted_body, LogLevel.DEBUG);

        boolean drop=false;   // whether you need to save an OnionHandler or not, i.e. whether you expect to receive a REPLY or not

        String[] tokens = decrypted_body.split(" ", 2);
        String nextAddress;
        Message innerMessage;
        if("drop".equals(tokens[0])){
            // if it's an onion request to drop connection
            drop = true;
            // remove the key
            OnionKeys.remove(outerMessage.getConnection_id());
            tokens = decrypted_body.split(" ", 3);
            nextAddress = tokens[1];
            try {
                innerMessage = new Message(tokens[2]);
            } catch (IOException e) {
//                Logger.log("Invalid inner message in REQUEST ONION");
                return;
            }
            // pass on the inner drop message to the next node
        } else{
            // if it's a normal onion request
            nextAddress = tokens[0];
            try {
                innerMessage = new Message(tokens[1]);
            } catch (IOException e) {
                Logger.log("Invalid inner message in REQUEST ONION");
                return;
            }

        }


        //  - get next peer
        Peer peer = PeerList.getPeer(nextAddress);
        if (peer == null){
            Logger.log("Not connected to nextAddress == [" + nextAddress + "], trying to connect to it ...", LogLevel.WARN);
            try{
                peer = new Peer(nextAddress);
                Logger.log("Successfully connected to nextAddress == [" + nextAddress + "]", LogLevel.SUCCESS);
            }catch (IOException e){
                Logger.log("Failed to connect to nextAddress == [" + nextAddress + "], sending back message to drop connection", LogLevel.ERROR);
                // send back to the requester to drop onion connection
                innerMessage = Message.createONION_REPLY(outerMessage, symmetricKey.encrypt(symmetricKey.encrypt(Global.getOCDropPhrase())));
                peer = requester;
                drop = true;
            }
        }

        if(!drop) {
            //  - save a runnable or something which is started when REPLY of the inner message is received
            OnionHandler onionHandler = new OnionHandler(outerMessage, requester, symmetricKey);
            onionRequests.put(innerMessage.getId(), onionHandler);
        }

        //  - send it to next
        peer.sendMessage(innerMessage);

    }


    // GET_VARIABLE
    private static void handleGET_REQUEST(Message request, Peer sender){
        sender.sendMessage(Message.createGET_REPLY(request));
    }

    // end REQUEST Handlers

    // REPLY Handlers

    // - CHAT
    private static void handleCHAT_REPLY(Message message, Peer sender){
        Logger.chat(sender.getAddress(), message.getBody());
    }

    // - PEER_DISCOVERY
    private static void handlePEER_DISCOVERY_REPLY(Message message){

        String[] addresses = message.getBody().split(";");
        //  - try to connect to every peer in the list
        //  - don't connect to peers you're already connected to
        for (int i = 0; i < addresses.length; i++) {
            try {
                new Peer(addresses[i]); // peer won't be added if a connection with that peer is already established
            } catch (IOException e) {
                // logger in constructor
            }
        }
    }

    // - KEY_EXCHANGE
    public static SymmetricKey handleKEY_EXCHANGE_REPLY(Message message){

        // this might happen if a connection is dropped during the key exchange protocol while establishing onion connection
        if(message == null || message.getMessageSubType() != MessageSubType.KEY_EXCHANGE){
            return null;
        }

        //  - get corresponding private key
        //  - decrypt message with it
        //  - store the key in corresponding onion connection


        //  - get corresponding private key
        AsymmetricKeyPair keyPair = key_exchange_map.remove(message.getId());
        if(keyPair == null){
            // if you don't have the corresponding private key you can't decrypt the message, so yeah
            Logger.log("REPLY KEY_EXCHANGE received, but no corresponding keyPair stored!", LogLevel.WARN);
            return null;
        }

        //  - decrypt message with it
        String decrypted_body = keyPair.decrypt(message.getBody());
        SymmetricKey symmetricKey = new SymmetricKey(decrypted_body);

        // - store the key in corresponding onion connection
        // - this message corresponds to an Onion Connection in MyOnionConnectionList
        // so when received it is added to that OC's queue, and it stores it in its own key array
        return symmetricKey;

    }

    // whenever a REQUEST KEY_EXCHANGE message is created, the keypair is stored in a dictionary
    // which is accessed when the corresponding REPLY is received
    public static void addKeyPair(String msg_id, AsymmetricKeyPair keyPair){
        key_exchange_map.put(msg_id, keyPair);
    }
    // end - KEY_EXCHANGE

    // - ONION
    private static void handleONION_REPLY(Message message){
        //  - if you're here, then you're a middle man
        //  - 1 - middle man - encrypt with the corresponding key and send it back

        OnionHandler onionHandler = onionRequests.remove(message.getId());
        if (onionHandler != null){
            onionHandler.init(message);
            onionHandler.run();
        }
    }

    // handling for a REPLY
    // which needs to be wrapped in a REPLY ONION
    // this is for the last node before the final destination
    // he sends a normal message
    // the final destination peer thinks of it as normal message
    // so when the last node receives a REPLY it should somehow know that it needs to wrap it and send it back
    private static boolean OnionHandlerWaiting(Message replyMessage){
        OnionHandler onionHandler = onionRequests.remove(replyMessage.getId());
        if(onionHandler == null){
            return false;
        }
        onionHandler.init(replyMessage);
        onionHandler.run();
        return true;
    }

    // GET_VARIABLE
    private static void handleGET_REPLY(Message reply, Peer sender){
        Logger.chat(sender.getAddress(), reply.getBody());
    }

    // end REPLY Handlers

}
