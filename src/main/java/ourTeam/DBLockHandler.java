package ourTeam;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBLockHandler {

    private ConcurrentHashMap<String,ReentrantReadWriteLock> partLocks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
    private ConcurrentHashMap<Integer,ReentrantReadWriteLock> setLocks = new ConcurrentHashMap<Integer, ReentrantReadWriteLock>();
    private ReentrantReadWriteLock allSetsLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock allPartsLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock partMapLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock setMapLock = new ReentrantReadWriteLock();

    public DBLockHandler(){

    }

    public void readLockPart(String part){
        partMapLock.readLock().lock();
        allPartsLock.readLock().lock();

        if(!partLocks.containsKey(part))
           createPart(part);


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
           createPart(part);

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

    public void readLockSet(int set){
        setMapLock.readLock().lock();
        allSetsLock.readLock().lock();


        if(!setLocks.containsKey(set))
            createSet(set);

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
            createSet(set);

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


    private void createSet(int set) {
        setMapLock.readLock().unlock();
        setMapLock.writeLock().lock();

        createSetAux(set);

        setMapLock.readLock().lock();
        setMapLock.writeLock().unlock();
    }

    private void createPart(String part) {
        partMapLock.readLock().unlock();
        partMapLock.writeLock().lock();

        createPartAux(part);

        partMapLock.readLock().lock();
        partMapLock.writeLock().unlock();
    }

    private void createSetAux(int set){
        if(!setLocks.containsKey(set))
            setLocks.put(set, new ReentrantReadWriteLock());
    }


    private void createPartAux(String part) {
        if(!partLocks.containsKey(part))
            partLocks.put(part, new ReentrantReadWriteLock());
    }


}
