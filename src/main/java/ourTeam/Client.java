package ourTeam;

import java.util.Scanner;
import java.util.Random;

import java.net.Socket;

import java.io.IOException;
import java.io.ObjectOutputStream;

/*
 *
	 The client randomly picks a lego set to order and places a new order with the application server every 50 milliseconds.
	 Many request-response connections can be created between client and server, but the client can not have more
	than 25 orders at a time that have not yet shipped.
	 The application server and the data store you build will communicate with each other via serialized Java objects being
	sent over TCP connections. This can be done either using java.net.Socket and java.net.ServerSocket or using Netty.
	 The client and application server can communicate either via TCP or via HTTP. You may use Netty or Jetty or
	HttpURLConnection.
 */

public class Client
{
	private static Random random = new Random();

	public static void main(String[] args) {
		request50ms();
	}

	private static void request50ms() {
		try(Socket socket = new Socket("localhost", 8189)) {
			Scanner fromServer = new Scanner(socket.getInputStream(), "UTF-8");
			ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());

			Thread requestThread = new Thread(()->{
				for (int requestNum = 0; true; requestNum++) {
					try {
						Thread.sleep(50);

						Request request = buildRequest();
						request.setName("#" + requestNum);
						System.out.println("Request to send: " + request);

						toServer.writeObject(request);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					} catch(IOException e) {
						e.printStackTrace();
						return;
					}
				}
			});

			Thread readThread = new Thread(()->{
				while (fromServer.hasNextLine()) System.out.println(fromServer.nextLine());
			});

			requestThread.start();
			readThread.start();

			requestThread.join();
			readThread.join();
		} catch(IOException e) {
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private static void requestRandom(int numRequests) {
		try(Socket socket = new Socket("localhost", 8189)) {
			Scanner fromServer = new Scanner(socket.getInputStream(), "UTF-8");
			ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());

			if (fromServer.hasNextLine()) System.out.println(fromServer.nextLine());

			for (int requestNum = 0; requestNum < numRequests; requestNum++) {
				Request request = buildRequest();
				request.setName("Request#" + requestNum);

				System.out.println("Request to send: " + request);

				toServer.writeObject(request);

				requestNum++;
			}

			while(fromServer.hasNextLine()) System.out.println(fromServer.nextLine());
			System.out.println("Connection to Server has been closed.");

		} catch(IOException e) {
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();
		}

	}

	private static Request buildRequest() {
		Request request = new Request();

		int numSets = 1;
		while (numSets > 0) {
			request.addSet(random.nextInt(1000));
			numSets--;
		}

		return request;
	}
}