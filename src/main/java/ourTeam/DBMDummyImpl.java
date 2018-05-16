package ourTeam;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Random;

public class DBMDummyImpl implements DBManager {

    private HashMap<Integer, Integer> setsInStock;
    private HashMap<Integer, Set<String>> setToParts;
    private HashMap<String, Integer> partsInStock;

    private Random random;

    public DBMDummyImpl() {
        this.setsInStock = new HashMap<>();
        this.setToParts = new HashMap<>();
        this.partsInStock = new HashMap<>();

        this.random = new Random();
    }

    public Integer getPartCount(int set, String part) {
        Integer amount = partsInStock.get(part);
        if (amount == null) return 0;
        else return amount;
    }

    public void decrementSet(int set, int amount) {
        int newAmount = setsInStock.get(set) - amount;
        if (newAmount < 0) throw new IllegalArgumentException("You are trying to decrement to less than zero");
        setsInStock.put(set, newAmount);
    }

    public Set<String> getParts(int set) {
        Set<String> parts = setToParts.get(set);
        if (parts == null) {
            parts = new HashSet<String>();
            for (int i = 0; i < 30; i++) parts.add("" + random.nextInt(1000));
            setToParts.put(set, new HashSet<>(parts));
        }
        return parts;
    }

    public void incrementPart(int set, String part, int amount) {
        Integer amountInStock = partsInStock.get(part);
        if (amountInStock == null) amountInStock = 0;
        partsInStock.put(part, amount + amountInStock);
    }

    public Integer getSetCount(int set) {
        Integer amountInStock = setsInStock.get(set);
        if (amountInStock == null) amountInStock = 0;
        return amountInStock;
    }

    public void incrementSet(int set, int amount) {
        Integer amountInStock = setsInStock.get(set);
        if (amountInStock == null) amountInStock = 0;
        setsInStock.put(set, amount + amountInStock);
    }

    public void decrementPart(int set, String part, int amount) {
        int newAmount = partsInStock.get(part) - amount;
        if (newAmount < 0) throw new IllegalArgumentException("You are trying to decrement to less than zero");
        partsInStock.put(part, newAmount);
    }
}
