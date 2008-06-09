
package net.sourceforge.tuned;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import net.sourceforge.tuned.FunctionIterator.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class FunctionIteratorTest {
	
	@Parameters
	public static Collection<Object[]> createParameters() {
		List<String> data = new ArrayList<String>();
		
		data.add("http://filebot.sourceforge.net");
		data.add("http://javapuzzlers.com");
		data.add("http://www.google.com");
		data.add("Vanessa Mae - Classical Gas"); // invalid URI
		
		return TestUtil.asParameters(TestUtil.rotations(data).toArray());
	}
	
	private final List<String> data;
	
	
	public FunctionIteratorTest(List<String> data) {
		this.data = data;
	}
	

	@Test
	public void skipNull() {
		Iterator<String> iterator = new FunctionIterator<String, String>(data, new FilterFunction("filebot"));
		
		String result = iterator.next();
		
		assertEquals("http://filebot.sourceforge.net", result);
	}
	

	@Test(expected = NoSuchElementException.class)
	public void noMoreNext() {
		Iterator<URI> iterator = new FunctionIterator<String, URI>(new ArrayList<String>(), new UriFunction());
		
		iterator.next();
	}
	

	@Test(expected = IllegalArgumentException.class)
	public void throwException() {
		Iterator<URI> iterator = new FunctionIterator<String, URI>(data, new UriFunction());
		
		while (iterator.hasNext()) {
			iterator.next();
		}
	}
	

	@Test
	public void iterate() {
		Iterator<URI> iterator = new FunctionIterator<String, URI>(data, new UriFunction());
		
		List<URI> values = new ArrayList<URI>();
		
		while (iterator.hasNext()) {
			try {
				values.add(iterator.next());
			} catch (Exception e) {
				
			}
		}
		
		assertTrue(values.size() == 3);
	}
	
	
	private static class UriFunction implements Function<String, URI> {
		
		@Override
		public URI evaluate(String sourceValue) {
			return URI.create(sourceValue);
		}
	}
	

	private static class FilterFunction implements Function<String, String> {
		
		private final String filter;
		
		
		public FilterFunction(String filter) {
			this.filter = filter;
		}
		

		@Override
		public String evaluate(String sourceValue) {
			if (sourceValue.contains(filter))
				return sourceValue;
			
			return null;
		}
	}
	
}
