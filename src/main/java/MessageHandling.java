import Util.Logger;

public class MessageHandling {
    // includes static methods for processing messages
    // though a user can just call handleMessage() passing it a message of any type


    public static void handle(Message message, Peer sender){
        if(message.getType() == MessageMainType.REQUEST){
            switch (message.getMessageType()){
                case CHAT -> handleCHAT_REQUEST(message, sender);
                case PEER_DISCOVERY -> handlePEER_DISCOVERY_REQUEST(message, sender);
                case KEY_EXCHANGE -> Logger.log("Todo");
                case ONION -> Logger.log("Todo");
            }
        }else{
            switch (message.getMessageType()){
                case CHAT -> handleCHAT_REPLY(message, sender);
                case PEER_DISCOVERY -> handlePEER_DISCOVERY_REPLY(message);
                case KEY_EXCHANGE -> Logger.log("Todo");
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

    // todo - KEY_EXCHANGE

    // todo - ONION

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

    // todo - ONION

    // end REPLY Handlers

}
