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
 *
	  The application server accepts orders for lego sets from the client
		o When an order is received, the application server accesses the inventory_sets table to see if a set is available.
			If so, it reduces the inventory_sets.quantity of the set by one and replies to the customer with a message that
			the set has been shipped
		o If inventory_sets.quantity < the number of that set the customer ordered (e.g. the customer ordered 5 lego
			police cars and inventory_sets.quantity for the police car set is < 5), the application server checks the
			inventory_parts table to see if there is enough inventory of all the parts in the set to assemble enough sets to
			fulfill the order. If so, it assembles the sets by decrementing the inventory_ parts.quantity by the amount
			needed and replies to the customer that the sets have shipped.
		o If there are not enough parts, the application server:
			 Sends a message to the client that the set is backordered
			 creates a timer thread which counts 100 milliseconds for the required parts to be manufactured.
				When the 100 milliseconds are up, the inventory_ parts.quantity for the part is incremented by 30.
			 Once all of a given order’s manufacturing timers are done, the application server tries again to fill the
				order.
			 When an order is filled, the server will include an order shipped message to the client

 */

public class Server
{
	// Only 25 unshipped orders at a time. Use a threadpool to enforce this, and to allow concurrency.
	private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(25, 25, 1, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.AbortPolicy());
	private final ThreadPoolExecutor manufacturePartsThreadPool = new ThreadPoolExecutor(100, 100, 1, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.DiscardPolicy());
	private final DBManager db = new DBMDummyImpl();
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
			for (int sessionNum = 0; true; sessionNum++) {
				try {
					Socket incoming = ssc.accept();
					System.out.println("New Connection: Session#" + sessionNum);
					new Thread(new ConnectionManager(incoming, sessionNum)).start();
				} catch (IOException e) {
					System.err.println("Caught exception while creating socket connection to client.");
					System.err.println(e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			System.err.println("Caught error while creating ServerSocket");
			System.err.println(e.getMessage());
			e.printStackTrace();
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

				while (true) {
					try {
						Request request = (Request) fromClient.readObject();

						System.out.println("Received request: " + request);

						try {
							threadPool.execute(new RequestHandler(request, db, manufacturePartsThreadPool, lockHandler, toClient));
						}catch (RejectedExecutionException e) {
							toClient.println("Request Rejected: 25 requests outstanding" + request.toString());
						}
					}  catch (EOFException e) {
						break;
					} catch (ClassNotFoundException e) {
						System.out.println("Caught ClassNotFoundException: " + e.getMessage());
						toClient.println("Error: Could not find class.");
						break;
					}
				}
			} catch (SocketException e) {
				System.out.println("Session#" + sessionNum + " disconnected");
			} catch (IOException e) {
				e.printStackTrace(); //TODO
			} finally {
				try {
					this.socket.close();
				} catch (IOException e) {
					e.printStackTrace(); //TODO
				}
			}

		}
	}
}