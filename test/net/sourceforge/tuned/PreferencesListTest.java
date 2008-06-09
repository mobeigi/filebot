
package net.sourceforge.tuned;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.prefs.Preferences;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class PreferencesListTest {
	
	private static Preferences root;
	private static Preferences strings;
	private static Preferences numbers;
	private static Preferences temp;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		root = Preferences.userRoot().node("filebot-test/PreferencesList");
		
		strings = root.node("strings");
		strings.put("0", "Rei");
		strings.put("1", "Firefly");
		strings.put("2", "Roswell");
		strings.put("3", "Angel");
		strings.put("4", "Dead like me");
		strings.put("5", "Babylon");
		
		numbers = root.node("numbers");
		numbers.putInt("0", 4);
		numbers.putInt("1", 5);
		numbers.putInt("2", 2);
		
		temp = root.node("temp");
	}
	

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		root.removeNode();
	}
	

	@Test
	public void get() {
		List<String> list = PreferencesList.map(strings, String.class);
		
		assertEquals("Rei", list.get(0));
		assertEquals("Roswell", list.get(2));
		assertEquals("Babylon", list.get(5));
	}
	

	@Test
	public void testAdd() {
		List<Integer> list = PreferencesList.map(numbers, Integer.class);
		
		list.add(3);
		
		assertEquals("3", numbers.get("3", null));
	}
	

	@Test
	public void remove() {
		temp.put("0", "Gladiator 1");
		temp.put("1", "Gladiator 2");
		temp.put("2", "Gladiator 3");
		temp.put("3", "Gladiator 4");
		
		List<String> list = PreferencesList.map(temp, String.class);
		
		assertEquals("Gladiator 2", list.remove(1));
		assertEquals("Gladiator 4", list.remove(2));
		assertEquals("Gladiator 1", list.remove(0));
	}
	

	@Test
	public void setEntry() {
		List<String> list = PreferencesList.map(strings, String.class);
		
		list.set(3, "Buffy");
		
		assertEquals(strings.get("3", null), "Buffy");
	}
	

	@Test
	public void toArray() throws Exception {
		List<String> list = PreferencesList.map(strings, String.class);
		
		assertArrayEquals(list.subList(0, 3).toArray(), new Object[] { "Rei", "Firefly", "Roswell" });
	}
}
