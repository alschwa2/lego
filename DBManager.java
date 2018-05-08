/*
 * 
	What is this class:

 	This class is the interface between the Server and the Database.
 	***** WE ARE GOING TO CHANGE THIS FROM AN ABSTRACT CLASS TO AN INTERFACE SOON****
 	***** THE INTERFACE IS NOT DONE YET. DO NOT TRY TO IMPLEMENT THE INTERFACE UNTIL IT IS CONFIRMED AS FINISHED *****
 	Basically, this is going to be the interface that the Server talks to get information from the database.
 	The methods are going to be in terms of Sets and Parts. How this translates to dynamo queries is going to be the job
 		of the Dynamo team. They are going to create a class that implements this interface.
 	The application team will create a dummy implementation for testing purposes so that each team can work independantly.





	Copied and pasted from the assignment:

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
public abstract class DBManager
{
	public boolean setIsAvailible(String set);

	public boolean partsAreAvailible(String set);

	public boolean orderSet(String set);

	public boolean decrementSet(String set);

	public boolean getParts(String set);

	/*
	 */
	public boolean orderParts(String set);

	private LinkedList<String> getMissingParts(String set);

	private boolean incrementPartByThirty(String part);
}
/*

Okay so we have to deal with the fact that each part can appear multiple times in the set. How do we do that? 

*/