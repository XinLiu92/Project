public class DocumentResult {
    private int docId;
    private float score;
    private int rank;
    private String question;
    private String answer;

    DocumentResult(int id, float s,String question,String answer) {
        this.docId = id;
        this.score = s;
        this.rank = 0;
        this.question = question;
        this.answer = answer;
    }

    DocumentResult(int id, float s) {
        this.docId = id;
        this.score = s;
        this.rank = 0;
    }

    public String getQuestion(){
        return this.question;
    }

    public String getAnswer(){
        return this.answer;
    }
    public int getId() {
        return this.docId;
    }

    public float getScore() {
        return this.score;
    }

    public int getRank() {
        return this.rank;
    }

    public void setId(int id) {
        this.docId = id;
    }

    public void setScore(float s) {
        this.score = s;
    }

    public void setRank(int r) {
        this.rank = r;
    }
}
