
package net.sourceforge.filebot.ui.panel.rename;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "history")
class History {
	
	@XmlElement(name = "sequence")
	private List<Sequence> sequences = new ArrayList<Sequence>();
	
	
	public static class Sequence {
		
		@XmlAttribute(name = "date", required = true)
		private Date date;
		
		@XmlElement(name = "rename", required = true)
		private List<Element> elements;
		
		
		private Sequence() {
			// hide constructor
		}
		

		public Date date() {
			return date;
		}
		

		public List<Element> elements() {
			return elements;
		}
	}
	

	public static class Element {
		
		@XmlAttribute(name = "dir", required = true)
		private File dir;
		
		@XmlAttribute(name = "from", required = true)
		private String from;
		
		@XmlAttribute(name = "to", required = true)
		private String to;
		
		
		private Element() {
			// hide constructor
		}
		

		public File dir() {
			return dir;
		}
		

		public File from() {
			return new File(dir, from);
		}
		

		public File to() {
			return new File(dir, to);
		}
	}
	
	
	public List<Sequence> sequences() {
		return Collections.unmodifiableList(sequences);
	}
	

	public void add(Iterable<Entry<File, File>> elements) {
		Sequence sequence = new Sequence();
		
		sequence.date = new Date();
		sequence.elements = new ArrayList<Element>();
		
		for (Entry<File, File> entry : elements) {
			File from = entry.getKey();
			File to = entry.getValue();
			
			// sanity check, parent folder must be the same for both files
			if (!from.getParentFile().equals(to.getParentFile())) {
				throw new IllegalArgumentException(String.format("Illegal entry: ", entry));
			}
			
			Element element = new Element();
			
			element.dir = from.getParentFile();
			element.from = from.getName();
			element.to = to.getName();
			
			sequence.elements.add(element);
		}
		
		sequences.add(sequence);
	}
	

	public void add(History other) {
		this.sequences.addAll(other.sequences);
	}
	

	public int size() {
		return sequences.size();
	}
	

	public void clear() {
		sequences.clear();
	}
	

	public void store(File file) throws JAXBException {
		Marshaller marshaller = JAXBContext.newInstance(History.class).createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		marshaller.marshal(this, file);
	}
	

	public void load(File file) throws JAXBException {
		Unmarshaller unmarshaller = JAXBContext.newInstance(History.class).createUnmarshaller();
		
		History history = ((History) unmarshaller.unmarshal(file));
		
		clear();
		add(history);
	}
}
