import Util.LogLevel;
import Util.Logger;

import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

public class MessageHandling {
    // includes static methods for processing messages
    // though a user can just call handleMessage() passing it a message of any type


    // dictionary for key_exchange requests
    private static HashMap<String, AsymmetricKeyPair> key_exchange_map = new HashMap<>();



    public static void handle(Message message, Peer sender){
        if(message.getMessageMainType() == MessageMainType.REQUEST){
            switch (message.getMessageSubType()){
                case CHAT -> handleCHAT_REQUEST(message, sender);
                case PEER_DISCOVERY -> handlePEER_DISCOVERY_REQUEST(message, sender);
                case KEY_EXCHANGE -> handleKEY_EXCHANGE_REQUEST(message, sender);
                case ONION -> Logger.log("Todo");
            }
        }else{
            switch (message.getMessageSubType()){
                case CHAT -> handleCHAT_REPLY(message, sender);
                case PEER_DISCOVERY -> handlePEER_DISCOVERY_REPLY(message);
                case KEY_EXCHANGE -> handleKEY_EXCHANGE_REPLY(message);
                case ONION -> Logger.log("Todo");
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
    // todo - ONION
    private static void handleONION_REQUEST(Message message){
        // todo:
        //      - getConnectionId
        //      - get corresponding key - if none just send back a REPLY unknown or something
        //      - decrypt to get inner message which should have body == "<nextAddress> <msg.toString()>"
        //      - send it to next
        //      - save the request id of both outer and inner message
        //      - save a runnable or something which is started when REPLY of the inner message is received
    }

    // end REQUEST Handlers

    // REPLY Handlers

    // - CHAT
    private static void handleCHAT_REPLY(Message message, Peer sender){
        Logger.chat(sender.getAddress(), message.getBody());
    }

    // - PEER_DISCOVERY
    private static void handlePEER_DISCOVERY_REPLY(Message message){
        //  todo - maybe a better implementation
        //   - if both lists are sorted - merging like in merge sort might be good


        String[] addresses = message.getBody().split(";");
        //  - try to connect to every peer in the list
        //  - don't connect to peers you're already connected to
        for (int i = 0; i < addresses.length; i++) {
            new Peer(addresses[i]); // peer won't be added if a connection with that peer is already established
        }
    }

    // todo - KEY_EXCHANGE
    private static void handleKEY_EXCHANGE_REPLY(Message message){
        // todo:
        //  - get corresponding private key
        //  - decrypt message with it
        //  - store the key in corresponding onion connection


        //  - get corresponding private key
        AsymmetricKeyPair keyPair = key_exchange_map.remove(message.getId());
        if(keyPair == null){
            // if you don't have the corresponding private key you can't decrypt the message, so yeah
            Logger.log("REPLY KEY_EXCHANGE received, but no corresponding keyPair stored!", LogLevel.WARN);
            return;
        }

        //  - decrypt message with it
        String decrypted_body = keyPair.decryptPrivate(message.getBody());
        SymmetricKey symmetricKey = new SymmetricKey(decrypted_body);

        // todo - store the key in corresponding onion connection

    }

    // whenever a REQUEST KEY_EXCHANGE message is created, the keypair is stored in a dictionary
    // which is accessed when the corresponding REPLY is received
    public static void addKeyPair(String msg_id, AsymmetricKeyPair keyPair){
        key_exchange_map.put(msg_id, keyPair);
    }
    // end - KEY_EXCHANGE

    // todo - ONION
    private static void handleONION_REPLY(Message message){
        // todo:
        //  - get msg id
        //  - see if you're the original sender or a middle man based on it


        // todo:
        //      - 1 - middle man - encrypt with the corresponding key and send it back


        // todo:
        //      - 2 - original sender - decrypt with the corresponding keys in corresponding order
        //      - then process the inner most message
    }

    // end REPLY Handlers

}
