package java;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.containsString;

public class TestClass{

    @Test
    public void testMethod() {
        assertThat("a", containsString("Hello"));
    }


    /*
    When testing data store options #2 and #3,
     the unit tests must use the AWS SDK to kill some of the EC2 machines that the data store
      is running on and show that the system still works correctly and returns correct results
    */
    @Test
    public void testAWSKill() {
        
    }
    /*
     * When an order is received, the application server accesses the inventory_sets table to see if a set is available.
     * If so, it reduces the inventory_sets.quantity of the set by one and replies to the customer with a message that
	 * the set has been shipped.
     */
    @Test
    public void testOrderRecieved() {
        
    }

    /*
     * If inventory_sets.quantity < the number of that set the customer ordered (e.g. the customer ordered 5 lego
	 * police cars and inventory_sets.quantity for the police car set is < 5), the application server checks the
	 * inventory_parts table to see if there is enough inventory of all the parts in the set to assemble enough sets to
	 * fulfill the order. If so, it “assembles” the sets by decrementing the inventory_ parts.quantity by the amount
 	 * needed and replies to the customer that the sets have shipped.
     */

    @Test
    public void testAssembleSets() {
        
    }

    

    /*
     * If there are not enough parts, the application server:
     * Sends a message to the client that the set is “backordered”
     * creates a timer thread which counts 100 milliseconds for the required parts to be “manufactured.”
     * When the 100 milliseconds are up, the inventory_ parts.quantity for the part is incremented by 30.
     * Once all of a given order’s manufacturing timers are done, the application server tries again to fill the order.
     * When an order is filled, the server will include an “order shipped” message to the client.
    */

    @Test
    public void testNotEnoughParts() {
        
    }
    /* 
     * The client randomly picks a lego set to order 
     * and places a new order with the application server every 50 milliseconds.
     */
    @Test
    public void testPlaceOrder() {
        
    }

    /*
     * Many “request-response” connections can be created between client and server, but the client can not have more
	 * than 25 orders at a time that have not yet shipped.
     */

    @Test
    public void testRequestResponse() {
        
    }


}
