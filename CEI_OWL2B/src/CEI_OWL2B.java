import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;

public class CEI_OWL2B {

	public static void main(String[] args) {

		FileInputStream in = null;
		OntModel ontologie = ModelFactory.createOntologyModel();
		
		File file = new File("test.txt");
		String ontologyName = "";
		FileWriter writer;

		try 
		{
		   in=new FileInputStream("time.owl");
		} 
		catch (FileNotFoundException ex) 
		{
			ex.printStackTrace();
		}
		
		System.out.println("Lecture de l'ontologie...");
		ontologie.read(in,null,null); 
		
		ExtendedIterator<OntClass> it = ontologie.listNamedClasses();
		ExtendedIterator<OntProperty> it3 = ontologie.listOntProperties();
		
		try {
			writer = new FileWriter(file);
			writer.write("CONTEXT\r\n\tTimeContext\r\nSETS\r\n\tThing\r\nCONSTANTS\r\n");
			
			// Ajout des classes du modèle au fichier texte
			System.out.println("Ecriture des classes...");
			while(it.hasNext())
			{
				OntClass s = it.next();
				writer.write("\t"+s.toString().replaceAll("(\\S)*#", "")+"\r\n");
			}
			
			// Ajout des properties
			System.out.println("Ecriture des properties...");
			while(it3.hasNext())
			{
				OntProperty s = it3.next();
				writer.write("\t"+s.toString().replaceAll("(\\S)*#", "")+"\r\n");
			}
			
			// Ajout des axiomes
			System.out.println("Ecriture des axiomes...");
			writer.write("AXIOMS\r\n");
			it = ontologie.listNamedClasses();
			int axmCount=1;
			while(it.hasNext())
			{
				OntClass s = it.next();
				ExtendedIterator<OntClass> itSup = s.listSuperClasses();
				while(itSup.hasNext()){
					OntClass sup = itSup.next();
					writer.write("\t"+"@axm"+Integer.toString(axmCount)+ " " +s.toString().replaceAll("(\\S)*#", "")+" <: "+sup.toString().replaceAll("(\\S)*#", "")+"\r\n");
					axmCount++;
				}
				
			}
			
			writer.close();
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Done !");

	}

}
