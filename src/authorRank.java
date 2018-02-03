/**
 * 
 */

/**
 * @author snehal vartak
 *
 */
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections15.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.PajekNetReader;

public class authorRank {
	private static final String indexPath = "D:/IU/Search_workspace/Assignment_3/author_index/";
	private static final String graphFile = "D:\\IU\\Search_workspace\\Assignment_3\\author.net";
	
	public static void main(String args[]) throws IOException {
		
		PajekNetReader netReader = new PajekNetReader(FactoryUtils.instantiateFactory( Object.class ));
		
		Graph<Integer, Integer> graph = new UndirectedSparseGraph<Integer, Integer>();
		netReader.load(graphFile, graph);

		
		PageRank<Integer,Integer> pr= new PageRank<>(graph, 0.1f);
		pr.setTolerance(0.85);
		pr.evaluate(); //calculate page rank score for all authors in the graph
		
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		HashMap<String,String> authorNames = new HashMap<String,String>();
		HashMap<String,Double> vertex = new HashMap<String,Double>();
		for(int v : graph.getVertices()) {
			
			String vertexLabel = (String)netReader.getVertexLabeller().transform(v);
			vertex.put(vertexLabel, pr.getVertexScore(v));
			//System.out.println(pr.getVertexScore(v)+ " "+ vertexLabel+"\n");
		}

		for(int i=0; i < reader.maxDoc(); i++){
			
			authorNames.put(reader.document(i).get("authorid"),reader.document(i).get("authorName"));		
		}
		
		Map<String,Double> result = sortByRelevance(vertex);
		int count = 1;
		System.out.println("Top 10 Ranked Authors are (Author Name, Author id):");
		for(String id : result.keySet()) {
			if(count < 11) {
				System.out.println(authorNames.get(id) + ", " + id); 
			}
			count++;
		} 
		
	}
	// Below function is taken from https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java 
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByRelevance(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(
						Map.Entry::getKey, 
						Map.Entry::getValue, 
						(e1, e2) -> e1, 
						LinkedHashMap::new));
	}
}
