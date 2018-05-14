import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Scanner;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
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
	private ThreadPoolExecutor threadPool;
	private ThreadPoolExecutor manufacturePartsThreadPool;
	DBManager db;

	public static void main(String[] args) {
		Server s = new Server();
		s.listenForRequests();
	}

	public Server() {
		this.threadPool = new ThreadPoolExecutor(25, 25, 1, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.DiscardPolicy());
		this.threadPool.prestartAllCoreThreads();

		this.manufacturePartsThreadPool = new ThreadPoolExecutor(100, 25, 1, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.DiscardPolicy());
		this.manufacturePartsThreadPool.prestartAllCoreThreads();
		
		this.db = new DBMRandom();
	}

	/*
	 * Create a thread that listens for client requests.
	 * When a request is recieved, put it on the queue.
	 * If there is no room on the queue, reject the request.
	 */
	private void listenForRequests() {
		//create ServerSocket
		try (ServerSocket ssc = new ServerSocket(8189)) {
			//create Socket
			try (Socket incoming = ssc.accept()) {

				ObjectInputStream fromClient = new ObjectInputStream(incoming.getInputStream());
				PrintWriter toClient = new PrintWriter(new OutputStreamWriter(incoming.getOutputStream(), "UTF-8"), true);

				toClient.println("Hello! Enter BYE to exit.");

				while (true) {
					try {
						Request request = (Request) fromClient.readObject();

						/*
						 * Here is the logic for what this thread should do with the message 
						 * 	that it recieved from the client
						 */
						System.out.println("Recieved request: " + request);
						//threadPool.execute(new Handler(request, toClient));
						threadPool.execute(new RequestHandler(request, db, manufacturePartsThreadPool, toClient));
						
					} catch(EOFException e) {
						this.threadPool.shutdown();
						return;
					} catch(ClassNotFoundException e) {
						System.out.println("Caught ClassNotFoundException: " + e.getMessage());
						toClient.println("Error: Could not find class.");
					}
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}
}