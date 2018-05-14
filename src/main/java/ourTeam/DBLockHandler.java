package ourTeam;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBLockHandler {

    private ConcurrentHashMap<String,ReentrantReadWriteLock> partLocks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
    private ConcurrentHashMap<Integer,ReentrantReadWriteLock> setLocks = new ConcurrentHashMap<Integer, ReentrantReadWriteLock>();
    private ReentrantReadWriteLock partMapLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock setMapLock = new ReentrantReadWriteLock();

    public DBLockHandler(){

    }

    public void readLockPart(String part){
        partMapLock.readLock().lock();

        if(!partLocks.containsKey(part)) {
           createPart(part);
        }

        partLocks.get(part).readLock().lock();

        partMapLock.readLock().unlock();
    }

    public void writeLockPart(String part){
        partMapLock.readLock().lock();

        if(!partLocks.containsKey(part)) {
           createPart(part);
        }

        partLocks.get(part).writeLock().lock();

        partMapLock.readLock().unlock();
    }

    public void readLockSet(int set){
        setMapLock.readLock().lock();

        if(!setLocks.containsKey(set)){
            createSet(set);
        }

        setLocks.get(set).readLock().lock();

        setMapLock.readLock().unlock();
    }

    public void writeLockSet(int set){
        setMapLock.readLock().lock();

        if(!setLocks.containsKey(set))
            createSet(set);

        setLocks.get(set).writeLock().lock();

        setMapLock.readLock().unlock();
    }

    private void createSet(int set) {
        setMapLock.readLock().unlock();
        setMapLock.writeLock().lock();

        createSetAux(set);

        setMapLock.writeLock().unlock();
        setMapLock.readLock().lock();
    }

    private void createPart(String part) {
        partMapLock.readLock().unlock();
        partMapLock.writeLock().lock();

        createPartAux(part);

        partMapLock.writeLock().unlock();
        partMapLock.readLock().lock();
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
