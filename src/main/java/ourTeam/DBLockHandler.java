package ourTeam;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBLockHandler {

    private ConcurrentHashMap<String,ReentrantReadWriteLock> partLocks;
    private ConcurrentHashMap<Integer,ReentrantReadWriteLock> setLocks;

    private ReentrantReadWriteLock allSetsLock;
    private ReentrantReadWriteLock allPartsLock;

    private ReentrantReadWriteLock partMapLock;
    private ReentrantReadWriteLock setMapLock;

    public DBLockHandler(){
        this.partLocks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
        this.setLocks = new ConcurrentHashMap<Integer, ReentrantReadWriteLock>();

        this.allSetsLock = new ReentrantReadWriteLock();
        this.allPartsLock = new ReentrantReadWriteLock();

        this.partMapLock = new ReentrantReadWriteLock();
        this.setMapLock = new ReentrantReadWriteLock();
    }

    /*
     * ***** PARTS *****
     */
    public void readLockPart(String part){
        partMapLock.readLock().lock();
        allPartsLock.readLock().lock();

        if(!partLocks.containsKey(part))
           createPartLock(part);


        partLocks.get(part).readLock().lock();

        allPartsLock.readLock().unlock();
        partMapLock.readLock().unlock();
    }

    public void readUnlockPart(String part){
        partMapLock.readLock().lock();
        allPartsLock.readLock().lock();

        partLocks.get(part).readLock().unlock();

        allPartsLock.readLock().unlock();
        partMapLock.readLock().unlock();
    }

    public void writeLockPart(String part){
        partMapLock.readLock().lock();
        allPartsLock.readLock().lock();

        if(!partLocks.containsKey(part))
           createPartLock(part);

        partLocks.get(part).writeLock().lock();

        allPartsLock.readLock().unlock();
        partMapLock.readLock().unlock();
    }

    public void writeUnlockPart(String part){
        partMapLock.readLock().lock();
        allPartsLock.readLock().lock();

        partLocks.get(part).writeLock().unlock();

        allPartsLock.readLock().unlock();
        partMapLock.readLock().unlock();
    }

    /*
     * ***** SETS *****
     */

    public void readLockSet(int set){
        setMapLock.readLock().lock();
        allSetsLock.readLock().lock();


        if(!setLocks.containsKey(set))
            createSetLock(set);

        setLocks.get(set).readLock().lock();

        allSetsLock.readLock().unlock();
        setMapLock.readLock().unlock();
    }

    public void readUnlockSet(int set) {
        setMapLock.readLock().lock();
        allSetsLock.readLock().lock();

        setLocks.get(set).readLock().unlock();

        allSetsLock.readLock().unlock();
        setMapLock.readLock().unlock();
    }

    public void writeLockSet(int set){
        setMapLock.readLock().lock();
        allSetsLock.readLock().lock();

        if(!setLocks.containsKey(set))
            createSetLock(set);

        setLocks.get(set).writeLock().lock();

        allSetsLock.readLock().unlock();
        setMapLock.readLock().unlock();
    }

    public void writeUnlockSet(int set){
        setMapLock.readLock().lock();
        allSetsLock.readLock().lock();

        setLocks.get(set).writeLock().unlock();

        allSetsLock.readLock().unlock();
        setMapLock.readLock().unlock();
    }

    /*
     * ***** LOCK EVERYTHING *****
     */

    public void readLockAllSets(){
        allSetsLock.readLock().lock();
    }
    public void readUnlockAllSets(){
        allSetsLock.readLock().unlock();
    }
    public void writeLockAllSets(){
        allSetsLock.writeLock().lock();
    }
    public void writeUnlockAllSets(){
        allSetsLock.writeLock().unlock();
    }

    public void readLockAllParts(){
        allPartsLock.readLock().lock();
    }
    public void readUnlockAllParts(){
        allPartsLock.readLock().unlock();
    }
    public void writeLockAllParts(){
        allPartsLock.writeLock().lock();
    }
    public void writeUnlockAllParts(){
        allPartsLock.writeLock().unlock();
    }

    /*
     * ***** CREATING NEW LOCKS *****
     */

    private void createSetLock(int set) {
        setMapLock.readLock().unlock();
        setMapLock.writeLock().lock();


        if(!setLocks.containsKey(set))
            setLocks.put(set, new ReentrantReadWriteLock());

        setMapLock.readLock().lock();
        setMapLock.writeLock().unlock();
    }

    private void createPartLock(String part) {
        partMapLock.readLock().unlock();
        partMapLock.writeLock().lock();

        if(!partLocks.containsKey(part))
            partLocks.put(part, new ReentrantReadWriteLock());

        partMapLock.readLock().lock();
        partMapLock.writeLock().unlock();
    }
}
