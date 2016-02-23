import java.util.Iterator;
import java.util.Vector;

import org.apache.jena.ontology.IntersectionClass;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.ontology.UnionClass;
import org.apache.jena.rdf.model.Resource;
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

	// Génère les constantes associées à une ontologie
	public Vector<String> extractConstants (OntModel ontologie)
	{
		Vector<String> result = new Vector<String>();
		ExtendedIterator<OntClass> it = ontologie.listNamedClasses();
		ExtendedIterator<OntProperty> it2 = ontologie.listOntProperties();

		// Lecture des classes
		while(it.hasNext())
		{
			OntClass s = it.next();
			result.add(shortenURL(s));
		}

		// Lecture des properties
		while(it2.hasNext())
		{
			OntProperty s = it2.next();
			if(s.getURI().contains(name))
			{
				result.add(shortenURL(s));
			}
		}

		return result;
	}

	// Génère les axiomes associés à une ontologie
	public Vector<String> extractAxioms(OntModel ontologie)
	{
		Vector<String> result = new Vector<String>();

		// Gestion des déclarations de classes
		ExtendedIterator<OntClass> it = ontologie.listNamedClasses();
		while(it.hasNext())
		{
			OntClass s = it.next();
			String name = shortenURL(s);

			// Gestion des statements subClassOf
			ExtendedIterator<OntClass> superClasses = s.listSuperClasses();
			while(superClasses.hasNext())
			{
				OntClass superClass = superClasses.next();
				String axiom = interpretClass(superClass);
				if(axiom != "")
					result.add(name + " <: " + axiom);
			}
			
			// Gestion des classes déclarées par équivalence
			ExtendedIterator<OntClass> equivalentClasses = s.listEquivalentClasses();
			while(equivalentClasses.hasNext())
			{
				OntClass equivalentClass = equivalentClasses.next();
				String axiom = interpretClass(equivalentClass);
				if(axiom != "")
					result.add(name + " = " + axiom);	
			}

			ExtendedIterator<OntClass> disjoint = s.listDisjointWith();
			while(disjoint.hasNext())
			{
				result.add(name+ " /\\ "+ shortenURL(disjoint.next()) + " = {}");
			}
		}

		// Gestion de la déclaration des propriétés
		ExtendedIterator<OntProperty> it2 = ontologie.listAllOntProperties();
		while(it2.hasNext())
		{
			OntProperty s = it2.next();
			if(s.getURI().contains(name))
			{
				String name = shortenURL(s);
				
				ExtendedIterator<? extends OntProperty> superProperties = s.listSuperProperties();

				// Déclaration des propriétés
				if(!superProperties.hasNext())
				{
					String symbole = s.isFunctionalProperty() ? " --> " : " <-> ";
					result.add(name + " : " + shortenURL(s.getDomain()) + symbole + shortenURL(s.getRange()));
				}
				while(superProperties.hasNext())
				{
					result.add(name + " <: " + shortenURL(superProperties.next()));
				}

				// Gestion des inverses
				if(s.hasInverse())
				{
					result.add(name + " = " + shortenURL(s.getInverse()) + "~");
				}
				// Gestion des propriétés transitives
				if(s.isTransitiveProperty())
				{
					result.add(name + " circ " + name + " <: " + name);
				}
				// Gestion des propriétés symétriques
				if(s.isSymmetricProperty())
				{
					result.add("id <: " + name + " circ " + name);
					
				}
				
				//TODO: gérer les fonctions réflexives
			}
		}

		return result;
	}

	// Interprète une restriction en retournant le texte correspondant en Event-B
	public String interpretRestriction(Restriction r)
	{
		String result = "";
		
		if(r.isSomeValuesFromRestriction())
		{
			result += "dom(" + shortenURL(r.getOnProperty()) + " |> " + interpretClass((OntClass) r.asSomeValuesFromRestriction().getSomeValuesFrom()) + ")";			
		}
		else if (r.isCardinalityRestriction())
		{
			if(r.asCardinalityRestriction().getCardinality() == 1)
			{
				result += "dom(" + shortenURL(r.getOnProperty()) + ")";			
			}
		}
		else if (r.isHasValueRestriction() || (r.isMinCardinalityRestriction() && r.asMinCardinalityRestriction().getMinCardinality() == 1))
		{
			result += "dom(" + shortenURL(r.getOnProperty()) + ")";			
		}
			
		return result;
	}
	
	// Interprète une classe en retournant le texte correspondant en Event-B
	public String interpretClass (OntClass c)
	{
		String result = "";
		
		// Classe existante
		if(constants.contains(shortenURL(c)))
		{
			result += shortenURL(c);
		}
		// Restriction
		else if (c.isRestriction())
		{
			result += interpretRestriction(c.asRestriction());
		}
		// Intersection ou Union
		else
		{
			String operateur ="";
			Iterator i =null;
			try
			{
				UnionClass ic = c.asUnionClass();
				i = ic.listOperands(); i.hasNext();
				operateur = " \\/ ";
			} catch(Exception e){}
			
			try
			{
				IntersectionClass ic = c.asIntersectionClass();
				i = ic.listOperands(); i.hasNext();
				operateur = " /\\ ";
			} catch(Exception e){}
			
			if(i != null){
				result += "(";

				while(i.hasNext() ) {
					OntClass op = (OntClass) i.next();
					
					result += interpretClass(op);

					if(i.hasNext())
						result += operateur;
					
				}
				result += ")";
			}
		}
		
		return result;
	}
	
	// Enlève le nom de l'ontologie d'une URL
	public static String shortenURL (Resource r)
	{
		return r.toString().replaceAll("(\\S)*#", "");
	}
	
	// Retourne le fichier contexte Event-B associé à ce contexte
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
