public class Pair {
    private String question;
    private String answer;
    private float score;
    public Pair(String question, String answer,float score ){
        this.question = question;
        this.answer = answer;
        this.score = score;
    }

    public void setQuestion(String question){
        this.question = question;
    }

    public void setAnswer(String answer){
        this.answer = answer;
    }

    public void setScore(float score){
        this.score = score;
    }

    public String getQuestion(){
        return this.question;
    }

    public String getAnswer(){
        return this.answer;
    }

    public float getScore(){
        return this.score;
    }
}
