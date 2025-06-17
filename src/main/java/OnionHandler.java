import Keys.SymmetricKey;
import Util.Logger;

public class OnionHandler implements Runnable{


    private Peer waitingPeer;             // peer you need to send it back to
    private SymmetricKey key;           // key you need to encrypt the inner REPLY with
    private Message requestMessage;     // the REQUEST ONION message for which this whole thing handles

    // too many args needed - (inner reply_id, outer reply_id, peer) so I'm doing it in a separate class instead of a dictionary with a 3-4 tuple as its value


    private Message innerReply = null; // the inner reply message which will be wrapped with encryption and then send back


    // Constructors
    public OnionHandler(Message onionRequest, Peer waitingPeer){
        requestMessage = onionRequest;
        this.waitingPeer = waitingPeer;
        key = OnionKeys.get(requestMessage.getConnection_id());
    }
    public OnionHandler(Message onionRequest, Peer waitingPeer, SymmetricKey key){
        requestMessage = onionRequest;
        this.waitingPeer = waitingPeer;
        this.key = key;
    }
    // end Constructors

    public void init(Message innerReply){
        this.innerReply = innerReply;
    }

    @Override
    public void run() {
        // started when corresponding reply is received
        if(innerReply == null){
            Logger.log("Inner Reply not set! You need to init() the innerReply message in order to run OnionHandler");
            return;
        }

        // encrypt body
        String body = key.encrypt(innerReply.toString());
        // create reply with corresponding id
        Message message = Message.createONION_REPLY(requestMessage, body);
        // send it back
        waitingPeer.sendMessage(message);
    }
}
