
import org.junit.Assert;
import org.junit.Test;
import ourTeam.Client;
import ourTeam.DBManagerImpl;
import ourTeam.Request;
import ourTeam.Server;
import sun.nio.ch.ThreadPool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.containsString;

public class TestClass{


    private static final String DBHost = "localhost";

    private ThreadPoolExecutor threadPool;

    public class ServerRunner implements Runnable {

        Server s;

        public ServerRunner(Server server){
            s = server;
        }

        @Override
        public void run() {
            s.main(null);
        }
    }

    public class ClientRequestRunner implements Runnable{

        Client c;
        Request r;
        String h;

        public ClientRequestRunner(Client client, Request request, String host){
            c = client;
            r = request;
            h = host;
        }
        @Override
        public void run() {
            c.request(r, h);
        }
    }
    private void initializeServer(ThreadPoolExecutor tp, Server s){
        tp.execute(new ServerRunner(s));
    }

    private void requestClient(ThreadPoolExecutor tp, Client c, Request r, String host){
        tp.execute(new ClientRequestRunner(c, r, host));
    }

    private ThreadPoolExecutor getNewThreadPool(){
        return new ThreadPoolExecutor(25, 25, 1, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>(), new ThreadPoolExecutor.AbortPolicy());
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
        DBManagerImpl db = new DBManagerImpl();
        int setToOrder = -1, setQuantity = 0;
        for(int i=1; i<2000; i++){
            setQuantity = db.getSetCount(i);
            if (setQuantity > 0) {
                setToOrder = i;
                i = 2000;
            }
        }

        ThreadPoolExecutor tp = getNewThreadPool();
        Server s = new Server();
        Client c = new Client();
        Request r = new Request();
        r.addSet(setToOrder);
        initializeServer(tp, s);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        requestClient(tp, c, r, DBHost);
        r = new Request();
        r.setName("quit");
        requestClient(tp, c, r, DBHost);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tp.shutdownNow();
        Assert.assertTrue(db.getSetCount(setToOrder) == (setQuantity - 1));


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
        DBManagerImpl db = new DBManagerImpl();
        HashMap<String,Integer> oldPartQuantities = new HashMap<>();
        int setToOrder = -1, setQuantity = 0, partAmount = 0;
        boolean setHasParts = true;
        Set<String> parts = db.getParts(1);
        for(int i=1; i<2000; i++){
            setHasParts = true;
            setQuantity = db.getSetCount(i);
            if (setQuantity == 0) {
                parts = db.getParts(i);
                for (String part : parts){
                    if(oldPartQuantities.containsKey(part))
                        partAmount = oldPartQuantities.get(part);
                    else {
                        partAmount = db.getPartCount(i, part);
                        oldPartQuantities.put(part, partAmount);
                    }
                    if(db.getPartCount(i, part) <= 0) {
                        setHasParts = false;
                        break;
                    }
                }
                if(setHasParts) {
                    setToOrder = i;
                    i = 2000;
                }
            }
        }

        ThreadPoolExecutor tp = getNewThreadPool();
        Server s = new Server();
        Client c = new Client();
        Request r = new Request();
        r.addSet(setToOrder);
        initializeServer(tp, s);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        requestClient(tp, c, r, DBHost);
        r = new Request();
        r.setName("quit");
        requestClient(tp, c, r, DBHost);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tp.shutdownNow();
        for(String part : parts){
            Assert.assertTrue(db.getPartCount(setToOrder,part) == (oldPartQuantities.get(part) - 2));
        }


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
        DBManagerImpl db = new DBManagerImpl();
        HashMap<String,Integer> oldPartQuantities = new HashMap<>();
        int setToOrder = -1, setQuantity = 0, partAmount = 0;
        boolean setHasParts = true;
        Set<String> parts = db.getParts(1);
        Set<String> NAParts = new HashSet<>();
        for(int i=1; i<2000; i++){
            setHasParts = true;
            setQuantity = db.getSetCount(i);
            if (setQuantity == 0) {
                parts = db.getParts(i);
                for (String part : parts){
                    if(oldPartQuantities.containsKey(part))
                        partAmount = oldPartQuantities.get(part);
                    else {
                        partAmount = db.getPartCount(i, part);
                        oldPartQuantities.put(part, partAmount);
                    }
                    if(db.getPartCount(i, part) <= 0) {
                        NAParts.add(part);
                        setHasParts = false;
                    }
                }
                if(!setHasParts) {
                    setToOrder = i;
                    i = 2000;
                }
            }
        }

        ThreadPoolExecutor tp = getNewThreadPool();
        Server s = new Server();
        Client c = new Client();
        Request r = new Request();
        r.addSet(setToOrder);
        initializeServer(tp, s);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        requestClient(tp, c, r, DBHost);
        r = new Request();
        r.setName("quit");
        requestClient(tp, c, r, DBHost);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tp.shutdownNow();
        for(String part : NAParts){
            Assert.assertTrue(db.getPartCount(setToOrder,part) == 28);
        }


    }


}
