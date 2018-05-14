import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Request implements Serializable
{
	private HashMap<Integer, Integer> sets;

	public Request() {
		this.sets = new HashMap<Integer, Integer>();
	}

	public HashMap<Integer, Integer> getSets() {
		return new HashMap<Integer, Integer>(this.sets);
	}

	public void addSet(Integer setID) {
		addSet(setID, 1);
	}

	public void addSet(int setID, int amount) {
		int currentAmount = sets.get(setID) == null ? 0 : sets.get(setID);
		amount += currentAmount;
		sets.put(setID, amount);
	}

	@Override
	public String toString() {
		String returnString = "Request -> ";
		for (Map.Entry<Integer, Integer> e : sets.entrySet()) {
			returnString += e.getKey() + ": " + e.getValue();
			returnString += "; ";
		}
		return returnString; 
	}
}