import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.SimilarityBase;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import PairPac.*;

public class TFIDF_bnn_bnn {
    private IndexSearcher searcher; // lucene IndexSearcher
    private QueryParser parser; // lucene QueryParser

    private int numDocs; // no of documents to be returned
    private List<String> queryPageList; // List of pages to query

    // Map of queries list to map of Documents list to scores for that query
    public Map<String, List<DocumentResult>> queryResults;


    public TFIDF_bnn_bnn(List<String> pl, int n) throws IOException{
        numDocs = n;
        queryPageList = pl; // Each page title will be used as a query

        // Parse the postbody field using StandardAnalyzer
        String fields[] = {"question","answer"};
        parser = new MultiFieldQueryParser(fields, new StandardAnalyzer());



        String INDEX_DIRECTORY =  "./index";

        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));

        // Set bnn_bnn similarity class which computes tf[t,d]
        SimilarityBase bnn_bnn = new SimilarityBase() {
            protected float score(BasicStats stats, float freq, float docLen) {
                return freq > 0 ? 1 : 0;
            }

            @Override
            public String toString() {
                return null;
            }
        };
        searcher.setSimilarity(bnn_bnn);
    }



    public Map<String, List<DocumentResult>> getResults() throws IOException{
        queryResults = new HashMap<>();

        Map<String,String> QAmap = new HashMap<>();
        for (String page : queryPageList) { // For every post in posts.xml
            HashMap<Integer, Float> scores = new HashMap<>(); // Mapping of each
            // Document to
            // its score
            Map<Integer,Pair> map = new HashMap<>();
            HashMap<Integer, DocumentResult> docMap = new HashMap<>();
            HashMap<TermQuery, Float> queryweights = new HashMap<>();
            List<TermQuery> terms = new ArrayList<>();
            PriorityQueue<DocumentResult> docQueue = new PriorityQueue<>(new ResultComparator());



            ArrayList<DocumentResult> docResults = new ArrayList<>();


            for (String term : page.split(" ")) { // For every word in page
                // name...
                // Take word as query term
                // for postbody
                TermQuery postq = new TermQuery(new Term("answer", term));
                TermQuery titleq = new TermQuery(new Term("question", term));
                terms.add(postq);
                terms.add(titleq);
                // check if query is present then add one else add 0;
                queryweights.put(postq, queryweights.getOrDefault(postq, 0.0f) + 1.0f);
                queryweights.put(titleq, queryweights.getOrDefault(titleq, 0.0f) + 1.0f);
            }
            // For every Term
            for (TermQuery query : terms) {

                // Index Reader for calculation
                IndexReader reader = searcher.getIndexReader();

                // float df = (reader.docFreq(query.getTerm()) == 0) ? 0 : 1;

                float DF = (reader.docFreq(query.getTerm()) == 0) ? 1 : reader.docFreq(query.getTerm());
                float qTF = 1; // boolean term frequency

                float qdf = DF;
                float qWeight = qTF * qdf;

                queryweights.put(query, qWeight);

                // Get the topN documents that match our query
                TopDocs tpd = searcher.search(query, numDocs);
                for (int i = 0; i < tpd.scoreDocs.length; i++) { // For every
                    // returned
                    // document...
                    Document doc = searcher.doc(tpd.scoreDocs[i].doc); // Get
                    // the
                    // document

                    int docId = Integer.parseInt(doc.get("id"));
                    double score = tpd.scoreDocs[i].score * queryweights.get(query); // Calculate
                    // TF-IDF
                    // for
                    // document


                    DocumentResult dResults = docMap.get(docId);
                    if (dResults == null) {
                        dResults = new DocumentResult(docId, (float) score);
                    }
                    float prevScore = dResults.getScore();
                    // Store score for later use
                    scores.put(Integer.parseInt(doc.get("id")), (float) (prevScore + score));
                    QAmap.put(doc.get("question"),doc.get("answer"));
                    map.put(Integer.parseInt(doc.get("id")),new Pair(doc.get("question"),doc.get("answer"),(float) (prevScore + score)));

                }
            }


            Iterator<Map.Entry<Integer, Pair>> iter_map = map.entrySet().iterator();
            while (iter_map.hasNext()){
                Map.Entry<Integer,Pair> e1 = iter_map.next();
                int docId = e1.getKey();
                Pair p = e1.getValue();

                String question = p.getQuestion();
                String answer = p.getAnswer();
                float score = p.getScore();
                scores.put(docId, score / scores.size());
                DocumentResult docResult = new DocumentResult(docId, score,question,answer);
                docQueue.add(docResult);
            }

            // For every document and its corresponding score...
//            for (Map.Entry<Integer, Float> entry : scores.entrySet()) {
//                int docId = entry.getKey();
//                Float score = entry.getValue();
//
//                scores.put(docId, score);
//
//                DocumentResult docResult = new DocumentResult(docId, score);
//                docQueue.add(docResult);
//            }

            int rankCount = 1;
            DocumentResult current;
            while ((current = docQueue.poll()) != null) {
                current.setRank(rankCount);
                docResults.add(current);
                rankCount++;
                if (rankCount >= numDocs)
                    break;
            }

            // Map our Documents and scores to the corresponding query
            queryResults.put(page, docResults);
        }

        return queryResults;
    }


    public void write() throws IOException {
        System.out.println("TFIDF_bnn_bnn writing results to: " + "/"
                +  "bnn-bnn.run");
        FileWriter runfileWriter = new FileWriter(
                new File("./" + "bnn-bnn.run"));
        for (Map.Entry<String, List<DocumentResult>> results : queryResults.entrySet()) {
            String query = results.getKey();
            List<DocumentResult> list = results.getValue();
            for (int i = 0; i < list.size(); i++) {
                DocumentResult dr = list.get(i);
                runfileWriter.write(query.replace(" ", "-") + " Q0 " + dr.getId() + " " + dr.getRank() + " "
                        + dr.getScore() +  " group7-TFIDF_bnn_bnn\n");
            }
        }
        runfileWriter.close();

    }
}
