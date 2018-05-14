import java.io.PrintWriter;

public class Handler implements Runnable 
{
	Request request;
	PrintWriter toClient;

	public Handler(Request request, PrintWriter toClient) {
		this.request = request;
		this.toClient = toClient;
	}
	
	public void run() {
		toClient.println("Echo: " + request);
	}
}