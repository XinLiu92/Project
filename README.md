# Installation and compile

Group 7: 

Kunpeng Xie, unhid: 949610377, webcat username: kx1005

Xin Lium unhid: 910991617, webcat username:xl1044;



**Required Tool:**

1. Maven3.5.4: To automatic loading up required dependencies, download from:<https://maven.apache.org/download.cgi>
2. Git: Version control, download from <https://git-scm.com/downloads>
3. Intellij IDEA: download from <https://www.jetbrains.com/idea/download/#section=mac>

**Tool Installation:**

1. Maven installation guide <https://maven.apache.org/install.html>
2. Git installation:<https://git-scm.com/book/en/v2/Getting-Started-Installing-Git>
3. Intellij installation: <https://www.jetbrains.com/help/idea/install-and-set-up-product.html>

Data Download:

Download the dataset from:https://www.microsoft.com/en-us/download/details.aspx?id=52419

unzip the WikiQAcorpus.zip and suns WikiQA.tsv in folder WikiQAcorpus.

Use the WikiQA.tsv under: 

Reporsitory

1. Clone the project by https://github.com/XinLiu92/Project.git to your local.

2. Open the cloned repository in Intellij, and reimport maven dependencies. Windows type in ctrl+shift+a to find action, type in "reimport", you will find "reimport all maven projects", then select it and press enter. Mac will type in cmd+shift +a instead. All of the necessary dependencies are included in pom.xml

3. Make sure you pass two arguments to the program, the first one is index directory, second one is the data file directory

4. Rebuild the project and run main.java

5. By changing the boolean variable defualtScore under Main.java to false, you can swich the score function to the one we need to change in assignment spec.

6. Then pass WikiQA.tsv as argument run the program to get the result.

**Run in command line:**

1. clone **project** from github(https://github.com/XinLiu92/Project.git) and cd in to the directory which contains pom.xml and src folder.

2. run

   ```
   mvn clean install
   ```

   and

   ```
   mvn compile 
   ```

3. run

   ```
   mvn exec:java -Dexec.mainClass="main" -Dexec.args="arg0"  
   ```

   The output files will be created under the repository folder.

   arg0:Pass WikiQA.tsv's full directory to be argument 
