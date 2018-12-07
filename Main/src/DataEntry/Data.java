package DataEntry;


import java.io.*;
import java.util.*;
import PairPac.*;

public class Data {

    private static List<Ans> ansList = new ArrayList<>();
    private static List<Que> queList = new ArrayList<>();
    private static List<DocTitle> docTitleList = new ArrayList<>();
    private static int ansID = 1;
    public List<Ans> getAnsList(){
        return ansList;
    }

    public List<Que> getQueList(){
        return queList;
    }

    public List<DocTitle> getDocTitleList(){
        return docTitleList;
    }

    public  static Map<String,List<Pair>> titleWithQA = new HashMap<>();

    public static int count = 0;

    public static void tsv_reader(String dir) throws IOException {
        StringTokenizer st ;
        BufferedReader TSVFile = new BufferedReader(new FileReader(dir));
        String dataRow = TSVFile.readLine(); // Read first line.
        int n = 0;

        while (dataRow != null){

            st = new StringTokenizer(dataRow,"\t");
            List<String> dataArray = new ArrayList<String>() ;
            while(st.hasMoreElements()){
                dataArray.add(st.nextElement().toString());
            }
//            for (String item:dataArray) {
//                System.out.print(item + " --> ");
//            }
            count++;

            if (n != 0 ){
                Ans ans = new Ans(dataArray.get(5),ansID++,Integer.parseInt(dataArray.get(6)));
                ansList.add(ans);

                //Que que = new Que(dataArray.get(1),Integer.parseInt(dataArray.get(0).substring(1)));
                Que que = new Que(dataArray.get(1),n-1);
                queList.add(que);

                DocTitle docTitle = new DocTitle(dataArray.get(3),Integer.parseInt(dataArray.get(2).substring(1)));
                docTitleList.add(docTitle);
//                int id = Integer.parseInt(dataArray.get(0).substring(1));
                Pair p = new Pair(que.getQuestion(),ans.getAnsBody(),n-1);

                if (!titleWithQA.containsKey(docTitle.getDocument_title())) {
                    List<Pair> list = new ArrayList<>();
                    list.add(p);
                    titleWithQA.put(docTitle.getDocument_title(),list);
                }else{
                    titleWithQA.get(docTitle.getDocument_title()).add(p);
                }
//                for (String s : dataArray){
//                    System.out.println(s);
//                }



            }

            //System.out.println(); // Print the data line.
            dataRow = TSVFile.readLine(); // Read next line of data.
            n++;
           // if (n == 3) break;

        }
//        System.out.println("total "+count+"row");
//        System.out.println("id " + ansID);
        // Close the file once all data has been read.
        TSVFile.close();
        // End the printout with a blank line.
        System.out.println();
    }

    public static Map<String,List<Pair>> getTitleWithQA(){
        return titleWithQA;
    }
}
