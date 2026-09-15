package problem1;
import java.io.*;
import java.net.*;

public class ClientHandle implements Runnable{
    private Socket socket;
    private String s_dir;
    private String clientName  = "Unknown";

    public ClientHandle(Socket socket, String s_dir){
        this.socket = socket;
        this.s_dir = s_dir;
    }

    @Override
    public void run() {
        try(
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        ) {
            clientName = in.readUTF();
            String addr = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            System.out.println("[SERVER] New client connected: " + clientName + " (" + addr + ")");

            boolean running  = true;
            while(running){
                String command = in.readUTF();
                String[] parts = command.split(" ", 2);
                String c = parts[0].toLowerCase();

                switch (c){
                    case "list":
                        handleList(out);
                        break;
                    case "get":
                         if (parts.length < 2 || parts[1].trim().isEmpty()) {
                            out.writeUTF("ERROR");
                            out.writeUTF("No filename given.");
                            out.flush();
                            System.out.println("[SERVER] Invalid GET from " + clientName + " (missing filename)");
                            } else {
                            handleget(out, parts[1].trim());
                        }
                        break;
                     case "quit":
                        out.writeUTF("BYE");
                        out.flush();
                        running = false;
                        break;

                    default:
                        out.writeUTF("ERROR");
                        out.writeUTF("Unknown command.");
                        out.flush();
                        System.out.println("[SERVER] Invalid request from " + clientName + ": '" + command + "'");
                }
                
                }
            }
         catch (IOException e) {
            System.out.println("[SERVER] Error with client " + clientName + ": " + e.getMessage());
        } finally {
            System.out.println("[SERVER] Client disconnected: " + clientName);
            try { socket.close(); } catch (IOException ignored) {}
        }
        }
        
private void handleList(DataOutputStream out) throws IOException {
        File dir = new File(s_dir);
        //String[] files = dir.list();
        File[] files = dir.listFiles(File::isFile);
        if (files == null) {
            out.writeUTF("ERROR");
            out.writeUTF("Could not list files.");
            out.flush();
            System.out.println("[SERVER] Error listing files for " + clientName);
            return;
        }

        out.writeUTF("OK");
        out.writeInt(files.length);
        for (File file : files) {
            out.writeUTF(file.getName());
            out.writeLong(file.length());
        }
        out.flush();
        System.out.println("[SERVER] Available files sent to " + clientName);
        
    }
private void handleget(DataOutputStream out, String filename) throws IOException {
        File file = new File(s_dir + filename);
        if (!file.exists() || !file.isFile()) {
            out.writeUTF("ERROR");
            out.writeUTF("File '" + filename + "' not found on server.");
            out.flush();
            System.out.println("[SERVER] File not found for " + clientName + ": " + filename);
            return;
        }

        out.writeUTF("OK");
        out.writeLong(file.length());
        System.out.println("[SERVER] Sending " + filename + " (" + file.length()+ " bytes) to " + clientName);
        try (FileInputStream f = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int r;
            while ((r = f.read(buffer)) != -1) {
                out.write(buffer, 0, r);
            }
        }
        out.flush();
        System.out.println("[SERVER] sending finish " + clientName + ": " + filename);
    }
}