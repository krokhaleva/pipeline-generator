package generator;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;


public class TestGenerator {

	public static void main(String[] args) throws IOException {
		
		Model model = ModelFactory.createDefaultModel();
		org.apache.jena.query.ARQ.init();
		
		InputStream input = TestGenerator.class.getResourceAsStream("/resources/ontology.n3");
        if (input == null) {
        	input = TestGenerator.class.getClassLoader().getResourceAsStream("ontology.n3");
        }        
        RDFDataMgr.read(model, input, Lang.NTRIPLES);
        input.close();
        
		System.out.println("\nResult:");
		RDFDataMgr.write(System.out, PipelineGenerator.generate(model), Lang.TTL);

	}
	
}
