import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;

public class CEI_OWL2B {

	public static void main(String[] args) {

		FileInputStream in = null;
		OntModel ontologie = ModelFactory.createOntologyModel();

		try 
		{
		   in=new FileInputStream("travel.owl");
		} 
		catch (FileNotFoundException ex) 
		{
			ex.printStackTrace();
		}
		
		System.out.println("Lecture de l'ontologie...");
		ontologie.read(in,null,null); 
				
		Context context = new Context(ontologie);
		
		System.out.println(context);

		System.out.println("Done !");

	}

}
