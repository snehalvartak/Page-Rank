import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections15.FactoryUtils;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.TransformerUtils;
import org.apache.commons.collections15.functors.MapTransformer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.algorithms.scoring.PageRankWithPriors;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.io.PajekNetReader;

public class authorRankWithQuery {
	
	private static final String indexPath = "D:/IU/Search_workspace/Assignment_3/author_index/";
	private static final String graphFile = "D:\\IU\\Search_workspace\\Assignment_3\\author.net";
	public static void main(String args[]) throws IOException, ParseException {
		
		 calculate("Data Mining");
		 calculate("Information Retrieval");
	}
	
	// Prior Calculation and PageRank 
	public static void calculate(String queryString) throws IOException, ParseException{
		
		// Read the graph with PajekNetReader
		PajekNetReader netReader = new PajekNetReader(FactoryUtils.instantiateFactory( Object.class ));
		Graph<Integer, Integer> graph = new UndirectedSparseGraph<Integer, Integer>();
		netReader.load(graphFile, graph);
	/*	System.out.println(graph.getVertices());
		for(Integer id: graph.getVertices()) {
			String vertexLabel =(String) netReader.getVertexLabeller().transform(id);
			System.out.println(id + " " + vertexLabel);
		}*/
		// Read the index file 
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new BM25Similarity());
		Analyzer analyzer = new StandardAnalyzer();
		QueryParser parser = new QueryParser("content", analyzer); 
		
		Query query = parser.parse(QueryParser.escape(queryString));
		TopDocs results = searcher.search(query, 300);
		ScoreDoc[] docs = results.scoreDocs;
		
		//System.out.println(docs.length);
		float totalScore = 0.0f;
		// Calculating the priors and store them in a Map
		HashMap<String,Double> authorPrior = new HashMap<String, Double>(); 
		for (int k = 0; k < docs.length; k++) {
			Document document = searcher.doc(docs[k].doc);	
			if(authorPrior.containsKey(document.get("authorid"))) {
				double score = authorPrior.get(document.get("authorid"));
				authorPrior.put(document.get("authorid"), score + docs[k].score);
			}else {
				authorPrior.put(document.get("authorid"), (double) docs[k].score);
			}
			
			totalScore += docs[k].score;
        	
		}
		
		// To pass the priors to page rank, we need to store the vertex ID from the graph as the key and prior as the value
		// To get this format we do the iteration
		Map<Integer, Double> finalPrior = new HashMap<Integer,Double>();
		//Normalize the prior and initialize the authors not in the top 300 results to 0
		for(int id: graph.getVertices()) {
			String vertexLabel =(String) netReader.getVertexLabeller().transform(id);
			//int label = Integer.parseInt(vertexLabel);
			//System.out.println(label);
			if (authorPrior.containsKey(vertexLabel) ) {
				//authorPrior.put(label, authorPrior.get(label)/totalScore);
				finalPrior.put(id, authorPrior.get(vertexLabel)/totalScore);
			}
			else {
				finalPrior.put(id, 0.0);
			}
		}
		
		// Author Name to author id Mapping
		Map<String,String> authorNames = new HashMap<String,String>();
		for(int i=0; i < reader.maxDoc(); i++){
			
			authorNames.put(reader.document(i).get("authorid"),reader.document(i).get("authorName"));		
		}
		// Convert the priors to Transformer Object type
		Transformer<Integer, Double> vertexPrior = new Transformer<Integer, Double>(){
			@Override
			public Double transform(Integer s) {
				return finalPrior.get(s);
			}
		};
		//Transformer<Integer, Double> vertexPrior =MapTransformer.getInstance(authorPrior);
		PageRankWithPriors<Integer,Integer> pageRank = new PageRankWithPriors<Integer,Integer>(graph,vertexPrior ,0.1);
		pageRank.setTolerance(0.85);
		pageRank.evaluate(); //calculate page rank score for all authors in the graph
		
		HashMap<String,Double> finalRanks = new HashMap<String,Double>();
		
		for(int v : graph.getVertices()) {
			String vlabel = (String) netReader.getVertexLabeller().transform(v);
			finalRanks.put(authorNames.get(vlabel), pageRank.getVertexScore(v));
		}		
		Map<String, Double>result = sortByRelevance(finalRanks);	
			
			int count = 1;
			System.out.println("\nTop 10 Ranked Authors are (Author Name, Score)" + " for "+ queryString + ":");
			for(String id : result.keySet()) {
				if(count < 11) {
					System.out.println(id + " " + result.get(id)); 
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
