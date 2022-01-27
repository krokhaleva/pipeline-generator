package generator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.RDF;

public class PipelineGenerator {
	
	private static final Model rules = ModelFactory.createDefaultModel();

	private static final String OP   = "urn:op#";
	private static final String QOS  = "urn:qos#";
	private static final String SEL  = "urn:sel#";
	private static final String VAR  = "urn:var#";
	private static final String PL   = "urn:platform#";
	private static final String RULE = "urn:rule#";
	private static final String ASTU = "http://ontology.adms.ru/UIP/md/2021-1#";
	
	
	private static final Property SELTYPE 		= rules.createProperty(SEL, "type");
	private static final Property SELLINK 		= rules.createProperty(SEL, "link");
	private static final Property SELTARGETTYPE = rules.createProperty(SEL, "targetType");
	private static final Property OPDEP 		= rules.createProperty(OP,  "dep");
	private static final RDFNode  OPLOAD 		= rules.createResource(OP + "Load");
	private static final RDFNode  OPLOADLINKS 	= rules.createResource(OP + "LoadLinks");
	
	
	
	public static Model generate(Model ontology) {
		
		Model rules = ModelFactory.createDefaultModel();
		
		List<RDFNode> typesList = getTypes(ontology);		
		Map<RDFNode, List<RDFNode>> types2SuperclassesMap = getTypesSuperClasses(typesList, ontology);		
		List<RDFNode> sortedTypes = sortTypes(typesList, types2SuperclassesMap);		
		List<List<RDFNode>> objectProperties = getObjectProperties(typesList, ontology);
		
		generateRules(sortedTypes, types2SuperclassesMap, objectProperties, rules);		
		
		return rules;		
	}

	private static List<List<RDFNode>> getObjectProperties(List<RDFNode> typesList, Model ontology) {
		
		List<List<RDFNode>> objectProperties = new ArrayList<List<RDFNode>>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("VALUES ?t { ");
				
		for (RDFNode node : typesList) {
			sb.append("<" + node.asResource().getURI() + "> \n");
		}
		
		sb.append("}");
				
		final String queryObjectPropertiesString = 
				"select distinct ?pred ?source_type ?target_type "
				+ "{ \n"
				+    "?s a ?t .  \n "
				+    sb.toString()
				+ "{ \n"
				+ "{ \n"
				+ "?pred <http://www.w3.org/2000/01/rdf-schema#domain> ?t "
				+ "} \n"
				+ "UNION \n"
				+ "{ \n"
				+ "?pred <http://www.w3.org/2000/01/rdf-schema#range> ?t \n"
				+ "}\n"
				+ "}\n"
				+  "?pred <http://www.w3.org/2000/01/rdf-schema#domain> ?source_type . \n"
				+  "?pred <http://www.w3.org/2000/01/rdf-schema#range> ?target_type .\n"
				+ "} \n";
		
		Query query = QueryFactory.create(queryObjectPropertiesString);		
		QueryExecution qe = QueryExecutionFactory.create(query, ontology);		
	    ResultSet results =  qe.execSelect();	    
	    while (results.hasNext()) {	    	
	    	QuerySolution qs = results.next();
	    	List<RDFNode> objectProperty = new ArrayList<RDFNode>();
	    	objectProperty.add(0, qs.get("source_type"));
	    	objectProperty.add(1, qs.get("pred"));	    	
	    	objectProperty.add(2, qs.get("target_type")); 
	    	objectProperties.add(objectProperty);
	    }			    
	    qe.close();	
		
		
		return objectProperties;
	}

	private static ArrayList<RDFNode> getTypes(Model ontology) {
		
		ArrayList<RDFNode> typesList = new ArrayList<RDFNode>();
		
		final String queryTypesString = 
				"select distinct ?t "
				+ "{ "
				+ "  ?s a ?t . "
				+ "filter ( ?t != <https://www.w3.org/2002/07/owl#Class> "
				+ "&& ?t !=  <https://www.w3.org/2002/07/owl#ObjectProperty> )"
				+ "} \n ";
		
		Query query = QueryFactory.create(queryTypesString);		
		QueryExecution qe = QueryExecutionFactory.create(query, ontology);		
	    ResultSet results =  qe.execSelect();	    
	    while (results.hasNext()) {	    	
	    	QuerySolution qs = results.next();
			RDFNode node = qs.get("t");
			typesList.add(node);	    	
	    }			    
	    qe.close();		
		return typesList;
	}
	
	private static Map<RDFNode, List<RDFNode>> getTypesSuperClasses(List<RDFNode> typesList, Model ontology) {
		
		Map<RDFNode, List<RDFNode>> types2SuperClassesMap = new HashMap<RDFNode, List<RDFNode>>();
		
		final String querySuperclassesString = 
				"select ?t ?superclass "
				+ "{ "
				+ "  ?t <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?superclass "
				+ "} \n ";
		
		Query query = QueryFactory.create(querySuperclassesString);		
		QueryExecution qe = QueryExecutionFactory.create(query, ontology);		
	    ResultSet results =  qe.execSelect();	    
	    while (results.hasNext()) {	    	
	    	QuerySolution qs = results.next();
			RDFNode type = qs.get("t");
			RDFNode superClass = qs.get("superclass");
			if (types2SuperClassesMap.containsKey(type)) {
				types2SuperClassesMap.get(type).add(superClass);
			} else {
				List<RDFNode> sc = new ArrayList<RDFNode>();
				sc.add(superClass);
				types2SuperClassesMap.put(type, sc);
			}
	    }			    
	    qe.close();	
	    
	    return types2SuperClassesMap;
		
	}
	
	private static List<RDFNode> sortTypes(List<RDFNode> typesList, Map<RDFNode, List<RDFNode>> types2SuperClassesMap) {
		
		List<RDFNode> sortedTypesList = new ArrayList<RDFNode>();
		
		Graph types2SuperClassesGraph = new Graph(typesList.size());
	    for (Map.Entry<RDFNode, List<RDFNode>> entry : types2SuperClassesMap.entrySet()) {
	    	for (RDFNode node : entry.getValue()) {
	    		types2SuperClassesGraph.addEdge(typesList.indexOf(entry.getKey()), typesList.indexOf(node));
	    	}
	    }
	    
		Stack<Integer> stack = types2SuperClassesGraph.topologicalSort();
		
		while (stack.empty() == false) {
			sortedTypesList.add(typesList.get(stack.pop()));
		}
		
		return sortedTypesList;
		
	}
	
	private static void generateRules(List<RDFNode> sortedTypesList, Map<RDFNode, List<RDFNode>> types2SuperClassesMap, List<List<RDFNode>> objectProperties, Model model) {
		
		/*RULE:load_type#111 a op:Load ;
	    	sel:type TYPE ;
	    	op:dep RULE:load_type#110.*/
		
		int i = 1;
		Collections.reverse(sortedTypesList);
		Map<RDFNode, RDFNode> typesRulesMap = new HashMap<RDFNode, RDFNode>();
		for (RDFNode node : sortedTypesList) {
			
			Resource s = new ResourceImpl(RULE + "load_type" + i);
			typesRulesMap.put(node, s);
			List<RDFNode> deps = types2SuperClassesMap.get(node);
		
	        model.add(model.createStatement(s, RDF.type, OPLOAD));
	        model.add(model.createStatement(s, SELTYPE, node));
	        if (deps != null) {
		        for (RDFNode dep : deps) {
		        	model.add(model.createStatement(s, OPDEP, typesRulesMap.get(dep)));
		        }
	        }
			
			i++;
		}
		
		/*RULE:load_links#123 a op:LoadLinks;
			sel:type SOURCETYPE; 
			sel:link PRED ;
			sel:targetType TARGETTYPE;
			op:dep RULE:load_links#001.
		 */

		for (List<RDFNode> list: objectProperties) {			
			
			Resource s = new ResourceImpl(RULE + "load_links" + i);
				
	        model.add(model.createStatement(s, RDF.type, OPLOADLINKS));
	        model.add(model.createStatement(s, SELTYPE, list.get(0)));
	        model.add(model.createStatement(s, SELLINK, list.get(1)));
	        model.add(model.createStatement(s, SELTARGETTYPE, list.get(2)));
	        model.add(model.createStatement(s, OPDEP, typesRulesMap.get(list.get(0))));
	        model.add(model.createStatement(s, OPDEP, typesRulesMap.get(list.get(2))));
	        
	        i++;
		
		}

	}

} 