import Util.LogLevel;
import Util.Logger;
import Keys.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

public class OnionConnection implements Runnable {
    // for storing keys when you are the original sender of onion message

    // its run method is a protocol for establishing keys between you and the in-between nodes


    // todo - can randomly use 3 in-between nodes for which you exchanged keys with
    //      - less overhead of connection establishment, although this is the most secure way to exchange keys
    //          i.e.
    //          A -> B; (no encryption) - get B key
    //          then A -> B -> C; (A->B encrypted with B key) - get C key
    //          A -> B -> C -> D; (A->B encrypted with B key, B->C encrypted with C key) - get D key
    //          - in REPLY keys are encrypted with A's public key
    //          - also can be scaled to n keys, as long as there are enough peers


    private LinkedBlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    // put in at MessageHandling if message.getId() is present in

    private boolean connection_established = false; // false until all keys are received

    private int n;  // number of in-between nodes
    private String[] connection_ids;
    private SymmetricKey[] symmetricKeys;
    private String[] addresses;
    private final String final_address;

    // Constructors
    public OnionConnection(int n, String final_dest){
        // no point in having more in-between nodes than the total amount of peers in the network
        // or using the final destination as an in-between node
        this.n = Math.min(n, PeerList.getSize() - 1);
        final_address = final_dest;

        this.symmetricKeys = new SymmetricKey[n];
        this.addresses = new String[n];
        this.connection_ids = new String[n];
    }

    public OnionConnection(String final_dest){
        // default number of in-between peers == 3
        n = 3;
        final_address = final_dest;

        this.symmetricKeys = new SymmetricKey[n];
        this.addresses = new String[n];
        this.connection_ids = new String[n];
    }
    // end Constructors



    // starts with a protocol for exchanging keys
    // then process the messages which it sent out - taking out from a queue
    @Override
    public void run() {

        // 1 - get keys
        // 2 - process messages

        Thread.currentThread().setName("OnionConnection[" + final_address + "]");

        ArrayList<String> temp = new ArrayList<>(PeerList.getAddressArrayList(final_address).subList(0, n));
        addresses = temp.toArray(new String[0]);

        // 1 - get keys
        Message message;
        for (int i = 0; i < n; i++) {

            // create KEY_EXCHANGE message
            message = Message.createKEY_EXCHANGE_REQUEST();

            // - wrap it in ONION messages
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
                return;
            }

            //  - peel off the every encryption
            for (int j = 0; j < i; j++) {
                try {
                    message = new Message(symmetricKeys[j].decrypt(message.getBody()));
                } catch (IOException e) {
                    Logger.log("Invalid inner message in Onion, during key exchange protocol!", LogLevel.ERROR);
                    return;
                }
            }


            //  - get the key
            //  - store the key
            symmetricKeys[i] = MessageHandling.handleKEY_EXCHANGE_REPLY(message);
            connection_ids[i] = message.getConnection_id();
            if (symmetricKeys[i] == null){
                // logger in the above method call
                return;
            }

        }
        connection_established = true;
        Logger.log("Successfully established OnionConnection with in-between addresses:" + Arrays.toString(addresses) + "; and final address " + final_address, LogLevel.SUCCESS);

        // 2 - process messages
        while (true){
            try {
                message = messageQueue.take();
            } catch (InterruptedException e) {
                Logger.log("Error extracting message from queue", LogLevel.WARN);
                continue;
            }
            message = decrypt(message);
            MessageHandling.handle(message, PeerList.getPeer(final_address));
        }
    }


    // whether all keys are received
    public boolean isConnection_established(){
        return connection_established;
    }

    // SEND message
    public void sendMessage(Message message){
        if (!isConnection_established()){
            return;
        }

        // todo
        //  encrypt message - using key corresponding with appropriate nextAddress and stuff
        //  save message id in OnionConnectionList, so that when REPLY is received
        //  it is added to this OC's message queue
        //  send it

        String encrypted_body = symmetricKeys[n-1].encrypt(final_address + " " + message.toString());

        message = Message.createONION_REQUEST(connection_ids[n-1], encrypted_body);
        for (int i = n-1; i > 0; i--) {
            encrypted_body = symmetricKeys[i-1].encrypt(addresses[i] + " " + message.toString());
            message = Message.createONION_REQUEST(connection_ids[i-1], encrypted_body);
        }

        MyOnionConnectionList.addRequest(message.getId(), this);
        PeerList.getPeer(addresses[0]).sendMessage(message);
    }

    private Message decrypt(int wrapCount, Message message){
        for (int i=0; i < wrapCount; i++){
            try {
                message = new Message(symmetricKeys[i].decrypt(message.getBody()));
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
    public void addMessage(Message message){
        messageQueue.add(message);
    }
}
