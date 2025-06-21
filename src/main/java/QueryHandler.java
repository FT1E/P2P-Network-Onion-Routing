import Util.LogLevel;
import Util.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class QueryHandler implements Runnable{


    @Override
    public void run() {

        Thread.currentThread().setName("QueryHandler");

        Scanner sc = new Scanner(System.in);

        Logger.log("QueryHandler started! Press enter to see peer list and onion connection list (where you are the original sender):", LogLevel.SUCCESS);
//        Logger.log("*".repeat(50), LogLevel.DEBUG);

        String input;
        String[] addreses;
        OnionConnection[] onionConnections;

        sc.nextLine();
        while(true){

            addreses = getPeerList();
            Logger.log(formatPeerList(addreses));

            onionConnections = getOCArray();
            Logger.log(formatOCArray(onionConnections));

            if(!sc.hasNextLine()) {
                Logger.log("Scanner reached EOF, Stopping QueryHandler ...", LogLevel.DEBUG);
                break;
            }

            input = sc.nextLine();
            processInput(input, addreses, onionConnections);
        }
//        Logger.log("No next line, exited input loop", LogLevel.DEBUG);
    }



    // methods for getting interface values + formatting them

    // just a formatted string of peer addresses

    // Peer List methods
    private String[] getPeerList(){
        return PeerList.getAddressArray(".", PeerList.getSize(), false);
    }

    private String formatPeerList(String[] peerList){
        String output = "\n\tPeer List:\n\t#\tAddress\n";
        for (int i = 0; i < peerList.length; i++) {
            output += "\t" + i + "\t" + peerList[i] + "\n";
        }
        return output;
    }
    // end Peer List methods

    // My Onion Connection List methods
    private OnionConnection[] getOCArray(){
        return MyOnionConnectionList.getArray();
    }

    private String formatOCArray(OnionConnection[] oc){
        String output = "\n\tOnion Connection List: [nodes used] <Final address>\n\t#\n";
        for (int i = 0; i < oc.length; i++) {
            output += "\t" + i + "\t" + Arrays.toString(oc[i].getMiddleNodes()) + "\t" + oc[i].getFinalAddress() + "\n";
        }
        return output;
    }

    // end My Onion Connection List methods

    // end methods for getting interface values + formatting them

    // Input processing methods

    // processInput
    private void processInput(String input, String[] addresses, OnionConnection[] onionConnections){

        // Logger.log(Arrays.toString(tokens), LogLevel.DEBUG);


        String[] tokens = input.split(" ", 2);
        if(tokens.length < 2){
            return;
        }

        // check command
        switch (tokens[0].toUpperCase()){
            case "DSEND" -> handleDSend(input, addresses);
            case "OSEND" -> handleOSend(input, addresses, onionConnections);
            case "OSENDN" -> handleOSendN(input, addresses, onionConnections);
            case "TRACK" -> handleTrack(tokens[1]);
            case "CREATE" -> handleCreate(tokens[1]);
            case "CONNECT" -> handleConnect(tokens[1]);
            default -> {
                Logger.log("Unknown command!", LogLevel.WARN);
                return;
            }
        }
    }
    // end processInput


    // DSEND
    private void handleDSend(String input, String[] addresses){

        String[] tokens = input.split(" ", 4);

        if(tokens.length < 4){
            Logger.log("Query has too few arguments!", LogLevel.WARN);
            return;
        }

        // check address argument
        String address = checkAddressArgument(tokens[1], addresses);
        Peer peer = PeerList.getPeer(address);
        if(peer == null){
            return;
        }

        // check message argument
        Message message = checkMessageArgument(tokens[2], tokens[3]);
        if(message == null){
            return;
        }

        // send message directly to that peer
        peer.sendMessage(message);
    }
    // end DSEND

    // - OSEND
    private void handleOSend(String input, String[] addresses, OnionConnection[] onionConnections){

        String[] tokens = input.split(" ", 5);
        if(tokens.length < 5){
            Logger.log("Query has too few arguments!", LogLevel.WARN);
            return;
        }

        // check message arguments
        Message message = checkMessageArgument(tokens[3], tokens[4]);
        if(message == null){
            return;
        }


        OnionConnection oc;
        // 1st argument check
        // "-n" - make a new onion connection with the final address being that corresponding to the 3rd argument
        // "-o" - use an already existing onion connection, with the index corresponding to the list
        if(tokens[1].equalsIgnoreCase("-n")){
            // -n for new onion connection

            // check address argument
            String address = checkAddressArgument(tokens[2], addresses);
            if(address == null) {
                return;
            }

            try {
                oc = new OnionConnection(address);
            } catch (IOException e) {
                // logger in constructor
                return;
            }

        }else if(tokens[1].equalsIgnoreCase("-o")){
            // -o for using already established (old) connection

            // 3rd argument check - index of onion connection with the given list
            try{
                int i = Integer.parseInt(tokens[2]);
                oc = onionConnections[i];
            }catch (NumberFormatException | IndexOutOfBoundsException e){
                Logger.log("Index argument for OSEND -o is either not a number or out of bounds for given list!", LogLevel.WARN);
                return;
            }
        }else{
            Logger.log("Invalid first argument for OSEND! It can be either -n or -o (case doesn't matter)", LogLevel.WARN);
            return;
        }


        // wait for connection to be established
        while (!oc.isConnection_established()){
            Logger.log("Waiting for Onion Connection to be established ...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.log("Error in QueryHandler at currentThread.wait()", LogLevel.ERROR);
            }
        }

        // send the message with through the oc
        oc.sendMessage(message);
    }
    // end OSEND

    // - OSENDN
    private void handleOSendN(String input, String[] addresses, OnionConnection[] onionConnections){
        // note
        // if n <= 0
        //  sending the message though the onion connection will be the same as directly sending the message to the final destination
        // if n > PeerList.getSize()
        //  - n is set to PeerList.getSize() - 1
        //  - so the final destination isn't used as an in-between node
        //  - and no peer is used twice in the onion routing as an in-between node

        String[] tokens = input.split(" ", 5);
        if(tokens.length < 5){
            Logger.log("Query has too few arguments!", LogLevel.WARN);
            return;
        }

        // tokens[0] == OSENDN

        // tokens[1] == number of in-between nodes
        int n;
        try{
            n = Integer.parseInt(tokens[1]);
        }catch (NumberFormatException e){
            Logger.log("Invalid number format for number of in-between peers for OSENDN!", LogLevel.WARN);
            return;
        }


        // tokens[2] == peer index (in given peer list) / peer address of final destination for onion connection
        //  check address argument
        String address = checkAddressArgument(tokens[2], addresses);
        if(address == null){
            return;
        }

        //  check message argument
        Message message = checkMessageArgument(tokens[3], tokens[4]);
        if(message == null){
            return;
        }

        // all args are ok
        // establish onion connection
        OnionConnection oc = null;
        try {
            oc = new OnionConnection(n, address);
        } catch (IOException e) {
            // logger in constructor
            // although in this case it's probably redundant
            // since arguments were checked earlier here
            return;
        }

        while (!oc.isConnection_established()){
            Logger.log("Waiting for Onion Connection to be established ...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.log("Error in QueryHandler at currentThread.wait()", LogLevel.ERROR);
            }
        }
        oc.sendMessage(message);
    }
    // end OSENDN


    // - TRACK
    private void handleTrack(String arg){
        switch (arg.toUpperCase()){
            case "ON" -> Global.setTrack(true);
            case "OFF" -> Global.setTrack(false);
            default -> Logger.log("TRACK argument should be either ON/OFF", LogLevel.WARN);
        }
    }
    // end TRACK

    // - CREATE
    private void handleCreate(String args){
        String[] tokens = args.split(" ", 2);
        if (tokens.length < 2){
            Logger.log("Query has too few arguments!", LogLevel.WARN);
            return;
        }
        Global.addVariable(tokens[0], tokens[1]);
    }
    // end CREATE

    // - CONNECT
    private void handleConnect(String address){
        if(PeerList.getPeer(address) != null){
            Logger.log("Already connected to [" + address + "]");
            return;
        }
        address = Global.getNormalFormAddress(address);
        if(Global.getMyIp().equals(address) || "127.0.0.1".equals(address)) {
            Logger.log(address + " is your address!", LogLevel.WARN);
        }

        try {
            Peer peer = new Peer(address);
            peer.sendMessage(Message.createPEER_DISCOVERY_REQUEST());
        } catch (IOException e){
            // logger in constructor
        }
    }
    // end CONNECT

    // end Input processing methods

    // helper methods for validation
    // address argument
    private String checkAddressArgument(String address_arg, String[] addresses){
        // returns null if invalid
        // else the address received from the argument

        String address = null;
        try{
            // check if it's an index for given peer list
            int i = Integer.parseInt(address_arg);
            address = addresses[i];
        }catch (IndexOutOfBoundsException e){
            Logger.log("Index out of bounds for given peer list!", LogLevel.WARN);
            return null;
        }catch (NumberFormatException e){
            // else it's probably a literal value for address
            // like 172.16.0.2
            address = address_arg;
            // check if you're connected to that address
            Peer peer = PeerList.getPeer(address);
            if(peer == null){
                Logger.log("No peer with given address!", LogLevel.WARN);
                return null;
            }
        }

        return address;
    }

    // Message type and message argument
    private Message checkMessageArgument(String msg_type, String msg_body){
        // returns null if invalid message type
        // anything is ok for message body

        MessageSubType messageSubType;
        try {
            messageSubType = MessageSubType.valueOf(msg_type.toUpperCase());
        }catch (IllegalArgumentException e){
            Logger.log("Invalid message type:" + msg_type.toUpperCase(), LogLevel.WARN);
            return null;
        }

        Message message = null;
        switch (messageSubType){
            case CHAT -> message = Message.createCHAT(msg_body);
            case GET -> message = Message.createGET_REQUEST(msg_body);
            default -> {
                Logger.log("Invalid message type used in query:" + messageSubType.name());
                return null;
            }
        }
        return message;
    }

    // end helper methods for validation

}
