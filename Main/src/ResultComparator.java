import java.util.Comparator;

class ResultComparator implements Comparator<DocumentResult> {
    public int compare(DocumentResult d2, DocumentResult d1) {
        if (d1.getScore() < d2.getScore())
            return -1;
        if (d1.getScore() == d2.getScore())
            return 0;
        return 1;
    }
}