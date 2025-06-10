import Util.LogLevel;
import Util.Logger;

import java.io.IOException;
import java.util.UUID;

public class OnionMessage extends Message{


    private final String next_address;
    private final String connection_id;


    // constructors

    // general constructor
    public OnionMessage(String id, MessageMainType messageMainType, String next_address, String connection_id, String body) throws IOException{
        super(id, messageMainType);

        // subtype set to ONION in super()

        this.next_address = next_address;
        if(connection_id == null){
            this.connection_id = UUID.randomUUID().toString();
        }else{
            this.connection_id = connection_id;
        }
        this.body = body;
    }


    // constructor from a Message object which has subtype ONION
    public OnionMessage(Message message) throws IOException{
        super(message.getId(), message.getType());

        String[] tokens = message.getBody().split(" ", 3);
        if (tokens.length != 3){
            Logger.log("Onion message has invalid format", LogLevel.ERROR);
            throw new IOException();
        }

        this.next_address = tokens[0];
        this.connection_id = tokens[1];
        this.body = tokens[2];
    }

    private OnionMessage(String rawMessage) throws IOException{
        super();

        String[] tokens = rawMessage.split(" ", 6);

        if(tokens.length != 6){
            Logger.log("Raw onion message has invalid format", LogLevel.ERROR);
            throw new IOException();
        }

        this.id = tokens[0];
        this.messageMainType = MessageMainType.valueOf(tokens[1]);

        if(MessageSubType.valueOf(tokens[2]) != MessageSubType.ONION){
            Logger.log("Trying to create a raw message which isn't an ONION message", LogLevel.ERROR);
            throw new IOException();
        }
        this.messageSubType = MessageSubType.ONION;

        this.next_address = tokens[3];
        this.connection_id = tokens[4];
        this.body = tokens[5];
    }
    // end constructors


    // to string conversion
    @Override
    public String toString() {
        return id + " " + messageMainType + " " + messageSubType + " " + next_address + " " + connection_id + " " + body;
    }


    // getters
    public String getNext_address() {
        return next_address;
    }

    public String getConnection_id() {
        return connection_id;
    }
    // end getters
}
