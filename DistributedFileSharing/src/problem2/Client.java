package problem2;
import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    private static final String host = "127.0.0.1";
    private static final int tport = 6000;
    private static final String  ddir = "./downloads/";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("name: ");
        String name = sc.nextLine().trim();
        File d = new File(ddir);
        if(!d.exists()){
            d.mkdir();
        }
        try{
            Socket socket = new Socket(host, tport);
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.writeUTF("CLIENT");
            out.writeUTF(name);
            out.flush();
            System.out.println("[CLIENT] Connected to tracker at " + name);
            boolean r = true;
            while(r){
                String l = sc.nextLine().trim();
                if (l.equals("")) {
                    continue;
                }
                String[] parts = l.split(" ", 2);
                String c = parts[0].toLowerCase();
                
                if (c.equals("list")) {
                    dolist(in,out);                    
                }
                else if(c.equals("find")){
                    if (parts.length < 2) {
                        System.out.println("[CLIENT] ERROR: No filename given.");
                    } else {
                        doFind(in, out, parts[1].trim());
                    }
                }
                else if(c.equals("get") ){
                    if (parts.length < 2) {
                        System.out.println("[CLIENT] ERROR: No filename given.");
                    } else {
                        doGet(in, out, parts[1].trim());
                    }
                }
                else if(c.equals("upload")){
                    if (parts.length < 2) {
                        System.out.println("[CLIENT] ERROR: Usage: upload <host:port> <path>");
                    } else {
                        String[] uploadArgs = parts[1].trim().split(" ", 2);
                        if (uploadArgs.length < 2) {
                            System.out.println("[CLIENT] ERROR: Usage: upload <host:port> <path>");
                        } else {
                            doupload(uploadArgs[0].trim(), uploadArgs[1].trim());
                        }
                    }
                }
                else if (c.equals("quit")) {
                    out.writeUTF("QUIT");
                    out.flush();
                    System.out.println("[CLIENT] You are successfully disconnected.");
                    r = false;
                }
                else{
                    System.out.println("[CLIENT] ERROR: Unknown command " + c + "Valid commands: list, find <filename>, get <filename>, upload <host:port> <path>, quit");
                }
            }
            in.close();
            out.close();
            socket.close();
        } catch (IOException e){
            System.out.println("[CLIENT] Error: "+ e.getMessage());
        }
    }
    private static void dolist (DataInputStream in, DataOutputStream out) throws IOException{
        out.writeUTF("LIST");
        out.flush();
        String res = in.readUTF();
        if(!res.equals("ok")){
           System.out.println("[TRACKER] " + in.readUTF());
            return; 
        }
        int filecount = in.readInt();
         if (filecount == 0) {
            System.out.println("[TRACKER] No files are currently available on the network.");
            return;
        }
        System.out.println("[TRACKER] Full catalogue:");
        for (int i = 0; i < filecount; i++) {
            String filename = in.readUTF();
            long size = in.readLong();
            int node = in.readInt();

            String nodestxt = "";
            for (int j = 0; j < node; j++) {
                String label = in.readUTF();
                String host = in.readUTF();
                int port = in.readInt();
                if (j > 0) {
                    nodestxt = nodestxt + ", ";
                }
                nodestxt = nodestxt + label + " (" + host + ":" + port + ")";
            }

            System.out.println("  " + (i + 1) + ". " + filename + " (" + size + " bytes) -> " + nodestxt);
        }

    }

    private static void doupload(String nodeAddress, String path){
        String[] hostPort = nodeAddress.split(":");
            if (hostPort.length != 2) {
            System.out.println("[CLIENT] ERROR: Node address must look like host:port");
            return;
        }

        String host = hostPort[0];
        int port;
        try {
            port = Integer.parseInt(hostPort[1]);
        } catch (NumberFormatException e) {
            System.out.println("[CLIENT] ERROR: Node address must look like host:port");
            return;
        }
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("[CLIENT] ERROR: Local file not found: " + path);
            return;
        }
                String filename = file.getName();
        long size = file.length();
        System.out.println("[CLIENT] Uploading " + filename + " (" + size + " bytes) to node " + nodeAddress + "...");

        try {
            Socket socket = new Socket(host, port);
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            FileInputStream fis = new FileInputStream(file);

            out.writeUTF("UPLOAD " + filename + " " + size);

            byte[] buffer = new byte[4096];
            int actuallyRead;
            while ((actuallyRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, actuallyRead);
            }
            out.flush();

            String response = in.readUTF();
            if (response.equals("ok")) {
                System.out.println("[CLIENT] Upload complete. Node " + nodeAddress + " now hosts " + filename + ".");
            } else {
                String errorMsg = in.readUTF();
                System.out.println("[CLIENT] Upload failed: " + errorMsg);
            }

            fis.close();
            in.close();
            out.close();
            socket.close();
        }catch (IOException e) {
            System.out.println("[CLIENT] Upload failed: " + e.getMessage());
        }
    }

    private static void doFind(DataInputStream in, DataOutputStream out, String filename) throws IOException {
        out.writeUTF("FIND " + filename);
        out.flush();

        String response = in.readUTF();
        if (!response.equals("ok")) {
            System.out.println("[TRACKER] ERROR: " + in.readUTF());
            return;
        }

        in.readUTF(); 
        long size = in.readLong();
        int nodeCount = in.readInt();

        System.out.println("[TRACKER] " + filename + " (" + size + " bytes) is available on " + nodeCount + " node(s):");
        for (int i = 0; i < nodeCount; i++) {
            String label = in.readUTF();
            String host = in.readUTF();
            int port = in.readInt();
            System.out.println("  " + (i + 1) + ". " + label + " (" + host + ":" + port + ")");
        }
    }

    private static void doGet(DataInputStream in, DataOutputStream out, String filename) throws IOException {
        out.writeUTF("FIND " + filename);
        out.flush();

        String response = in.readUTF();
        if (!response.equals("ok")) {
            System.out.println("[TRACKER] ERROR: " + in.readUTF());
            return;
        }

        in.readUTF();
        long totalSize = in.readLong();
        int nodeCount = in.readInt();

        List<String> nodeLabels = new ArrayList<String>();
        List<String> nodeHosts = new ArrayList<String>();
        List<Integer> nodePorts = new ArrayList<Integer>();
        for (int i = 0; i < nodeCount; i++) {
            nodeLabels.add(in.readUTF());
            nodeHosts.add(in.readUTF());
            nodePorts.add(in.readInt());
        }
        int chunkCount = 4;
        long baseChunkSize = totalSize / chunkCount;

        long[] startPositions = new long[chunkCount];
        long[] endPositions = new long[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            startPositions[i] = i * baseChunkSize;
            if (i == chunkCount - 1) {
                endPositions[i] = totalSize - 1; 
            } else {
                endPositions[i] = startPositions[i] + baseChunkSize - 1;
            }
        }

        System.out.println("[CLIENT] " + filename + " found on " + nodeCount + " node(s); downloading in " + chunkCount + " chunk(s).");

        File outFile = new File(ddir + filename);

        RandomAccessFile sizerRaf = new RandomAccessFile(outFile, "rw");
        sizerRaf.setLength(totalSize);
        sizerRaf.close();

        Thread[] threads = new Thread[chunkCount];

        for (int i = 0; i < chunkCount; i++) {
            final int chunkNumber = i + 1;
            final long start = startPositions[i];
            final long end = endPositions[i];

            int nodeIndex = i % nodeCount; 
            final String nodeLabel = nodeLabels.get(nodeIndex);
            final String nodeHost = nodeHosts.get(nodeIndex);
            final int nodePort = nodePorts.get(nodeIndex);
            final String finalFilename = filename;
            final File finalOutFile = outFile;

            System.out.println("[CLIENT] Chunk " + chunkNumber + " (bytes " + start + "-" + end + ") -> "
                    + nodeLabel + " (" + nodeHost + ":" + nodePort + ")");

            Runnable job = new Runnable() {
                public void run() {
                    downloadChunk(finalFilename, chunkNumber, start, end, nodeHost, nodePort, nodeLabel, finalOutFile);
                }
            };

            threads[i] = new Thread(job);
            threads[i].start(); 
        }
       
        for (int i = 0; i < chunkCount; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                System.err.println("[CLIENT] Error: " + e.getMessage());
            }
        }

        System.out.println("[CLIENT] All chunks received. " + filename + " reassembled -> " + ddir + filename + " (" + totalSize + " bytes)");
    }

    private static void downloadChunk(String filename, int chunkNumber, long start, long end,
                                       String host, int port, String label, File outFile) {
        try {
            Socket socket = new Socket(host, port);
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            out.writeUTF("GETCHUNK " + filename + " " + start + " " + end);
            out.flush();

            String response = in.readUTF();
            if (!response.equals("ok")) {
                String errorMsg = in.readUTF();
                System.out.println("[CLIENT] Chunk " + chunkNumber + " FAILED from " + label + ": " + errorMsg);
                in.close();
                out.close();
                socket.close();
                return;
            }

            long length = in.readLong();
            RandomAccessFile raf = new RandomAccessFile(outFile, "rw");
            raf.seek(start);

            byte[] buffer = new byte[4096];
            long remaining = length;
            while (remaining > 0) {
                int howMuchToRead = 4096;
                if (remaining < howMuchToRead) {
                    howMuchToRead = (int) remaining;
                }
                int actuallyRead = in.read(buffer, 0, howMuchToRead);
                if (actuallyRead == -1) {
                    break;
                }
                raf.write(buffer, 0, actuallyRead);
                remaining = remaining - actuallyRead;
            }

            raf.close();
            in.close();
            out.close();
            socket.close();

            System.out.println("[CLIENT] Chunk " + chunkNumber + " complete (" + length + " bytes) from " + label);

        } catch (IOException e) {
            System.out.println("[CLIENT] Chunk " + chunkNumber + " FAILED from " + label + ": " + e.getMessage());
        }
    }
 
}
