import Util.LogLevel;
import Util.Logger;
import Keys.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class OnionConnection implements Runnable {
    // for storing keys when you are the original sender of onion message

    // its run method is a protocol for establishing keys between you and the in-between nodes

    private LinkedBlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    // put in at MessageHandling if message.getId() is present in

    private boolean connection_established = false; // false until all keys are received

    private int n;  // number of in-between nodes
    private String[] connection_ids;
    private SymmetricKey[] symmetricKeys;
    private String[] addresses;
    private final String final_address;

    private final Message quitMessage = Message.createCHAT(Global.getOCDropPhrase());

    // Constructors
    public OnionConnection(int n, String final_dest) throws IOException{
        final_address = final_dest;
        if(PeerList.getPeer(final_address) == null){
            Logger.log("Final address for onion connection not present in PeerList:" + final_address, LogLevel.ERROR);
            throw new IOException();
        }

        // no point in having more in-between nodes than the total amount of peers in the network
        // or using the final destination as an in-between node
        n = Math.max(n, 0); // in case n < 0
        int max_bound = Math.max(PeerList.getSize() - 1, 0);    // in-case PeerList.getSize() == 0
        this.n = Math.min(n, max_bound);

        this.symmetricKeys = new SymmetricKey[n];
        this.addresses = new String[n];
        this.connection_ids = new String[n];
        MyOnionConnectionList.add(this);
    }

    public OnionConnection(String final_dest) throws IOException{
        final_address = final_dest;
        if(PeerList.getPeer(final_address) == null){
            Logger.log("Final address for onion connection not present in PeerList:" + final_address, LogLevel.ERROR);
            throw new IOException();
        }

        int max_bound = Math.max(PeerList.getSize() - 1, 0);    // in-case PeerList.getSize() == 0
        // default number of in-between peers == 3
        n = Math.min(3, max_bound);

        this.symmetricKeys = new SymmetricKey[n];
        this.addresses = new String[n];
        this.connection_ids = new String[n];

        MyOnionConnectionList.add(this);
    }
    // end Constructors



    // starts with a protocol for exchanging keys
    // then process the messages which it sent out - taking out from a queue
    @Override
    public void run() {

        // 1 - get keys
        // 2 - process messages

        Thread.currentThread().setName("OnionConnection[" + final_address + "]");

        addresses = PeerList.getAddressArray(final_address, n, true);

        // 1 - get keys
        //  - if an error happens during this key exchange protocol
        //  - this OC is removed from MyOnionConnectionList and connection is dropped
        Message message;
        for (int i = 0; i < n; i++) {

            // create KEY_EXCHANGE message
            message = Message.createKEY_EXCHANGE_REQUEST();

            // - wrap it in ONION messages with encryption and corresponding connection_ids
            for (int j = i; j > 0; j--) {
                // nextAddress == address[j]
                // message for peer[address[j-1]], so encrypt with key[j-1]
                String encrypted_body = symmetricKeys[j-1].encrypt(addresses[j] + " " + message.toString());
                message = Message.createONION_REQUEST(connection_ids[j-1], encrypted_body);
            }

            MyOnionConnectionList.addRequest(message.getId(), this);
            // send message to peer[address[0]]
            PeerList.getPeer(addresses[0]).sendMessage(message);


            // - wait until reply is received
            // - get reply
            try {
                message = messageQueue.take();
            } catch (InterruptedException e) {
                Logger.log("Error during key exchange protocol, while trying to take message from queue!", LogLevel.ERROR);
                MyOnionConnectionList.remove(this);
                return;
            }

            //  - peel off the every encryption
            message = decrypt(i, message);
            if(message == null){
                Logger.log("Error in unwrapping message during key exchange protocol", LogLevel.ERROR);
                MyOnionConnectionList.remove(this);
                return;
            }


            //  - get the key
            //  - store the key
            symmetricKeys[i] = MessageHandling.handleKEY_EXCHANGE_REPLY(message);
            connection_ids[i] = message.getConnection_id();
            if (symmetricKeys[i] == null){
                // logger in the above method call
                MyOnionConnectionList.remove(this);
                return;
            }

        }

        setConnection_established(true);
        Logger.log("Successfully established OnionConnection with in-between addresses:" + Arrays.toString(addresses) + "; and final address " + final_address, LogLevel.SUCCESS);

        // 2 - process messages
        while (isConnection_established() || !messageQueue.isEmpty()){
            try {
                message = messageQueue.take();
            } catch (InterruptedException e) {
                Logger.log("Error extracting message from queue", LogLevel.WARN);
                continue;
            }
            if(message.equals(quitMessage)){
                break;
            }
            message = decrypt(message);
            MessageHandling.handle(message, PeerList.getPeer(final_address));
        }

        // - send a message which tells each node on the path to drop connection, i.e. forget the key
        dropConnection();
        MyOnionConnectionList.remove(this);
    }


    // whether all keys are received
    public boolean isConnection_established(){
        return connection_established;
    }

    private void setConnection_established(boolean status){
        connection_established = status;
    }

    // SEND message
    public void sendMessage(Message message){
        if (!isConnection_established()){
            return;
        }

        //  encrypt message - using key corresponding with appropriate nextAddress and stuff
        String encrypted_body = symmetricKeys[n-1].encrypt(final_address + " " + message.toString());
        message = Message.createONION_REQUEST(connection_ids[n-1], encrypted_body);
        for (int i = n-1; i > 0; i--) {
            encrypted_body = symmetricKeys[i-1].encrypt(addresses[i] + " " + message.toString());
            message = Message.createONION_REQUEST(connection_ids[i-1], encrypted_body);
        }

        //  save message id in OnionConnectionList, so that when REPLY is received
        //  it is added to this OC's message queue
        MyOnionConnectionList.addRequest(message.getId(), this);
        //  send it
        PeerList.getPeer(addresses[0]).sendMessage(message);
    }

    public void dropConnection(){

        Logger.log("Dropping onion connection ...", LogLevel.DEBUG);
        setConnection_established(false);

        Message message;
        //  encrypt message - using key corresponding with appropriate nextAddress and stuff
        // no need to send dropConnection message to the final node so no final_address is given
        String encrypted_body = symmetricKeys[n-1].encrypt("drop " + null + " " + Global.getOCDropPhrase());
        message = Message.createONION_REQUEST(connection_ids[n-1], encrypted_body);
        for (int i = n-1; i > 0; i--) {
            encrypted_body = symmetricKeys[i-1].encrypt("drop " + addresses[i] + " " + message.toString());
            message = Message.createONION_REQUEST(connection_ids[i-1], encrypted_body);
        }

        //  send it
        PeerList.getPeer(addresses[0]).sendMessage(message);
    }


    private Message decrypt(int wrapCount, Message message){
        String inner;
        for (int i=0; i < wrapCount; i++){
            inner = symmetricKeys[i].decrypt(message.getBody());

            if(inner.equals(symmetricKeys[i].encrypt(Global.getOCDropPhrase()))){
                setConnection_established(false);
                addMessageToQueue(quitMessage);
//                Logger.log("Received encrypted drop phrase for dropping onion connection!", LogLevel.DEBUG);
                return null;
            }
            try {
                message = new Message(inner);
            } catch (IOException e) {
                Logger.log("Invalid inner message in ONION");
                return null;
            }
        }
        return message;
    }
    public Message decrypt(Message message){
        return decrypt(n, message);
    }

    // used for adding messages to processing queue
    // only if message was sent by this Onion Connection
    public void addMessageToQueue(Message message){
        messageQueue.add(message);
    }


    // getters

    public String[] getMiddleNodes() {
        return addresses;
    }

    public String getFinalAddress() {
        return final_address;
    }

    // end getters
}
