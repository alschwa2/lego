package ourTeam;

import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class DBMRandom implements DBManager {

	Random random;

	public DBMRandom() {
		this.random = new Random();
	}

    public int getPartCount(String part) {
    	return random.nextInt(2);
    }

	public void decrementSet(int set, int amount) {
		return;
	}

	public Set<String> getParts(int set) {
		HashSet<String> randomSet = new HashSet<String>();

		for (int i = 0; i < 30; i++) randomSet.add("" + random.nextInt(1000));

		return randomSet;
	}

    public void incrementPart(String part, int incrementPartsBy) {
    	return;
    }

    public int getSetQuantity(int set) {
    	return 0;
    }

    public void incrementSet(int set, int amount) {
    	return;
    }

    public void decrementPart(String part, int amount) {
    	return;
    }
}