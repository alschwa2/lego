import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 *	***Issues found***
 */
public class RequestHandler implements Runnable {

    private Request request;
    private DBManager DB;
    private ReentrantReadWriteLock DBLock = new ReentrantReadWriteLock();
    private ThreadPoolExecutor partConstructorThreadPool;
    private final int incrementPartsBy = 30;


    public RequestHandler(Request request, DBManager DB, ThreadPoolExecutor threadPool){
        this.request = request;
        this.DB = DB;
        partConstructorThreadPool = threadPool;
    }

    @Override
    public void run(){

        HashMap<Integer, Integer> sets = request.getSets(); //setNames/quantities desired
        Set<Integer> setNameList = sets.keySet(); // set of names of sets wanted by client
        List<Integer> NA = new ArrayList<Integer>(); // list to contain sets that are not available
        Boolean setsInStock = false; // boolean to be used later

        DBLock.writeLock().lock(); //the check and shipping, though it starts with a read, are both write locked to ensure set is not used between checking and shipping

        while(setsInStock == false) {
            
            for (Integer set : setNameList) { //check if sets are in stock
                if (sets.get(set) < DB.getSetQuantity(set))
                    NA.add(set);
            }

            if (!NA.isEmpty()) { //if some sets are not in stock, must determine which parts are needed

               // DBLock.writeLock().unlock(); //this will take a while, so we should only use a read-lock
               // DBLock.readLock().lock();

                HashMap<Integer, Set<String>> parts = new HashMap<Integer, Set<String>>(); //a map mapping the set name to a set of its parts needed
                Map<String, Integer> neededParts = new HashMap<String, Integer>();//part needed and amount needed

                int actualPartQuantity, neededPartQuantity, differenceNeeded = 0; // placeholders for later

                for (Integer set : NA) { //for every set
                    Set<String> setParts = DB.getParts(set);//get a set of its parts and put that set in a map under the set's name
                    parts.put(set, setParts);

                    for (String part : setParts) { //for every part of this set
                        actualPartQuantity = DB.getPartCount(part);
                        neededPartQuantity = sets.get(set); 
                        REWRITE
                        if (neededParts.containsKey(part)) neededPartQuantity += neededParts.get(part);

                        if (actualPartQuantity < neededPartQuantity) {//if we do not have sufficient parts to make a new set
                            differenceNeeded = neededPartQuantity - actualPartQuantity; //find out how many parts we need

	                        if (neededParts.containsKey(part)) { //if the part was already needed by another set, need to update amount needed for that part
	                            differenceNeeded += neededParts.get(part);
	                        } else {                            // at least this amount of parts will be ordered
	                        	neededParts.put(part, differenceNeeded); 
	                        }
	                    }
                    }
                }

               // DBLock.readLock().unlock();

                //potentially the parts number could decrease here, causing the part number to be insufficient to create a set

                if (!neededParts.isEmpty()) { //the map of needed parts is not empty, we must need to order some parts

                    DBLock.writeLock().unlock(); //this will take a while, should release locks

                    manufactureParts(neededParts); //"manufacture" the parts (start thread with 100 millisecond timer for each parts, wait for them to finish

                    DBLock.writeLock().lock();

                    incrementParts(neededParts); //increment the amount of parts in the actual database

                }

                //else, parts in stock
                // DBLock.writeLock().lock();


                manufactureSets(sets);

                setsInStock = true; //since we locked the database, will be true until unlock (naive implementation, dont need loop for this)
            }
            else {
                setsInStock = true; //end loop, dont need to manufacture parts or sets
            }
        }
        //all sets are in stock
        shipSets(sets);

        DBLock.writeLock().unlock();

        informClientSuccess();

    }

    private Boolean setsInStock() {
        //TODO
        return true;
    }



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

    class partConstructor implements Runnable{
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
        //TODO
    }


    

    private void shipSets(HashMap<Integer, Integer> sets){
        Set<Integer> requestedSets = sets.keySet();
        for(Integer set : requestedSets){
            DB.decrementSet(set, sets.get(set));
        }
    }


    //got this from here: https://stackoverflow.com/questions/7446710/how-to-round-up-integer-division-and-have-int-result-in-java
    private int roundUp(int num, int divisor) {
        return (num + divisor - 1) / divisor;
    }
}
