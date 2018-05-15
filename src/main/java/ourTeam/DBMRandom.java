package ourTeam;

import java.util.Random;
import java.util.HashSet;
import java.util.Set;

class DBMRandom implements DBManager {

    Random random;

    DBMRandom() {
        this.random = new Random();
    }

    public Integer getPartCount(int set, String part) {
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

    public void incrementPart(int set, String part, int incrementPartsBy) {
        return;
    }

    public Integer getSetCount(int set) {
        return 0;
    }

    public void incrementSet(int set, int amount) {
        return;
    }

    public void decrementPart(int set, String part, int amount) {
        return;
    }
}