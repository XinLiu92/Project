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

public class TFIDF_lnc_ltn {


    private IndexSearcher searcher;
    private QueryParser parser;

    // List of pages to query
    private List<String> pageList;

    // Number of documents to return
    private int numDocs;

    // Map of queries to map of Documents to scores for that query
    public Map<String, List<DocumentResult>> queryResults;


    public TFIDF_lnc_ltn(List<String> pl, int n) throws ParseException, IOException {

        numDocs = n; // Get the (max) number of documents to return
        pageList = pl; // Each page title will be used as a query

        // Parse the parabody field using StandardAnalyzer
        String fields[] = {"question","answer"};
        parser = new MultiFieldQueryParser(fields, new StandardAnalyzer());

        // Create an index searcher
        String INDEX_DIRECTORY = "/Users/xinliu/Documents/UNH/18Fall/cs853/index";
        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open((new File(INDEX_DIRECTORY).toPath()))));

        // Set our own similarity class which computes tf[t,d]
        SimilarityBase lnc_ltn = new SimilarityBase() {
            protected float score(BasicStats stats, float freq, float docLen) {
                return (float) (1 + Math.log10(freq));
            }

            @Override
            public String toString() {
                return null;
            }
        };
        searcher.setSimilarity(lnc_ltn);
    }

    public Map<String, List<DocumentResult>> getResult() throws IOException {

        queryResults = new HashMap<>(); // Maps query to map of Documents with
        // TF-IDF score
        Map<String,String> QAmap = new HashMap<>();
        for (String page : pageList) { // For every page in .cbor.outline
            // We need...

            Map<Integer, Float> scores = new HashMap<>(); // Mapping of each
            // Document to
            // its score
            Map<Integer,Pair> map = new HashMap<>();
            HashMap<Integer, DocumentResult> docMap = new HashMap<>();
            HashMap<TermQuery, Float> queryweights = new HashMap<>(); // Mapping
            // of
            // each
            // term
            // to
            // its
            // query
            // tf
            ArrayList<TermQuery> terms = new ArrayList<>(); // List of every
            // term in the query
            PriorityQueue<DocumentResult> docQueue = new PriorityQueue<>(new ResultComparator());
            ArrayList<DocumentResult> docResults = new ArrayList<>();

            for (String term : page.split(" ")) { // For every word in page
                // name...
                // Take word as query term
                // for parabody
                TermQuery postq = new TermQuery(new Term("answer", term));
                TermQuery titleq = new TermQuery(new Term("question", term));
                terms.add(postq);
                terms.add(titleq);

                // Add one to our term weighting every time it appears in the
                // query
                queryweights.put(postq, queryweights.getOrDefault(postq, 0.0f) + 1.0f);
                queryweights.put(titleq, queryweights.getOrDefault(titleq, 0.0f) + 1.0f);
            }
            for (TermQuery query : terms) { // For every Term

                // Get our Index Reader for helpful statistics
                IndexReader reader = searcher.getIndexReader();

                // If document frequency is zero, set DF to 1; else, set DF to
                // document frequency
                float DF = (reader.docFreq(query.getTerm()) == 0) ? 1 : reader.docFreq(query.getTerm());

                // Calculate TF-IDF for the query vector
                float qTF = (float) (1 + Math.log10(queryweights.get(query))); // Logarithmic
                // term
                // frequency
                float qIDF = (float) (Math.log10(reader.numDocs() / DF)); // Logarithmic
                // inverse
                // document
                // frequency
                float qWeight = qTF * qIDF; // Final calculation

                // Store query weight for later calculations
                queryweights.put(query, qWeight);

                // Get the top 100 documents that match our query
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
//                    String question = doc.get("question");
//                    System.out.println(question);
//                    System.out.println(doc.get("answer"));


                    DocumentResult dResults = docMap.get(docId);
                    if (dResults == null) {
                        dResults = new DocumentResult(docId, (float) score);
                    }
                    float prevScore = dResults.getScore();
                    // Store score for later use
                    scores.put(Integer.parseInt(doc.get("id")), (float) (prevScore + score));
                    map.put(Integer.parseInt(doc.get("id")),new Pair(doc.get("question"),doc.get("answer"),(float) (prevScore + score)));

                }
            }

            // Get cosine Length
            float cosineLength = 0.0f;
//            for (Map.Entry<Integer, Float> entry : scores.entrySet()) {
//                Float score = entry.getValue();
//
//                cosineLength = (float) (cosineLength + Math.pow(score, 2));
//            }

            for (Map.Entry<Integer,Pair> entry : map.entrySet()){
                float score = entry.getValue().getScore();
                cosineLength = (float) (cosineLength + Math.pow(score, 2));
            }

            cosineLength = (float) (Math.sqrt(cosineLength));

            // Normalization of scores
//            for (Map.Entry<Integer, Float> entry : scores.entrySet()) { // For
//                // every
//                // document
//                // and
//                // its
//                // corresponding
//                // score...
//                int docId = entry.getKey();
//                Float score = entry.getValue();
//
//                // Normalize the score
//                scores.put(docId, score / scores.size());
//
//                DocumentResult docResult = new DocumentResult(docId, score);
//                docQueue.add(docResult);
//            }



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
    public List<DocumentResult> getResultsForQuery(String query) {
        return this.queryResults.get(query);
    }

    public void write() throws IOException {
        System.out.println("TFIDF_lnc_ltn writing results to: " + "/Users/xinliu/Desktop/IR_project2/Project" + "/"
                +  "lnc-ltn.run");
        FileWriter runfileWriter = new FileWriter(
                new File("/Users/xinliu/Desktop/IR_project2/Project"+ "/" + "lnc-ltn.run"));
        for (Map.Entry<String, List<DocumentResult>> results : queryResults.entrySet()) {
            String query = results.getKey();
            List<DocumentResult> list = results.getValue();
            for (int i = 0; i < list.size(); i++) {
                DocumentResult dr = list.get(i);
                runfileWriter.write(query.replace(" ", "-") + " Q0 " + dr.getId() + " " + dr.getRank() + " "
                        + dr.getScore() +"  "+  dr.getQuestion() + "  "+ dr.getAnswer()+  " group7-TFIDF_lnc_ltn\n");
            }
        }
        runfileWriter.close();

    }
}
