package problem2;

public class FileEntry {
    String filename;
    String node;
    String host;
    int port;
    long size;

    public FileEntry(String filename, String node, String host, int port, long size) {
        this.filename = filename;
        this.node = node;
        this.host = host;
        this.port = port;
        this.size = size;
    }

    public String nodeAddress() {
        return host + ":" + port;
    }
}
