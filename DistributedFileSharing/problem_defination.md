# Distributed File Sharing over Java Sockets

## Overview

This assignment has two problems. Problem 2 is a direct extension of Problem 1 i.e. it reuses many aspects of problem 1. You are strongly encouraged to get Problem 1 fully working before starting Problem 2, since large parts of the server- and client-side logic carry over directly.

Design your own message/protocol classes as you see fit (a small object carrying a type + payload, similar in spirit to a typical client-server "Message" class, is recommended but not required). What is graded is that your program's console input and output match the behaviour shown in the worked examples in this document — the exact wire format is your design choice.

For both problems, no need to use modern frameworks like Nio, HttpClient etc. You have to use simple java.net classes.

## How to Read the Input/Output Examples

Every worked example below is a console transcript, not a single input/output pair — these are interactive, multi-process programs, so a "test case" is a short sequence of things typed and the lines that should appear in response.

`>` at the start of a line means the user typed that line at that program's console (shown here in a shaded row). Everything else is output from the application.

Each printed line is prefixed with the process that printed it, e.g. `[SERVER]`, `[CLIENT]`, `[TRACKER]`, `[NODE-A]` — your exact wording may differ, but the information conveyed and the overall sequence must match.

Where multiple processes are involved, the transcript is split by process so you can see what each console shows at that point in the exchange.

---

## Problem 1 — Multi-Client File Request Server

### Scenario

You will build a single file server that a shared folder of files lives on, and any number of clients that connect to it over TCP to browse and download those files. Multiple clients must be able to connect and issue requests at the same time without one client's activity blocking another's.

### Requirements

#### Server

- Listens on a fixed TCP port and accepts connections using `java.net.ServerSocket`.
- Serves files from a designated local directory (e.g. `./server_files`).
- Spawns one dedicated thread per connected client to read and respond to that client's requests — a client must never be blocked waiting on another client's request or transfer to finish.
- Supports three request types from a connected client: list available files, get (download) a named file, and a graceful disconnect.
- Prints a log line for every significant event: a client connecting, a file being listed, a file transfer starting/finishing, an invalid request, and a client disconnecting.

#### Client

- Connects to the server and registers with a name typed by the user at startup.
- Reads commands from the console in a loop and sends the corresponding request to the server.
- Displays the server's response for each command, and for a successful get, saves the downloaded file under a local `./downloads` folder using its original filename.
- Reports a clear error message for any command the server rejects, and for any command the client itself does not recognise, without crashing or disconnecting.

### Command Reference

| Command | Description |
|---|---|
| `list` | Ask the server for the list of files currently available in its shared directory, with each file's size in bytes. |
| `get <filename>` | Download the named file from the server and save it locally. Fails with an error if the file does not exist on the server. |
| `quit` | Disconnect cleanly from the server and exit the client program. |

### Validations

- An unrecognised command must print an error and let the user keep typing further commands — it must not crash the client or close the connection.
- A get for a filename that is not in the server's shared directory must not crash the server, and must return a clear error to the requesting client only.
- The server must remain fully responsive to other clients while one client is mid-download (see Case 6).
- A downloaded file's contents must be byte-for-byte identical to the original on the server.

### Input / Output Examples

Setup for all test cases below: the server is started with shared directory `./server_files` containing `notes.txt`, `photo.jpg` and `report.pdf`.

Case 1–5 shows only the client's console, you must also print appropriate logs for each event on the server's console as well in order to keep track.

#### Case 1 — Successful list

```
> list
[SERVER] Available files:
  1. notes.txt (2350 bytes)
  2. photo.jpg (154302 bytes)
  3. report.pdf (88210 bytes)
```

#### Case 2 — Successful get (file exists)

```
> get notes.txt
[SERVER] Sending notes.txt (2350 bytes)...
[CLIENT] Download complete: notes.txt saved to ./downloads/notes.txt (2350 bytes)
```

#### Case 3 — get on a file that does not exist

```
> get missing.txt
[SERVER] ERROR: File 'missing.txt' not found on server.
```

#### Case 4 — Invalid / unrecognised command

```
> download notes.txt
[CLIENT] ERROR: Unknown command 'download'. Valid commands: list, get <filename>, quit
```

#### Case 5 — Graceful disconnect

```
> quit
[CLIENT] You are successfully disconnected.
```

#### Case 6 — Concurrent clients (threading requirement)

Note: Alice and Bob are connected at the same time. Alice starts downloading a large file; while that transfer is still in progress, Bob issues `list`. Bob's response must arrive immediately — it must not wait for Alice's transfer to finish.

**— Server console (order of these lines may vary) —**

```
[SERVER] New client connected: Alice (127.0.0.1:54321)
[SERVER] New client connected: Bob (127.0.0.1:54322)
[SERVER] Sending photo.jpg (154302 bytes) to Alice...
[SERVER] Available files sent to Bob.
[SERVER] Finished sending photo.jpg to Alice.
```

**— Alice's console —**

```
> get photo.jpg
[SERVER] Sending photo.jpg (154302 bytes)...
[CLIENT] Download complete: photo.jpg saved to ./downloads/photo.jpg (154302 bytes)
```

**— Bob's console (typed while Alice's download above is still running) —**

```
> list
[SERVER] Available files:
  1. notes.txt (2350 bytes)
  2. photo.jpg (154302 bytes)
  3. report.pdf (88210 bytes)
```

Note: Bob's list response must print well before Alice's download finishes — this is what demonstrates your one-thread-per-client design actually works.

---

## Problem 2 — Distributed File Sharing with Parallel Chunked Downloads

### Scenario

A single file server is a bottleneck and a single point of failure. In this problem you will extend Problem 1 into a small distributed system with three kinds of programs: one tracker server that only keeps track of which files live on which file-hosting nodes; multiple file-hosting nodes, each essentially a Problem-1 server with its own shared directory; and a client that can locate a file across the network, download it in parallel chunks from potentially different nodes at once, upload a new file into the network, and keep working even if one of the nodes it was using disappears mid-download.

Everything from Problem 1 that still applies carries over unchanged: one thread per connection on every server-side process, console-driven commands on the client, and clear error handling instead of crashes.

### Architecture

Nodes register their file lists with the tracker on startup (and again whenever their file list changes). The client only ever asks the tracker "who has this file" — it never asks a node to search for anything — then talks to the returned node(s) directly to move the actual file bytes.

### Requirements

#### Tracker Server

- Listens on a fixed TCP port, separate from every node's port.
- Maintains a registry mapping filename -> list of (node address, file size) for every file any currently-registered node hosts.
- Handles a node's register request by adding/updating that node's entry, and a node's update request the same way (e.g. after it uploads a new file).
- Handles a client's `find <filename>` request by returning every node currently hosting that file, or a clear "not found" response if none do.
- Handles a client's `list` request by returning the full catalogue: every known filename, its size, and which node(s) host it.
- Handles each connected node and client on its own thread, exactly like the server in Problem 1.

#### File-Hosting Node

- Behaves like the Problem 1 server for its own shared directory, plus it can serve a byte-range of a file (a "chunk"), not only the whole file, so multiple chunk requests for the same file can be served concurrently.
- Registers its full file list with the tracker on startup, including each file's size.
- Accepts an upload request from a client: receives the file's bytes, saves them into its own shared directory, then notifies the tracker that its file list has changed.
- Handles every connected client on its own thread, and must be able to serve more than one chunk request from multiple client at the same time (this is what makes parallel chunked downloads possible).

#### Client

- Connects to the tracker using an address given at startup, and registers a name, exactly as in Problem 1.
- For a `get <filename>`, first asks the tracker which node(s) host the file and how large it is, then splits the file into 4 equal-sized byte-range chunks (the last chunk absorbs any remainder if the size does not divide evenly), assigns the chunks round-robin across the hosting node(s), and downloads all chunks concurrently — one thread per chunk — writing each chunk directly to its correct offset in the local output file.
- For an `upload <nodeAddress> <localFilePath>`, sends the local file's bytes directly to the specified node and reports success or failure.

### Command Reference

| Command | Description |
|---|---|
| `list` | Ask the tracker for the entire file catalogue currently known across all registered nodes. |
| `find <filename>` | Ask the tracker which node(s) currently host the named file, and its size. |
| `get <filename>` | Download the named file in parallel chunks, from one or more nodes in round-robin style and reassemble it locally. |
| `upload <host:port> <path>` | Send a local file to the specified node so it becomes available on the network. |
| `quit` | Disconnect cleanly from the tracker and exit the client program. |

### Validations

- A find or get for a filename no registered node hosts must return a clear error, not an empty crash or a hang.
- Concurrent chunk writes into the same local output file must not corrupt one another — write each chunk to its own byte offset (e.g. using `RandomAccessFile` / positional writes) rather than relying on writes happening in order.
- An unrecognised client command must print an error and allow the user to keep typing further commands.

### Input / Output Examples

Setup for all cases below:

Node-A (127.0.0.1:7001, shared dir `./nodeA_files`) and Node-B (127.0.0.1:7002, shared dir `./nodeB_files`) have both already registered with the tracker (127.0.0.1:6000). Both nodes host `movie.mp4` (2,097,152 bytes) and `album.zip` (800,000 bytes). Node-A additionally hosts `notes.txt`. The client (name: Alice) is already connected to the tracker.

#### Case 1 — find — file available on multiple nodes

```
> find movie.mp4
[TRACKER] movie.mp4 (2097152 bytes) is available on 2 node(s):
  1. Node-A (127.0.0.1:7001)
  2. Node-B (127.0.0.1:7002)
```

#### Case 2 — find — file not hosted anywhere

```
> find ghost.mp4
[TRACKER] ERROR: No node currently hosts 'ghost.mp4'.
```

#### Case 3 — get — successful parallel multi-node download

```
> get movie.mp4
[CLIENT] movie.mp4 found on 2 node(s); downloading in 4 chunk(s).
[CLIENT] Chunk 1 (bytes 0-524287)        -> Node-A (127.0.0.1:7001)
[CLIENT] Chunk 2 (bytes 524288-1048575)  -> Node-B (127.0.0.1:7002)
[CLIENT] Chunk 3 (bytes 1048576-1572863) -> Node-A (127.0.0.1:7001)
[CLIENT] Chunk 4 (bytes 1572864-2097151) -> Node-B (127.0.0.1:7002)
[CLIENT] Chunk 2 complete (524288 bytes) from Node-B
[CLIENT] Chunk 1 complete (524288 bytes) from Node-A
[CLIENT] Chunk 4 complete (524288 bytes) from Node-B
[CLIENT] Chunk 3 complete (524288 bytes) from Node-A
[CLIENT] All chunks received. movie.mp4 reassembled -> ./downloads/movie.mp4 (2097152 bytes)
```

Note: The order in which chunks report 'complete' depends on thread scheduling and will vary between runs — only the final reassembled file and its size are checked.

#### Case 4 — upload — publish a new file to a node

**— Alice's console —**

```
> upload 127.0.0.1:7001 ./local_files/song.mp3
[CLIENT] Uploading song.mp3 (96000 bytes) to node 127.0.0.1:7001...
[CLIENT] Upload complete. Node 127.0.0.1:7001 now hosts song.mp3.
```

**— Node-A's console —**

```
[NODE-A] Receiving upload 'song.mp3' (96000 bytes) from Alice...
[NODE-A] Saved song.mp3 to ./nodeA_files/song.mp3
[NODE-A] Notifying tracker of updated file list...
```

**— Tracker's console —**

```
[TRACKER] Node-A (127.0.0.1:7001) file list updated -> added: song.mp3
```

**— Alice's console, immediately afterward —**

```
> find song.mp3
[TRACKER] song.mp3 (96000 bytes) is available on 1 node(s):
  1. Node-A (127.0.0.1:7001)
```

#### Case 5 — Invalid / unrecognised command

```
> download movie.mp4
[CLIENT] ERROR: Unknown command 'download'. Valid commands: list, find <filename>, get <filename>, upload <host:port> <path>, quit
```
