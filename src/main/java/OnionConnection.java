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

    private boolean connection_established = false; // false until all keys are received

    private int n;  // number of in-between nodes
    private String[] connection_ids;
    private SymmetricKey[] symmetricKeys;
    private String[] addresses;
    private final String final_address;

    // Constructor
    public OnionConnection(int n, String final_dest){
        // no point in having more in-between nodes than the total amount of peers in the network
        // or using the final destination as an in-between node
        this.n = Math.min(n, PeerList.getSize() - 1);
        final_address = final_dest;
    }

    public OnionConnection(String final_dest){
        // default number of in-between peers == 3
        n = 3;
        final_address = final_dest;
    }
    // end Constructor


    // protocol for exchanging keys
    @Override
    public void run() {
        // todo
        //      - get keys

        connection_established = true;
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
        //  send it


    }

}
