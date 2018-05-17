package ourTeam;

import java.net.SocketException;
import java.util.concurrent.*;

import java.io.IOException;
import java.io.EOFException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.ObjectInputStream;

import java.net.ServerSocket;
import java.net.Socket;

/*
 * Waits for connections from the client. Supports mutltiple connections. Runs threads (max 25) handing requests
 * 	with RequestHandlers running in the threads.
 */

public class Server
{
	// Only 25 unshipped orders at a time. Use a threadpool to enforce this, and to allow concurrency.
	private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(25, 25, 1, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.AbortPolicy());
	private final ThreadPoolExecutor manufacturePartsThreadPool = new ThreadPoolExecutor(100, 100, 1, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.DiscardPolicy());
	private final DBManager db = new DBManagerImpl();
	private final DBLockHandler lockHandler = new DBLockHandler();

	public static void main(String[] args) {
		Server s = new Server();
		s.listenForRequests();
	}

	public Server() {
		this.threadPool.prestartAllCoreThreads();
		this.manufacturePartsThreadPool.prestartAllCoreThreads();
	}

	/*
	 * Create a thread that listens for client requests.
	 * When a request is received, put it on the queue.
	 * If there is no room on the queue, reject the request.
	 */
	private void listenForRequests() {
		//create ServerSocket
		try (ServerSocket ssc = new ServerSocket(8189)) {
			//create Socket

			try {
				Socket incoming = ssc.accept();
				for (int sessionNum = 0; incoming != null; sessionNum++) {
						System.out.println("New Connection: Session#" + sessionNum);
						Thread connectionManager = new Thread(new ConnectionManager(incoming, sessionNum));
						connectionManager.setDaemon(true);
						connectionManager.start();
						incoming = ssc.accept();

				}
		} catch (IOException e) {
			System.err.println("Caught exception while creating socket connection to client.");
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		} catch (IOException e) {
			System.out.println("Server already running");
		}
	}


	/*
	 * ***** THE REST OF THE CODE IN THIS CLASS IS JUST THE ConnectionManager CLASS *****
	 */

	/*
	 * This class takes a socket and manages the connection/requests of this client instance.
	 * Refactoring this out into its own class allows for multiple client instances to connect to the server concurrently
	 */
	class ConnectionManager implements Runnable {
		Socket socket;
		int sessionNum;
		Server parent;

		ConnectionManager(Socket socket, int sessionNum) {
			this.socket = socket;
			this.sessionNum = sessionNum;

		}

		@Override
		public void run() {
			try {
				ObjectInputStream fromClient = new ObjectInputStream(this.socket.getInputStream());
				PrintWriter toClient = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream(), "UTF-8"), true);

				toClient.println("Server: Connected, Session #" + this.sessionNum);
				boolean endCondition = true;

				try {
					Request request = (Request) fromClient.readObject();
					while (request != null) {

							System.out.println("Received request: " + request);
							if(request.getName().equals("quit"))
								break;
							try {
								threadPool.execute(new RequestHandler(request, db, manufacturePartsThreadPool, lockHandler, toClient));
							}catch (RejectedExecutionException e) {
								toClient.println("Request Rejected: 25 requests outstanding" + request.toString());
							}
							request = (Request) fromClient.readObject();

					}
				}  catch (EOFException e) {

				} catch (ClassNotFoundException e) {
					System.out.println("Caught ClassNotFoundException: " + e.getMessage());
					toClient.println("Error: Could not find class.");

				}
			} catch (SocketException e) {
				System.out.println("Session#" + sessionNum + " disconnected");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					this.socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}