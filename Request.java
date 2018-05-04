import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/*
 * Class for the request that the client sends to the server.
 * Basically a wrapper around a Map of (String)<SET_NAME> to (int)<QUANTITY> of the set
 */
public class Request implements Serializable
{
	private HashMap<String, Integer> sets;

	public Request() {
		this.sets = new HashMap<String, Integer>();
	}

	public HashMap<String, Integer> getSets() {
		return new HashMap<String, Integer>(this.sets);
	}

	public void addSet(String setName) {
		addSet(setName, 1);
	}

	public void addSet(String setName, int amount) {
		int currentAmount = sets.get(setName) == null ? 0 : sets.get(setName);
		amount += currentAmount;
		sets.put(setName, amount);
	}

	@Override
	public String toString() {
		String returnString = "";
		for (Map.Entry<String, Integer> e : sets.entrySet()) {
			returnString += e.getKey() + ": " + e.getValue();
			returnString += "; ";
		}
		return returnString; 
	}
}