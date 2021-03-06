import DataEntry.Ans;
import DataEntry.Data;
import DataEntry.DocTitle;
import DataEntry.Que;
import org.apache.lucene.document.*;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;
import PairPac.*;
public class main {
    public static String GROUP = "Group 7";
    public static String INDEX_DIR = "./index";

    public static String file= "";

    public static List<Ans> ansList = new ArrayList<>();
    public static List<Que> queList = new ArrayList<>();
    public static List<DocTitle> docTitleList = new ArrayList<>();

    public static Map<String,List<PairPac.Pair>> titleWithQA;

    private static Data index(String dir) throws IOException {
        Data data = new Data();

        Indexer indexer = new Indexer(INDEX_DIR);

        //read answer
        data.tsv_reader(file);
        titleWithQA = data.getTitleWithQA();
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

        file = args[0];

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
        tfidf_anc_apc.write();

        TFIDF_lnc_ltn tfidf_lnc_ltn = new TFIDF_lnc_ltn(queryList, 30);
        result_lnc_ltn = tfidf_lnc_ltn.getResult();
        tfidf_lnc_ltn.write();
//
        TFIDF_bnn_bnn tfidf_bnn_bnn = new TFIDF_bnn_bnn(queryList, 30);
        result_bnn_bnn = tfidf_bnn_bnn.getResults();
        tfidf_bnn_bnn.write();

        LanguageModel_BL lmbl = new LanguageModel_BL(queryList,30);
        lmbl.getReulst();
        lmbl.generateResults();

       writeQrel(queryList,titleWithQA);



    }

    public static void writeQrel(List<String> queryList,Map<String,List<PairPac.Pair>> titleWithQA){
        List<String> qrelsOutput = new ArrayList<>();

        // For every query which should be a tag
        for (String query : queryList) {
            // Make sure our tag is the same as represented in memory
            String fixedQuery = query.replace(" ", "-");
            // Get all postIds which are tagged with the given tag
            List<PairPac.Pair> relevantPosts = titleWithQA.get(fixedQuery);

            // If we have no posts that are relevant, continue to the next query
            if (relevantPosts == null) {
                continue;
            }
            // For every postId that is relevant
            for (PairPac.Pair p : relevantPosts) {
                // create a qrel-line indicating relevance of 1
                String qrelStr = fixedQuery + " 0 " + Integer.toString(p.getId()) + " 1";
                // add to final output
                qrelsOutput.add(qrelStr);
            }

            writeToFile(qrelsOutput);
        }
    }

    public static void writeToFile(List<String> runfileStrings) {
        String fullpath ="./"+"tags.qrels";
        try (FileWriter runfile = new FileWriter(new File(fullpath))) {
            for (String line : runfileStrings) {
                runfile.write(line + "\n");
            }

            runfile.close();
        } catch (IOException e) {
            System.out.println("Could not open " + fullpath);
        }
    }


}

