
public class Main {

  /**
   * @param args
   */
  public static void main(String[] args) {
    Process p = new Process();
    Tree MyTree = p.Build_Decision_Tree("VNINDEX_TRAIN_2.txt");
    TreeDemo demo = new TreeDemo(MyTree);
  }
}
