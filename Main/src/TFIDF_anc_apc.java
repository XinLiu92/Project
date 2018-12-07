import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
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
public class TFIDF_anc_apc {


    //list of pages to query
    private List<String> pageList;
    private QueryParser parser;
    private IndexSearcher searcher;
    //num of doc to return
    private int numDocs;
    private Map<String,List<DocumentResult>> queryResult;

    public TFIDF_anc_apc(List<String> pl, int n) throws IOException{
        numDocs = n;
        pageList = pl;

        String fields[] = {"question","answer"};

        parser = new MultiFieldQueryParser(fields,new StandardAnalyzer());


        //create index searcher

        String INDEX_DIRECTORY =  "./index";

        searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(new File(INDEX_DIRECTORY).toPath())));

        SimilarityBase anc_apc = new SimilarityBase() {
            protected float score(BasicStats stats, float freq, float docLen) {
                float num = 0.5f * freq;
                return num;
                // return (float)(1 + Math.log10(freq));
            }

            @Override
            public String toString() {
                return null;
            }
        };

        searcher.setSimilarity(anc_apc);

    }


    public Map<String,List<DocumentResult>> getResults() throws IOException{

    //map query to map of document with tfidf score
        queryResult = new HashMap<>();
        Map<String,String> QAmap = new HashMap<>();
        //for every page

        for (String page : pageList){
            Map<Integer, Float> scores = new HashMap<>();
            Map<Integer,Pair> map = new HashMap<>();
            Map<Integer,Map<Float,String>> temp = new HashMap<>();

            HashMap<Integer, DocumentResult> docMap = new HashMap<>();

            HashMap<TermQuery, Float> queryweights = new HashMap<>(); // Mapping of each term to its query tf

            ArrayList<TermQuery> terms = new ArrayList<>(); // List of every term in the query

            PriorityQueue<DocumentResult> docQueue = new PriorityQueue<>(new ResultComparator());

            List<DocumentResult> docResults = new ArrayList<>();

            float maxQueryTF = 0;

            for (String term : page.split(" ")){
                TermQuery postq = new TermQuery(new Term("answer", term));
                TermQuery titleq = new TermQuery(new Term("question", term));
                terms.add(postq);
                terms.add(titleq);

                // Add one to our term weighting every time it appears in the
                // query


                queryweights.put(postq, queryweights.getOrDefault(postq, 0.0f) + 1.0f);
                queryweights.put(titleq, queryweights.getOrDefault(titleq, 0.0f) + 1.0f);
                if (queryweights.get(postq) > maxQueryTF) {
                    maxQueryTF = queryweights.get(postq);
                }
                if (queryweights.get(titleq) > maxQueryTF) {
                    maxQueryTF = queryweights.get(titleq);
                }
            }

            for (TermQuery query : terms){
                IndexReader reader = searcher.getIndexReader();
                // If document frequency is zero, set DF to 1; else, set DF to
                // document frequency

                float DF = (reader.docFreq(query.getTerm()) == 0) ? 0 : reader.docFreq(query.getTerm());

                // Calculate TF-IDF for the query vector
                // float qTF_num = (float)(1 +
                // Math.log10(queryweights.get(query))); // Logarithmic term
                // frequency

                float qTF_num = (float) (0.5 * queryweights.get(query));
                float qTF = 0.5f + (qTF_num / maxQueryTF);

                float qProb = (float) (Math.log10((reader.numDocs() - DF) / DF)); // Prob
                // idf
                // document
                // frequency

                float qWeight = qTF * qProb; // Final calculation

                // Store query weight for later calculations
                queryweights.put(query, qWeight);

                // Get the top 100 documents that match our query
                TopDocs tpd = searcher.search(query, numDocs);
                // get max tf for each document

                for (int i = 0; i < tpd.scoreDocs.length; i++) { // For every
                    // returned
                    // document...
                    Document doc = searcher.doc(tpd.scoreDocs[i].doc); // Get
                    // the
                    // document
                    int docId = Integer.parseInt(doc.get("id"));
//                    String question = doc.get("question");
//                    System.out.println(question);
//                    System.out.println(doc.get("answer"));
                    float max = Float.parseFloat(doc.get("maxtf"));

                    double score = (0.5f + (tpd.scoreDocs[i].score / max)) * queryweights.get(query); // Calculate
                    // TF-IDF
                    // for
                    // document

                    DocumentResult dResults = docMap.get(docId);
                    if (dResults == null) {
                        dResults = new DocumentResult(docId, (float) score,doc.get("question"),doc.get("answer"));
                    }
                    float prevScore = dResults.getScore();
                    // Store score for later use
                    scores.put(Integer.parseInt(doc.get("id")), (float) (prevScore + score));



                    QAmap.put(doc.get("question"),doc.get("answer"));

                    map.put(Integer.parseInt(doc.get("id")),new Pair(doc.get("question"),doc.get("answer"),(float) (prevScore + score)));
                }
            }

            float cosineLength = 0.0f;
//            for (Map.Entry<Integer, Float> entry : scores.entrySet()) {
//                Float score = entry.getValue();
//                cosineLength = (float) (cosineLength + Math.pow(score, 2));
//            }

            for (Map.Entry<Integer,Pair> entry : map.entrySet()){
                float score = entry.getValue().getScore();
                cosineLength = (float) (cosineLength + Math.pow(score, 2));
            }

            cosineLength = (float) (Math.sqrt(cosineLength));
            // Normalization of scores

//            Iterator<Map.Entry<Integer, Float>> iter_scores = scores.entrySet().iterator();
//            Iterator<Map.Entry<String, String>> iter_qa = QAmap.entrySet().iterator();

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
//            while(iter_scores.hasNext() && iter_qa.hasNext()){
//                Map.Entry<Integer, Float> e1 = iter_scores.next();
//                Map.Entry<String, String> e2 = iter_qa.next();
//
//                int docId = e1.getKey();
//                Float score = e1.getValue();
//
//                String question = e2.getKey();
//                String answer = e2.getValue();
//                scores.put(docId, score / scores.size());
//                DocumentResult docResult = new DocumentResult(docId, score,question,answer);
//                docQueue.add(docResult);
//            }

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
            queryResult.put(page, docResults);
        }


        return queryResult;

    }


    public void write() throws IOException {
        System.out.println("TFIDF_anc_apc writing results to: " + "/"
                +  "anc-apc.run");
        FileWriter runfileWriter = new FileWriter(
                new File("./" + "anc-apc.run"));
        for (Map.Entry<String, List<DocumentResult>> results : queryResult.entrySet()) {
            String query = results.getKey();
            List<DocumentResult> list = results.getValue();
            for (int i = 0; i < list.size(); i++) {
                DocumentResult dr = list.get(i);
                runfileWriter.write(query.replace(" ", "-") + " Q0 "+ dr.getId() + " " + dr.getRank() + " "
                        + dr.getScore() +  " group7-TFIDF_anc_apc\n");
            }
        }
        runfileWriter.close();

    }

}
