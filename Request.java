import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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