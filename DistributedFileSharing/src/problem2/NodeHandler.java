package problem2;
import java.io.*;
import java.net.*;

public class NodeHandler implements Runnable{
    Socket socket;
    String sdir;
    String label;
    int port;

    public NodeHandler(Socket socket, String sdir, String label, int port){
        this.socket = socket;
        this.sdir = sdir;
        this.label = label;
        this.port = port;
    }

    public void run(){
        try{
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            String cm = in.readUTF();
            String[] part = cm.split(" ");
            if (part[0].equalsIgnoreCase("GETCHUNK")) {
                String fn = part[1];
                long start = Long.parseLong(part[2]);
                long end = Long.parseLong(part[3]);
                handleChunk(out, fn, start, end);
            }
            else if (part[0].equalsIgnoreCase("UPLOAD")){
                
                String fn = part[1];
                long size = Long.parseLong(part[2]);
                handleUpload(in, out, fn, size);
            }
            else{
                out.writeUTF("ERROR");
                out.writeUTF("Unknown req");
                out.flush();
            }
            in.close();
            out.close();

        } catch(IOException e ){
            System.out.println("[" + label + "] Error: " + e.getMessage());
        } finally{
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("[" + label + "] Error closing socket: " + e.getMessage());
            }
        }
    }
    

    private void handleChunk(DataOutputStream out, String fn, long start, long end) throws IOException {
        File f = new File(sdir + fn);
        if (!f.exists()) {
            out.writeUTF("ERROR");
            out.writeUTF("File not found");
            out.flush();
            return;
        }
        long length = end - start + 1 ;
        out.writeUTF("ok");
        out.writeLong(length);
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        raf.seek(start);
        byte[] buffer = new byte[4096];
        long remaining = length;
        while (remaining > 0) {
            int toRead = 4096;
            if (remaining < toRead) {
                toRead = (int) remaining;
            }
            int actuallyRead = raf.read(buffer, 0, toRead);
            if (actuallyRead == -1) {
                break;
            }
            out.write(buffer, 0, actuallyRead);
            remaining = remaining - actuallyRead;
        }

        raf.close();
        out.flush();
        }

    private void handleUpload(DataInputStream in, DataOutputStream out, String fn, long size) throws IOException {
        File f = new File(sdir + fn);
        FileOutputStream fos = new FileOutputStream(f);

        byte[] buffer = new byte[4096];
        long remaining = size;
        while (remaining > 0) {
            int toRead = 4096;
            if (remaining < toRead) {
                toRead = (int) remaining;
            }
            int actuallyRead = in.read(buffer, 0, toRead);
            if (actuallyRead == -1) {
                break;
            }
            fos.write(buffer, 0, actuallyRead);
            remaining = remaining - actuallyRead;
        }
        fos.close();
        System.out.println("[" + label + "] Saved " + fn + " to " + sdir + fn);
        out.writeUTF("ok");
        out.flush();

        Node.registerWithTracker(label, port, sdir);
    }
}

 
