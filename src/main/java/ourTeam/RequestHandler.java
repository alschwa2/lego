package ourTeam;

import java.util.*;
import java.util.concurrent.*;

import java.io.PrintWriter;

/**
 * The RequestHandler Class, part of the lego client-server project. It deals with the majority of the logic for manufacturing and shipping sets to the client.
 *
 * An instance of this class is created when a new request comes in to the server from the client. The instance is fed the request object, DBManager object,
 * Threadpool object(for constructing missing parts, explained later), a DBLockManager object for the DataBase, and a PrintWriter object to contact the client
 * with the results. The RequestHandler instance is then put into its own thread and executed. it first determines whether there is sufficient quantity of the
 * requested sets, then reserves (decrements) those sets so that no other thread can ship them. If the amount in the DB was not sufficient, it informs the client
 * that the sets are backordered, and the manufacturing process begins.
 *
 * MANUFACTURING SETS:
 * It checks if there are enough parts in the DataBase to manufacture more. If There are enough parts, the parts are decremented in the DB and sets incremented, then decremented
 * again when the sets are shipped to the client. If there are not enough parts, the part manufacturing begins.
 *
 * MANUFACTURING PARTS:
 * The parts and amounts needed are determined, and the parts which are in stock are "reserved" (decremented) for later set manufacture. Once all parts are
 * manufactured (by setting timer threads for 100 ms per INCREMENT_PARTS_BY parts), the part count in the
 * DB are incremented by a multiple of INCREMENT_PARTS_BY, decremented by the amount of sets ordered, sets are incremented by the appropriate amount, sets are
 * decremented by the appropriate amount, then the client is informed that the parts have been shipped.
 *
 */
public class RequestHandler implements Runnable {

    private Request request;
    private DBManager DB;

    private ThreadPoolExecutor partConstructorThreadPool; //the thread pool which "constructs" parts (sets a timer for each part). Is the same threadpool for all RequestHandlers
    private PrintWriter toClient; //the output object to talk back to the client
    private DBLockHandler DBLocks; //the locks on the DataBase, shared by all RequestHandlers
    private static final int INCREMENT_PARTS_BY = 30; //how much the parts should be incremented when they are needed


    public RequestHandler(Request request, DBManager DB, ThreadPoolExecutor threadPool, DBLockHandler locks, PrintWriter toClient) {
        this.request = request;
        this.DB = DB;
        this.toClient = toClient;
        partConstructorThreadPool = threadPool;
        DBLocks = locks;
    }

    /**
     * The main logic of the RequestHandler, as described in the class description. Ships sets, manufactures sets if needed, manufactures parts if needed, and
     * communicates back to the client regarding shipped orders and backorders
     */
    @Override
    public void run() {
        HashMap<Integer, Integer> requestedSetsMap = request.getSets(); //setNames/quantities desired
        HashMap<Integer, Integer> setsNotAvailable = new HashMap<>(); // list to contain sets that are not available
        HashMap<Integer, Integer>  availableSets = new HashMap<Integer, Integer>(); //available sets which will be reserved right away, to prevent race conditions/over-locking down the line
        /*
         * figure out which sets we do not have in stock
         */

        DBLocks.writeLockAllSets(); //need exclusive access to the sets so that no other thread ships sets between us getting the set count and decrementing them

        determineAvailableSets(requestedSetsMap, setsNotAvailable, availableSets);
        reserveSets(availableSets);//decrement the available sets in the DB so that they can be reserved for this customer and unlocked for other thread's usage.

        DBLocks.writeUnlockAllSets();

        /*
         * manufacture those sets so that we can complete the order
         */

        if (!setsNotAvailable.isEmpty()) { //if some sets are not in stock, must determine which parts are needed
            alertClientBackordered();

            /*
             * figure out how much of which parts are needed in total to complete the order
             */
            DBLocks.readLockAllParts();

            Map<String, SetPartAmountTuple> neededParts = determineNeededParts(setsNotAvailable); //determine which and how many parts we need

            DBLocks.readUnlockAllParts();
            DBLocks.writeLockAllParts();//will actually be decrementing parts now, so need a write lock

            /*
             * Figure out how much of which parts we need to manufacture
             */
            Map<String, SetPartAmountTuple> partsToManufacture = new HashMap<String, SetPartAmountTuple>();
            Map<String, SetPartAmountTuple> partsToReserve = new HashMap<String, SetPartAmountTuple>();

            determinePartsToManufactureAndReserve(neededParts,partsToManufacture,partsToReserve);

            reserveParts(partsToReserve);
            DBLocks.writeUnlockAllParts();

            /*
             * manufacture those parts
             */
            if (!partsToManufacture.isEmpty()) { //the map of needed parts is not empty, we must need to order some parts
                manufactureParts(partsToManufacture); //"manufacture" the parts (start thread with 100 millisecond timer for each parts, wait for them to finish

                DBLocks.writeLockAllParts();

                incrementParts(partsToManufacture); //increment the amount of parts in the actual database
                decrementParts(partsToManufacture);

                DBLocks.writeUnlockAllParts();
            }

            //else, parts in stock
            /*
             * turn the parts into sets
             */
            DBLocks.writeLockAllSets();

            manufactureSets(setsNotAvailable);
            shipSets(setsNotAvailable);

            DBLocks.writeUnlockAllSets();
        }
        informClientSuccess();

    }

    private void alertClientBackordered() {
        if (toClient == null) {
            System.out.println("Your order (" + request.getName() + ") is backordered. Will ship soon!");
            return;
        }
        toClient.println("Your order (" + request.getName() + ") is backordered. Will ship soon!");
    }

    /**
     * This class is a helper object for any parts-amount hashmap we have. We cannot query the database to edit a part's amount with also including the set it is from, so
     * this class creates a tuple, with one element being the set that the part is from, and one being the amount of that part that is needed. This object
     * then becomes the value of any hashmap with a part as the key.
     */
    class SetPartAmountTuple{
        int set,amount;

        SetPartAmountTuple(int set, int amount){
            this.set = set;
            this.amount = amount;
        }
    }

    /*
     * ***** METHODS DEALING WITH EDITING DATABASE *****
     */

    /**
     * increments the parts in the database contained in the specified sets by the specified amounts
     * @param parts A hashmap with keyset of parts and valueset of tuples containing the name of the set in which the part is contained and the amount of parts
     *              by which to increment the key part.
     */
    private void incrementParts(Map<String, SetPartAmountTuple> parts) {
        Set<String> partNames = parts.keySet();
        for(String part : partNames){
            int increment = INCREMENT_PARTS_BY * roundUp(parts.get(part).amount, INCREMENT_PARTS_BY);
            DB.incrementPart(parts.get(part).set, part, increment);
        }
    }

    /**
     * reserves parts to be used in a manufacture process of sets later on. Since reserving here is the same as decrementing, just call decrementParts
     * @param partsToReserve parts to decrement in DB
     */
    private void reserveParts(Map<String,SetPartAmountTuple> partsToReserve) {
        decrementParts(partsToReserve);
    }


    /**
     * decrements the parts in the database contained in the specified sets by the specified amounts
     * @param parts A hashmap with keyset of parts and valueset of tuples containing the name of the set in which the part is contained and the amount of parts
     *      *            by which to decrement the key part.
     */
    private void decrementParts(Map<String, SetPartAmountTuple> parts) {
        Set<String> partNames = parts.keySet();
        SetPartAmountTuple tuple;
        for(String part : partNames){
            tuple = parts.get(part);
            DB.decrementPart(tuple.set, part, tuple.amount * 2);
        }
    }

    /**
     * Manufactures sets by incrementing their amount in the database by the specified number
     * @param sets A hashmap with keyset of sets and valueset of integers by which to increment the sets in the database
     */
    private void manufactureSets(HashMap<Integer, Integer> sets) {
        Set<Integer> setNames = sets.keySet();
        for(Integer set : setNames){
            DB.incrementSet(set, sets.get(set) + 1);
        }
    }


    /**
     * reserves sets from DB for future shipping. Since for our purposes shipping and decrementing the DB are the same thing, just calls shipSets
     * @param sets hashmap mapping set_number to amount_to_reserve
     */
    private void reserveSets(HashMap<Integer,Integer> sets) {
        shipSets(sets);
    }

    private void shipSets(HashMap<Integer, Integer> sets){
        Set<Integer> requestedSets = sets.keySet();
        for(Integer set : requestedSets){
            DB.decrementSet(set, sets.get(set));

        }
    }

    /*
       METHODS WHICH READ FROM DATABASE + USE SOME LOGIC
     */

    /**
     * Determines the amount of sets, based on the client's request, which are available and not available
     * @param requestedSetsMap
     * @param setsNotAvailable The map into which the function will store the sets which are not available <set : Amount needed>
     * @param availableSets The map into which the function will store the sets available <Set : Amount available to ship></Set>
     */
    private void determineAvailableSets(HashMap<Integer, Integer> requestedSetsMap, HashMap<Integer, Integer> setsNotAvailable, HashMap<Integer, Integer>  availableSets){
        Set<Integer> requestedSetNames = requestedSetsMap.keySet();
        for (Integer set : requestedSetNames) { //check if sets are in stock
            int amountNeeded = requestedSetsMap.get(set);
            int amountAvailable = DB.getSetCount(set);
            if (amountNeeded > amountAvailable) {
                setsNotAvailable.put(set, amountNeeded - amountAvailable);
                if(amountAvailable > 0)
                    availableSets.put(set, amountAvailable);
            }
            else
                availableSets.put(set, amountNeeded);
        }
    }

    /**
     * Determines the parts and amount of parts which need to be manufactured in order to manufacture the requested sets.
     *
     * @param neededParts The parts and amounts which are needed to manufacture the sets
     * @param partsToManufacture The map into which the function will store the parts and amount of parts which need to be manufactured
     * @param partsToReserve he map into which the function will store the parts and amount of parts which are available immediately and should be reserved for future set manufacture
     */
    private void determinePartsToManufactureAndReserve( Map<String, SetPartAmountTuple> neededParts, Map<String, SetPartAmountTuple> partsToManufacture,  Map<String, SetPartAmountTuple> partsToReserve){

        SetPartAmountTuple currentTuple;
        int actualPartQuantity, neededPartQuantity;
        for (String part : neededParts.keySet()) { //for every one of our needed parts, determine if we have insufficient quantities to fulfill the order
            currentTuple = neededParts.get(part);
            actualPartQuantity = DB.getPartCount(currentTuple.set, part); //how much we actually have
            neededPartQuantity = currentTuple.amount; //how much we need
            if (actualPartQuantity < neededPartQuantity) {
                partsToManufacture.put(part, new SetPartAmountTuple(currentTuple.set, (neededPartQuantity - actualPartQuantity)));
                if(actualPartQuantity > 0)
                    partsToReserve.put(part, new SetPartAmountTuple(currentTuple.set, actualPartQuantity));
            }
            else
                partsToReserve.put(part, new SetPartAmountTuple(currentTuple.set, (neededPartQuantity)));
        }
    }

    /**
     * Determines the parts and amount of parts which are needed to manufacture the requested sets.
     *
     * @param setsToManufacture a map of the sets which need to be manufactured
     * @return a map the parts and amount of parts needed to manufacture the specified sets
     */
    private Map<String, SetPartAmountTuple> determineNeededParts(HashMap<Integer, Integer> setsToManufacture){
        Map<String, SetPartAmountTuple> neededParts = new HashMap<String, SetPartAmountTuple>();//part needed and <set, amountOfPArtNeeded> tuple
        int actualPartQuantity, neededPartQuantity, differenceNeeded = 0; // placeholders for later

        for (Integer set : setsToManufacture.keySet()) { //for every set
            Set<String> setParts = DB.getParts(set);//get a set of its parts and put that set in a map under the set's name
            for (String part : setParts) { //for every part of this set
                if (!neededParts.containsKey(part)) {
                    neededParts.put(part, new SetPartAmountTuple(set, setsToManufacture.get(set))); //need one copy of the part per copy of the set
                } else {
                    neededParts.put(part, new SetPartAmountTuple(set, (setsToManufacture.get(set) + neededParts.get(part).amount)));
                }
            }
        }

        return neededParts;
    }

    /*
     * ***** METHODS DEALING WITH OTHER PARTS OF THE LOGIC *****
     */

    /**
     * Manufactures the specified parts by setting a timer in a thread for the amount of parts needed divided by INCREMENT_PARTS_BY, rounded up, then multiplied by 100.
     * This ensures that, for each part, 100 ms are put into the timer for each INCREMENT_PARTS_BY interval in the amount of parts needed for each part.
     * @param neededParts
     */
    private void manufactureParts(Map<String, SetPartAmountTuple> neededParts) {
        List<Callable<Object>> partRunnable = new ArrayList<Callable<Object>>();
        int amount, timeToWait;
        for(SetPartAmountTuple tuple : neededParts.values()){ //for every part
            amount = tuple.amount; //the exact amount of that part which is needed
            timeToWait = roundUp(amount, INCREMENT_PARTS_BY) * 100; //(amount/INCREMENT_PARTS_BY)(rounded up) * 100 ms
            partRunnable.add(Executors.callable(new PartConstructor(timeToWait)));
        }
        try {
            partConstructorThreadPool.invokeAll(partRunnable); //start all threads and wait until they finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * a runnable class meant to be created for one set part, meant to simulate the manufacturing of an amount of that part by waiting a specified amount of time.
     */
    class PartConstructor implements Runnable {
    	private int time;

    	public PartConstructor(int time) {
    		this.time = time;
    	}

        @Override
        public void run() {
            try {
                TimeUnit.MILLISECONDS.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void informClientSuccess(){
        if (toClient == null) {
            System.out.println("Your order (" + request.getName() + ") has shipped.");
            return;
        }
        toClient.println("Your order (" + request.getName() + ") has shipped.");
    }

    //got this from here: https://stackoverflow.com/questions/7446710/how-to-round-up-integer-division-and-have-int-result-in-java
    private int roundUp(int num, int divisor) {
        return (num + divisor - 1) / divisor;
    }
}
