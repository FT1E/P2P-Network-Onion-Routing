import Util.LogLevel;
import Util.Logger;

import java.util.Arrays;
import java.util.Scanner;

public class QueryHandler implements Runnable{


    @Override
    public void run() {

        Thread.currentThread().setName("QueryHandler");

        Scanner sc = new Scanner(System.in);


        String input;
        String[] addreses;
        OnionConnection[] onionConnections;
        while (true){

            addreses = getPeerList();
            Logger.log(formatPeerList(addreses));

            onionConnections = getOCArray();
            Logger.log(formatOCArray(onionConnections));

            input = sc.nextLine();
            processInput(input, addreses, onionConnections);
        }
//        Logger.log("No next line, exited input loop", LogLevel.DEBUG);
    }



    // methods for getting interface values + formatting them

    // just a formatted string of peer addresses

    // Peer List methods
    private String[] getPeerList(){
        return PeerList.getAddressArray(".");
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
        String output = "\n\tOnion Connection List:\n\t#\tNodes used\t\t\t\tFinal address\n";
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

        // check command
        switch (tokens[0].toUpperCase()){
            case "DSEND" -> handleDSend(input, addresses);
            case "OSEND" -> handleOSend(input, addresses, onionConnections);
            case "TRACK" -> handleTrack(tokens);
            case "CREATE" -> handleCreate(tokens[1]);
            default -> {
                Logger.log("Unknown command!", LogLevel.WARN);
                return;
            }
        }



    }
    // end processInput


    private void handleDSend(String input, String[] addresses){

        String[] tokens = input.split(" ", 4);

        if(tokens.length < 4){
            Logger.log("Too few arguments in query!", LogLevel.WARN);
            return;
        }

        // check address argument
        String address = null;
        try{
            int i = Integer.parseInt(tokens[1]);
            address = addresses[i];
        }catch (IndexOutOfBoundsException e){
            Logger.log("Index out of bounds for given peer list!", LogLevel.WARN);
            return;
        }catch (NumberFormatException e){
            address = tokens[1];
        }

        Peer peer = PeerList.getPeer(address);
        if(peer == null){
            Logger.log("No peer with given address!", LogLevel.WARN);
        }

        MessageSubType messageSubType;

        try {
            messageSubType = MessageSubType.valueOf(tokens[2].toUpperCase());
        }catch (IllegalArgumentException e){
            Logger.log("Invalid message type:" + tokens[2].toUpperCase(), LogLevel.WARN);
            return;
        }

       // at this point
       // valid command - DSEND
       // valid peer chosen
       // valid message type
       // valid length of query (len >= 4)
        // so make a REQUEST message with body = tokens[3]

        Message message = null;
        switch (messageSubType){
            case CHAT -> message = Message.createCHAT(tokens[3]);
            case GET_VAR -> message = Message.createGET_VARIABLE_REQUEST(tokens[3]);
            default -> {
                Logger.log("Invalid message type used in query:" + messageSubType.name());
                return;
            }
        }
        if (message == null) return;

        // send message directly to that peer
        peer.sendMessage(message);
    }
    // end DSEND

    // - OSEND
    private void handleOSend(String input, String[] addresses, OnionConnection[] onionConnections){

        String[] tokens = input.split(" ", 5);
        if(tokens.length < 5){
            Logger.log("Too few arguments in query!", LogLevel.WARN);
            return;
        }

        OnionConnection oc = null;

        // 2nd argument check
        // "-n" - make a new onion connection with the final address being that corresponding to the 3rd argument
        // "-o" - use an already existing onion connection, with the index corresponding to the list
        if(tokens[1].equalsIgnoreCase("-n")){

            // 3rd argument check
            // check address argument
            String address = null;
            try{
                int i = Integer.parseInt(tokens[2]);
                address = addresses[i];
            }catch (IndexOutOfBoundsException e){
                Logger.log("Index out of bounds for given peer list!", LogLevel.WARN);
                return;
            }catch (NumberFormatException e){
                address = tokens[2];
            }

            Peer peer = PeerList.getPeer(address);
            if(peer == null){
                Logger.log("No peer with given address!", LogLevel.WARN);
            }

            oc = new OnionConnection(address);

        }else if(tokens[1].equalsIgnoreCase("-o")){

            // 3rd argument check
            try{
                int i = Integer.parseInt(tokens[2]);
                oc = onionConnections[i];
            }catch (NumberFormatException | IndexOutOfBoundsException e){
                Logger.log("Index argument for OSEND -o is either not a number or out of bounds for given list", LogLevel.WARN);
                return;
            }
        }else{
            Logger.log("Invalid second argument");
            return;
        }


        // 4th argument check - message type
        MessageSubType messageSubType;

        try {
            messageSubType = MessageSubType.valueOf(tokens[3].toUpperCase());
        }catch (IllegalArgumentException e){
            Logger.log("Invalid message type:" + tokens[3].toUpperCase(), LogLevel.WARN);
            return;
        }

        Message message = null;
        switch (messageSubType){
            case CHAT -> message = Message.createCHAT(tokens[4]);
            case GET_VAR -> message = Message.createGET_VARIABLE_REQUEST(tokens[4]);
            default -> {
                Logger.log("Invalid message type used in query:" + messageSubType.name());
                return;
            }
        }
        if (message == null) return;

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
    // end OSEND

    // - TRACK
    private void handleTrack(String[] tokens){
        if(tokens.length != 2) {
            Logger.log("Only 1 argument needed for TRACK");
            return;
        }
        switch (tokens[1].toUpperCase()){
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

    // end Input processing methods
}
