package ourTeam;

import java.util.Scanner;
import java.util.Random;

import java.net.Socket;

import java.io.IOException;
import java.io.ObjectOutputStream;

/*
 * Client requets sets from the server
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

	public static void request(Request request){
		try(Socket socket = new Socket("localhost", 8189)) {
			Scanner fromServer = new Scanner(socket.getInputStream(), "UTF-8");
			ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());

			Thread requestThread = new Thread(()->{

					try {

						request.setName("#1");
						System.out.println("Request to send: " + request);

						toServer.writeObject(request);
					} catch(IOException e) {
						e.printStackTrace();
						return;
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

	public static void requestRandom(int numRequests) {
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