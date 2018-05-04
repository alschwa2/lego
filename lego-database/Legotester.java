import java.io.*;
import java.util.*;



/*
 * Tentative plan:
 ignore inventory_sets. Only deal with "inventories", "inventory_parts", "sets"
 delete all rows that are not represented in all three tables.

 Figure out how to use dynamo and the dbs
 */
public class Legotester {
	public static void main(String[] args) throws IOException {
		testOverlap("inventories.csv", 2, "sets.csv", 0);
		testOverlap("inventories.csv", 0, "inventory_parts.csv", 0);
		testOverlap("inventories.csv", 0, "inventory_sets.csv", 0);
		testOverlap("inventories.csv", 2, "inventory_sets.csv", 1);
	}

	private static void test1(String[] args) throws IOException {
		String fileName = args[0];
		try {
			int columnIndex = Integer.parseInt(args[1]);
			howManyUniqueColumn(fileName, columnIndex);
		} catch(NumberFormatException e) {
			e.printStackTrace();
		}
	}

	private static void howManyUniqueColumn(String fileName, int columnIndex) throws IOException {
		HashSet<String> values = new HashSet<String>();
		Scanner scanner = new Scanner(new File(fileName));
		scanner.nextLine();
		int total = 0;
		while(scanner.hasNextLine()) {
			String value = scanner.nextLine().split(",")[columnIndex];
			values.add(value);
			total++;
		}
		System.out.println(values.size());
		System.out.println(total);
		scanner.close();
	}

	private static void testOverlap(String file1, int column1, String file2, int column2) throws IOException {
		Scanner scanner1 = new Scanner(new File(file1));
		Scanner scanner2 = new Scanner(new File(file2));

		HashSet<String> set1 = new HashSet<String>();
		String columnName1 = scanner1.nextLine().split(",")[column1];
		while (scanner1.hasNextLine()) set1.add(scanner1.nextLine().split(",")[column1]);
		scanner2.nextLine();
		int failures1 = 0;
		while(scanner2.hasNextLine()) {
			if (set1.add(scanner2.nextLine().split(",")[column2])) failures1++;
		}
		scanner1.close();
		scanner2.close();

		scanner1 = new Scanner(new File(file1));
		scanner2 = new Scanner(new File(file2));

		HashSet<String> set2 = new HashSet<String>();
		String columnName2 = scanner2.nextLine().split(",")[column2];
		while (scanner2.hasNextLine()) set2.add(scanner2.nextLine().split(",")[column2]);
		scanner1.nextLine();
		int failures2 = 0;
		while(scanner1.hasNextLine()) {
			if (set2.add(scanner1.nextLine().split(",")[column1])) failures2++;
		}
		scanner1.close();
		scanner2.close();

		System.out.println("In " + file2 + "[" + columnName2 + "] not " + file1 + "[" + columnName1 + "]: " + failures1 + "\nIn " + file1 + "[" + columnName1 + "] not " + file2 + "[" + columnName2 + "]: " + failures2);
	}
}