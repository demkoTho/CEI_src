import java.util.Vector;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.util.iterator.ExtendedIterator;

public class Context {

	private String name = "";
	private Vector<String> sets = new Vector<String>();
	private Vector<String> constants = new Vector<String>();
	private Vector<String> axioms = new Vector<String>();
	

	public Context (OntModel ontologie)
	{
		name = ontologie.listOntologies().next().toString();
		constants = this.extractConstants(ontologie);
		axioms = this.extractAxioms(ontologie);
		
		sets.add("Thing");
		sets.add("int");
		sets.add("string");
		sets.add("dateTime");
	}
	
	public String getName() {
		return name;
	}
	
	public Vector<String> getSets() {
		return sets;
	}

	public Vector<String> getConstants() {
		return constants;
	}

	public Vector<String> getAxioms() {
		return axioms;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSets(Vector<String> sets) {
		this.sets = sets;
	}

	public void setConstants(Vector<String> constants) {
		this.constants = constants;
	}

	public void setAxioms(Vector<String> axioms) {
		this.axioms = axioms;
	}
	
	public Vector<String> extractConstants (OntModel ontologie)
	{
		Vector<String> result = new Vector<String>();
		ExtendedIterator<OntClass> it = ontologie.listNamedClasses();
		ExtendedIterator<OntProperty> it2 = ontologie.listOntProperties();
	
		// Lecture des classes
		while(it.hasNext())
		{
			OntClass s = it.next();
			result.add(s.toString().replaceAll("(\\S)*#", ""));
		}

		// Lecture des properties
		while(it2.hasNext())
		{
			OntProperty s = it2.next();
			if(s.getURI().contains(name))
			{
				result.add(s.toString().replaceAll("(\\S)*#", ""));
			}
		}
		
		return result;
	}
	
	public Vector<String> extractAxioms(OntModel ontologie)
	{
		Vector<String> result = new Vector<String>();
		
		// Transformation subClassOf -> inclusions
		ExtendedIterator<OntClass> it = ontologie.listNamedClasses();
		while(it.hasNext())
		{
			OntClass s = it.next();
			if(s.hasSuperClass() && constants.contains(s.getSuperClass().toString().replaceAll("(\\S)*#", "")))
			{
				result.add(s.toString().replaceAll("(\\S)*#", "")+" <: "+s.getSuperClass().toString().replaceAll("(\\S)*#", ""));
			}
			else
			{
				result.add(s.toString().replaceAll("(\\S)*#", "")+" <: Thing");
			}
		}
		
		
		
		ExtendedIterator<OntProperty> it2 = ontologie.listAllOntProperties();
		while(it2.hasNext())
		{
			OntProperty s = it2.next();
			if(s.getURI().contains(name))
			{
				String name = s.toString().replaceAll("(\\S)*#", "");
				OntProperty superProperty = s.getSuperProperty();
				
				// Déclaration des properties
				if(superProperty != null)
				{
					result.add(name + " <: " + superProperty.toString().replaceAll("(\\S)*#", ""));
				}
				else
				{
					String symbole = s.isFunctionalProperty() ? " --> " : " <-> ";
					result.add(name + " : " + s.getDomain().toString().replaceAll("(\\S)*#", "") + symbole + s.getRange().toString().replaceAll("(\\S)*#", ""));
				}
				
				// Gestion des inverses
				if(s.hasInverse())
				{
					result.add(name + " = " + s.getInverse().toString().replaceAll("(\\S)*#", "") + "~");
				}
				// Gestion des property transitives
				if(s.isTransitiveProperty())
				{
					result.add(name + " circ " + name + " <: " + name);
				}
				if(s.isSymmetricProperty())
				{
					result.add(name + " circ " + name + " <: id");
				}
			}
		}
		
		return result;
	}
	
	public String toString()
	{
		String result = "CONTEXT\r\n\t"+name+"\r\n";
		
		result += "SETS\r\n";
		for(String s : sets)
		{
			result += "\t" + s + "\r\n";
		}
		
		result+= "CONSTANTS\r\n";
		for(String s : constants)
		{
			result += "\t" + s + "\r\n";
		}
		
		result+= "AXIOMS\r\n";
		int i = 1;
		for(String s : axioms)
		{
			result += "\t@axm" + i + " " + s + "\r\n";
			i++;
		}

		return result;
	}

}
