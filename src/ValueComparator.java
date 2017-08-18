import java.util.*;

public class ValueComparator<K , V extends Comparable<V>> implements Comparator<K>
{
	private Map<K, V> map;
	
	public ValueComparator(Map<K, V> map) {
		this.map = map;
	}
	
	public static<K, V extends Comparable<V>> Map<K, V> sortByValue(Map<K, V> unsortedMap)
	{
		Map<K, V> sortedMap = new
		TreeMap<K, V>(new ValueComparator<K, V>(unsortedMap));
		sortedMap.putAll(unsortedMap);
		return sortedMap;
	}
	
	@Override
	public int compare(K keyA, K keyB) {
		Comparable<V> valueA = map.get(keyA);
		V valueB = map.get(keyB);
		return valueA.compareTo(valueB);
	}
	
}
