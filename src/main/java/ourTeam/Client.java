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
	private static Random random;

	public static void main(String[] args) {
		random = new Random();

		try(Socket socket = new Socket("localhost", 8189)) {
			Scanner fromServer = new Scanner(socket.getInputStream(), "UTF-8");
			ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());

			System.out.println(fromServer.nextLine());

			int tries = 1;
			while (tries > 0) {
				Request request = buildRequest();

				System.out.println(request);

				toServer.writeObject(request);

				if (fromServer.hasNextLine()) System.out.println(fromServer.nextLine());
				else System.out.println("Connection to Server has been closed.");

				tries--;
			}
		} catch(IOException e) {
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static Request buildRequest() {
		Request request = new Request();

		int numSets = random.nextInt(100) + 1;
		while (numSets > 0) {
			request.addSet(random.nextInt(1000));
			numSets--;
		}

		return request;
	}
}