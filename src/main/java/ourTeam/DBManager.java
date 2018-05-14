package ourTeam;

import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.LinkedList;
/*
 * This class is the interface between the Server and the Database.

	 There are roughly 26,000 parts and 11,000 sets in the data set. You will load all that data into the data store.
	 To keep things manageable and avoid the need for transactions and/or locking, we will make the connection between
		the application server and the data stores single threaded



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
public interface DBManager
{
    public abstract int getPartCount(String part);

	public abstract void decrementSet(int set, int amount);

	public abstract Set<String> getParts(int set);

    public abstract void incrementPart(String part, int incrementPartsBy);

    public abstract int getSetQuantity(int set);

    public abstract void incrementSet(int set);

    public abstract void incrementSetBy(int set, int amount);

    public abstract void decrementPart(String part);

    public abstract void decrementPartBy(String part, int amount);
}
