package ourTeam;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.io.PrintWriter;

/*
 * Just pointing out that this is assuming that a set only has one of a given part
 *	***Issues found***
 *
 */
public class RequestHandler implements Runnable {

    private Request request;
    private DBManager DB;
    private ReentrantReadWriteLock DBLock = new ReentrantReadWriteLock();
    private ThreadPoolExecutor partConstructorThreadPool;
    private PrintWriter toClient;
    private final int incrementPartsBy = 30;


    public RequestHandler(Request request, DBManager DB, ThreadPoolExecutor threadPool, PrintWriter toClient) {
        this.request = request;
        this.DB = DB;
        this.toClient = toClient;
        partConstructorThreadPool = threadPool;
    }

    @Override
    public void run() {
        HashMap<Integer, Integer> requestedSetsMap = request.getSets(); //setNames/quantities desired
        Set<Integer> requestedSetNames = requestedSetsMap.keySet(); // set of names of sets wanted by client
        List<Integer> setsNotAvailable = new ArrayList<Integer>(); // list to contain sets that are not available

        DBLock.writeLock().lock(); //the check and shipping, though it starts with a read, are both write locked to ensure set is not used between checking and shipping
            
        /*
         * figure out which sets we do not have in stock
         */
        for (Integer set : requestedSetNames) { //check if sets are in stock
            if (requestedSetsMap.get(set) < DB.getSetQuantity(set))
                setsNotAvailable.add(set);
        }

        /*
         * manufacture those sets so that we can complete the order
         */
        if (!setsNotAvailable.isEmpty()) { //if some sets are not in stock, must determine which parts are needed

           // DBLock.writeLock().unlock(); //this will take a while, so we should only use a read-lock
           // DBLock.readLock().lock();


            int actualPartQuantity, neededPartQuantity, differenceNeeded = 0; // placeholders for later

            /*
             * figure out how much we need of which parts are needed in total to complete the order
             */
            Map<String, Integer> neededParts = new HashMap<String, Integer>();//part needed and amount needed
            for (Integer set : setsNotAvailable) { //for every set
                Set<String> setParts = DB.getParts(set);//get a set of its parts and put that set in a map under the set's name

                for (String part : setParts) { //for every part of this set
                    if (!neededParts.containsKey(part)) {
                        neededParts.put(part, requestedSetsMap.get(set));
                    } else {
                        neededParts.put(part, requestedSetsMap.get(set) + neededParts.get(part));
                    }
                }
            }

            /*
             * Figure out how much of which parts we need to manufacture
             */
            Map<String, Integer> partsToManufacture = new HashMap<String, Integer>();
            for (String part : neededParts.keySet()) {
                actualPartQuantity = DB.getPartCount(part);
                neededPartQuantity = neededParts.get(part);

                if (actualPartQuantity < neededPartQuantity) {
                    partsToManufacture.put(part, neededPartQuantity - actualPartQuantity);
                }
            }


           // DBLock.readLock().unlock();

            //potentially the parts number could decrease here, causing the part number to be insufficient to create a set

            /*
             * manufacture those parts
             */
            if (!neededParts.isEmpty()) { //the map of needed parts is not empty, we must need to order some parts

                DBLock.writeLock().unlock(); //this will take a while, should release locks

                manufactureParts(partsToManufacture); //"manufacture" the parts (start thread with 100 millisecond timer for each parts, wait for them to finish

                DBLock.writeLock().lock();

                incrementParts(partsToManufacture); //increment the amount of parts in the actual database

            }

            //else, parts in stock
            // DBLock.writeLock().lock();

            /*
             * turn the parts into sets
             */
            manufactureSets(requestedSetsMap);
        }

        /*
         * ship the sets
         */

        //all sets are in stock
        shipSets(requestedSetsMap);

        DBLock.writeLock().unlock();

        informClientSuccess();

    }

    /*
     * ***** METHODS DEALING WITH DATABASE STUFF *****
     */

    private void incrementParts(Map<String, Integer> parts) {
        Set<String> partNames = parts.keySet();
        for(String part : partNames){
            int extraIncrement = incrementPartsBy * roundUp(parts.get(part), incrementPartsBy);
            DB.incrementPart(part, incrementPartsBy + extraIncrement);
        }
    }

    private void manufactureSets(HashMap<Integer, Integer> sets) {
        Set<Integer> setNames = sets.keySet();
        for(Integer set : setNames){
            decrementPartsOfSet(set);
            DB.incrementSet(set);
        }
    }

    private void decrementPartsOfSet(Integer set) {
        Set<String> parts = DB.getParts(set);
        for(String part : parts){
            DB.decrementPart(part);
        }
    }

    private void shipSets(HashMap<Integer, Integer> sets){
        Set<Integer> requestedSets = sets.keySet();
        for(Integer set : requestedSets){
            DB.decrementSet(set, sets.get(set));
        }
    }

    /*
     * ***** METHODS DEALING WITH OTHER PARTS OF THE LOGIC *****
     */

    private void manufactureParts(Map<String, Integer> neededParts) {
        List<Callable<Object>> partRunnables = new ArrayList<Callable<Object>>();
        int numberOfParts = neededParts.size();
        for(int amount : neededParts.values()){
            partRunnables.add(Executors.callable(new partConstructor(roundUp(amount, incrementPartsBy) * 100)));
        }
        try {
            partConstructorThreadPool.invokeAll(partRunnables);
        } catch (InterruptedException e) {
            e.printStackTrace(); //TODO
        }
    }

    class partConstructor implements Runnable {
    	private int time = 100;

    	public partConstructor(int time) {
    		this.time = time;
    	}

        @Override
        public void run() {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace(); //TODO
            }
        }
    }


    private void informClientSuccess(){
        toClient.println("Your order has shipped.");
    }

    //got this from here: https://stackoverflow.com/questions/7446710/how-to-round-up-integer-division-and-have-int-result-in-java
    private int roundUp(int num, int divisor) {
        return (num + divisor - 1) / divisor;
    }
}
