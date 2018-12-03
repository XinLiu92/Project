import DataEntry.Ans;
import DataEntry.Data;
import DataEntry.DocTitle;
import DataEntry.Que;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.print.Doc;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

public class main {
    public static String GROUP = "Group 7";
    public static String INDEX_DIR = "/Users/xinliu/Documents/UNH/18Fall/cs853/index";

    public static String OUTPUT_DIR = "";

    public static String WIKIQA_DIR = "";


    public static String TFIDF_BNN_BNN = "";
    public static String TFIDF_ANC_APC = "";
    public static String TFIDF_LNC_LTN = "";
    public static String LM_BL = "";
    public static String file=  "/Users/xinliu/Documents/UNH/18Fall/cs853/project/WikiQACorpus/WikiQA.tsv";
    //public static String file=  "/Users/xinliu/Documents/UNH/18Fall/cs819/assignment4/People.csv";
    public static List<Ans> ansList = new ArrayList<>();
    public static List<Que> queList = new ArrayList<>();
    public static List<DocTitle> docTitleList = new ArrayList<>();



    private static Data index(String dir) throws IOException {
        Data data = new Data();

        Indexer indexer = new Indexer("/Users/xinliu/Documents/UNH/18Fall/cs853/index");

        //read answer
        data.tsv_reader(file);

        ansList = data.getAnsList();
        queList = data.getQueList();
        docTitleList = data.getDocTitleList();

        return  data;

    }


    public static Map<String,List<Rank>> getRankList(Set<String> querySet) throws IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException {
        Indexer indexer = new Indexer(INDEX_DIR);
        Map<String,List<Rank>> rankMap = new HashMap<>();
         indexer.rebuildIndexes(queList,ansList);
        List<String> tmp = new ArrayList<>();
        for (String qury: querySet){
//            System.out.println(qury);

            if (!qury.contains("!") && !qury.contains("/")){
                tmp.add(qury);
            }

        }


        for (String query : tmp){
            //System.out.println(query);
            SearchEngine se = new SearchEngine(true,INDEX_DIR);
            TopDocs topDocs = se.performSearch(query, 100);
            ScoreDoc[] hits = topDocs.scoreDocs;
            List<Rank> rankList = new ArrayList<>();
            //System.out.println("================== hits size: "+ hits.length);
            for (int i = 0; i < hits.length;i++){
                Document document = se.getDocument(hits[i].doc);

                Rank rank = new Rank();
                rank.setQuery(query);
                rank.setParagId(document.get("id"));
                rank.setRank(i+1);
                rank.setScore(hits[i].score);
                rankList.add(rank);
            }
            //System.out.println("search done!    "+ "get rank list with size: " +rankList.size());
            rankMap.put(query,rankList);

        }

        return rankMap;
    }


    public static void main(String[] args) throws ParseException, org.apache.lucene.queryparser.classic.ParseException, IOException {

        Data data = null;
        try {
            data = index("");
        } catch (IOException e) {
            e.printStackTrace();
        }


       //use doc title to be testing query
        if (data == null){
            System.out.println("Data is null");
            return;
        }

        List<DocTitle> docTitleList = data.getDocTitleList();
        Set<String> querySet = new HashSet<>();

        for (DocTitle doc : docTitleList){
//            System.out.println(doc.getDocument_title());
            querySet.add(doc.getDocument_title());
        }


        Map<String,List<Rank>> rankMap = getRankList(querySet);
//        int n = 0;
//        for (String query : rankMap.keySet()){
//            System.out.println("query =====>>>>  " + query);
//
//            List<Rank> rankList = rankMap.get(query);
//
//            for (Rank r : rankList){
//                System.out.println(r.getParagId());
//            }
//
//            if (n > 10 ){
//                break;
//            }
//
//            n++;
//        }


        List<String> queryList = new ArrayList<>(querySet);

        Map<String, List<DocumentResult>> result_anc_apc = new HashMap<>();

        Map<String, List<DocumentResult>> result_bnn_bnn = new HashMap<>();

        Map<String, List<DocumentResult>> result_lnc_ltn = new HashMap<>();


        TFIDF_anc_apc tfidf_anc_apc = new TFIDF_anc_apc(queryList, 30);
        result_anc_apc = tfidf_anc_apc.getResults();

        int count = 0;

        for (String query : result_anc_apc.keySet()){
//            System.out.println(query);
//            System.out.println(result_anc_apc.get(query).size());
//            for (DocumentResult Doc : result_anc_apc.get(query)){
//                System.out.println(Doc.getId());
//            }

            if (count >10) break;
            count++;
        }
        tfidf_anc_apc.write();

        TFIDF_lnc_ltn tfidf_lnc_ltn = new TFIDF_lnc_ltn(queryList, 30);
        result_lnc_ltn = tfidf_lnc_ltn.getResult();

        TFIDF_bnn_bnn tfidf_bnn_bnn = new TFIDF_bnn_bnn(queryList, 30);
        result_bnn_bnn = tfidf_bnn_bnn.getResults();


        for (String page : queryList){
            System.out.println("Page=" + page);
            List<DocumentResult> bnn_list = result_bnn_bnn.get(page);
            List<DocumentResult> lnc_list = result_lnc_ltn.get(page);
            List<DocumentResult> anc_list = result_anc_apc.get(page);
        }
    }

}

