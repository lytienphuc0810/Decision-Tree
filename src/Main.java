
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Main {

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
      String learning_file;      
      String class_number;
      String decision = null;
      String test_file;
      
      System.out.println("Welcome to decision tree learning program");
      System.out.println("Input the learning dataset:");
      learning_file = bufferRead.readLine();
      System.out.println("How many classes do you want?");
      class_number = bufferRead.readLine();
      
      while(!"Y".equals(decision) && !"N".equals(decision)) {
        System.out.println("Do you want to test the decision tree? (Y/N)");
        decision = bufferRead.readLine();
      }
      if("Y".equals(decision)) {
        System.out.println("Input the test dataset:");
        test_file = bufferRead.readLine();
      }
      
      Process p = new Process();
      Tree MyTree = p.Build_Decision_Tree(learning_file, Integer.parseInt(class_number));
      TreeDemo demo = new TreeDemo(MyTree);
      
      if("Y".equals(decision)) {
//        p.Apply_Test_Dataset(MyTree, test_file);
      }
    } catch (IOException ex) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
    }
  }
}
