import java.util.Random;

/*
	This is going to be a dummy implementation of the DBmanager that the application team uses to test the application.
	See header comment in DBManager for more info. 
*/

public class DBMRandom implements DBManager {

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