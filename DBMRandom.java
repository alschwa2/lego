import java.util.Random;

public class DBMRandom extends DBManager {

	Random random;

	public DBMRandom() {
		super();
		this.random = new Random();
	}

	public boolean setIsAvailible(String set) {
		return this.random.nextBoolean();
	}
	
	public boolean partsAreAvailible(String set) {
		return this.random.nextBoolean();
	}
}