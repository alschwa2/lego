package ourTeam;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBLockHandler {

    private ConcurrentHashMap<String,ReentrantReadWriteLock> partLocks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
    private ConcurrentHashMap<String,ReentrantReadWriteLock> setLocks = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
    private ReentrantReadWriteLock partMapLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock setMapLock = new ReentrantReadWriteLock();

    public DBLockHandler(){

    }

    public void readLockPart(String part){
        partMapLock.readLock().lock();

        if(!partLocks.containsKey(part)) {
            partMapLock.readLock().unlock();
            partMapLock.writeLock().lock();

            createPart(part);

            partMapLock.writeLock().unlock();
            partMapLock.readLock().lock();
        }

        partLocks.get(part).readLock().lock();

        partMapLock.readLock().unlock();
    }


    private void createPart(String part) {
        if(!partLocks.containsKey(part))
            partLocks.put(part, new ReentrantReadWriteLock());
    }


}
