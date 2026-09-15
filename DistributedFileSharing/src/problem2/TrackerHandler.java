package problem2;
import java.io.*;
import java.net.*;
import java.util.*;

public class TrackerHandler implements Runnable {
    private Socket socket;
    private String name = "Unknown";

    public TrackerHandler(Socket socket){
        this.socket = socket;
    }

    public  void run(){
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String role = in.readUTF();
            if(role.equals("NODE")){
                handleNode(in, out);
            }
            else if (role.equals("CLIENT")){
                handleClient(in, out);
            }
            else{
                out.writeUTF("ERROR");
                out.writeUTF("Unknown role");
                out.flush();
            }
            in.close();
            out.close();
        } catch(IOException e){
            System.err.println("[TRACKER] Error with " + name+ ":"+ e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("[TRACKER] Error closing socket: " + e.getMessage());
            }
        }
    }

    private static synchronized void handleNode(DataInputStream in, DataOutputStream out) throws IOException {
        String label = in.readUTF();
        String host = in.readUTF();
        int port = in.readInt();

        int fileCount = in.readInt();
        List<String> names = new ArrayList<String>();
        List<Long> sizes = new ArrayList<Long>();
        for (int i = 0; i < fileCount; i++) {
            names.add(in.readUTF());
            sizes.add(in.readLong());
        }

        String nodeAddress = host + ":" + port;
        List<String> oldNames = new ArrayList<String>();
        for (int i = 0; i < Tracker.catalogue.size(); i++) {
            FileEntry e = Tracker.catalogue.get(i);
            if (e.nodeAddress().equals(nodeAddress)) {
                oldNames.add(e.filename);
            }
        }

        int i = 0;
        while (i < Tracker.catalogue.size()) {
            FileEntry e = Tracker.catalogue.get(i);
            if (e.nodeAddress().equals(nodeAddress)) {
                Tracker.catalogue.remove(i);
            } 
            else {
                i = i + 1;
            }
        }

        List<String> newlyAdded = new ArrayList<String>();
        for (int j = 0; j < names.size(); j++) {
            String name = names.get(j);
            long size = sizes.get(j);

            Tracker.catalogue.add(new FileEntry(name, label, host, port, size));

            if (!oldNames.contains(name)) {
                newlyAdded.add(name);
            }
       
        }
        out.writeUTF("ok");
        out.flush();

        String addedtxt = "(none)";
        if (newlyAdded.size() > 0) {
            addedtxt = "";
            for (int j = 0; j < newlyAdded.size(); j++) {
                if (j > 0) {
                    addedtxt = addedtxt + ", ";
                }
                addedtxt = addedtxt + newlyAdded.get(j);
            }
        }

        System.out.println("[TRACKER] " + label + " (" + nodeAddress + ") file list updated -> added: " + addedtxt);
    }

    private void handleClient(DataInputStream in, DataOutputStream out) throws IOException {
        String nme = in.readUTF();
        name = nme;
        System.out.println("[TRACKER] Client Connected " + nme);
        boolean r = true;
        while (r) {
            String cm = in.readUTF();
            String[] parts = cm.split(" ", 2);
            String c = parts[0].toUpperCase();

            if(c.equals("LIST")){
                handlelist(out);
            }
            else if (c.equals("FIND")){
                if(parts.length < 2  || parts[1].trim().equals("")){
                    out.writeUTF("ERROR");
                    out.writeUTF("No Filename given");
                    out.flush();
                }
                else{
                    String f= parts[1].trim();
                    handleFind(out, f);
                }
            }
            else if(c.equals("QUIT")){
                r = false;
            }
            else{
                out.writeUTF("ERROR");
                out.writeUTF("Unknown request");
                out.flush();
            }
        }
        System.out.println("[TRACKER] Client Disconnected " + nme);
    }

    private static synchronized void handleFind(DataOutputStream out, String filename) throws IOException {
        List<FileEntry> match = new ArrayList<FileEntry>();
        for(int i = 0; i < Tracker.catalogue.size(); i++){
            FileEntry e = Tracker.catalogue.get(i);
            if(e.filename.equals(filename)){
                match.add(e);
            }
        }
        if(match.size() == 0){
            out.writeUTF("ERROR");
            out.writeUTF("No node currently hosts " + filename);
            out.flush();
            return;
        }
        out.writeUTF("ok");
        out.writeUTF(filename);
        out.writeLong(match.get(0).size);
        out.writeInt(match.size());
        for(int i = 0; i < match.size(); i++){
            FileEntry e = match.get(i);
            out.writeUTF(e.node);
            out.writeUTF(e.host);
            out.writeInt(e.port);
        }
        out.flush();
    }
    private static synchronized void handlelist(DataOutputStream out) throws IOException{
        List<String> files = new ArrayList<String>();
        for(int i = 0; i< Tracker.catalogue.size(); i++){
            String fn = Tracker.catalogue.get(i).filename;
            if(!files.contains(fn)){
                files.add(fn);
            }
        }
        out.writeUTF("ok");
        out.writeInt(files.size());
        for (int i = 0; i< files.size(); i++){
            String fna = files.get(i);
            List<FileEntry> match = new ArrayList<FileEntry>();
            for(int j = 0; j < Tracker.catalogue.size(); j++){
                FileEntry e = Tracker.catalogue.get(j);
                if(e.filename.equals(fna)){
                    match.add(e);
                }
            }
            out.writeUTF(fna);
            out.writeLong(match.get(0).size);
            out.writeInt(match.size());
            for(int j = 0; j < match.size(); j++){
                FileEntry e = match.get(j);
                out.writeUTF(e.node);
                out.writeUTF(e.host);
                out.writeInt(e.port);
            }
        }
        out.flush();
    }

}
