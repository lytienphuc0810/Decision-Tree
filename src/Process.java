import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
 
public class Process {
  
  public double target_classes[];
  
  public int number_of_target_classes = 2;
  
  public double Entropy_Numeric(MyDB MyDatabase) {
    double result = 0.0;
    double percentage;
    for(int i = 1; i <= number_of_target_classes; i++) {
      percentage = MyDatabase.percentage_of_target_attr(i);
      if(Math.abs(percentage - 0) > 0.001) {
        result += percentage * Math.log(percentage)/Math.log(2.0);
      }
    }
    return (0 - result);
  }
    
  public double Information_Gain(String Attr_name, double pivot, MyDB MyDatabase) {
    double sum_entropy = 0;
    double percent, entropy;
    for(int i = 1; i <= 2; i++) {
      MyDB subMyDatabase = MyDatabase.split(Attr_name, pivot, i);
      percent = subMyDatabase.percentage();
//      System.out.println("^^^ Percentage " + percent);
      entropy = Entropy_Numeric(subMyDatabase);
//      System.out.println("&&& Entropy " + entropy);
      sum_entropy += percent * entropy;
    }

    double current_entropy = Entropy_Numeric(MyDatabase);

    return current_entropy - sum_entropy;
  }

  public void Recursive_Build_Decision_Tree(MyDB MyDatabase, Tree MyTree) {
    double max_ig = 0;
    String max_ig_attr = "";
    double pivot = 0;
    double temp_pivot;
    double ig;
    
    System.out.println("+++ At node: " + MyDatabase.where + 
                       "\n--- Node on Path: " + MyTree.height + " / " + MyDatabase.attr_count() + " has " + MyDatabase.row_count + " rows ");
    
    if( MyTree.height == MyDatabase.attr_count() || MyDatabase.row_count == 0) {
      //TODO Prunning here
      int i = MyDatabase.largest_percentage_class();
      if (i != -1) {
        System.out.println("Class: " + i);
        MyTree.current.class_number = i;
      }
      else {
        System.out.println("There is no record in this node");
        MyTree.current.class_number = -1;
      }
    }
    else {
      while(true){
        String Attr_name = MyDatabase.get_attr();
        if( Attr_name != null ) {
          if( MyTree.not_in_branch(Attr_name) ){
            temp_pivot = MyDatabase.best_pivot(Attr_name);
            ig = Information_Gain(Attr_name, temp_pivot, MyDatabase);
            System.out.println("$$$ " + Attr_name + " has IG: " + ig + " splited at: " + temp_pivot);
            if( "".equals(max_ig_attr)) {
              max_ig = ig;
              max_ig_attr = Attr_name;
              pivot = temp_pivot;              
            }
            else{
              if( ig > max_ig ) {
                max_ig = ig;
                max_ig_attr = Attr_name;
                pivot = temp_pivot;
              }
            }
          }
        }
        else {
          break;
        }
      }
      
      MyTree.current.Attr_name = max_ig_attr;
      MyTree.current.pivot = pivot;
      MyTree.increase_tree_height();
      MyTree.add_to_tree( new Node(), false);
      Recursive_Build_Decision_Tree( MyDatabase.split(max_ig_attr, pivot, 1), MyTree );
      MyTree.backtrack();
      
      MyTree.add_to_tree( new Node(), true);
      Recursive_Build_Decision_Tree( MyDatabase.split(max_ig_attr, pivot, 2), MyTree );
      MyTree.backtrack();
      MyTree.decrease_tree_height();
    }
  }

  public void Build_Decision_Tree(String filename) {
    //Recursive here
    Tree MyTree = new Tree();
    Database database = new Database(filename);
    MyDB MyDatabase = new MyDB(database);
    
    MyDatabase.initialize_target_classes();
    
    for(int i = 1; i <= number_of_target_classes; i++) {
      System.out.println("Class " + i + " from " + target_classes[i-1] + " to " + target_classes[i]);
    }
    
    Recursive_Build_Decision_Tree(MyDatabase, MyTree);
    int a =3;
  }

  public class Tree {
    public Node root;
    public Node current;
    public int height;
    
    public Tree() {
      root = new Node();
      current = root;
      height = 0;
    }
    
    public void add_to_tree(Node node, boolean right) {
      if( root == null ) {
        root = node;
        current = node;
      }
      else {
        if(right) {
          current.right = node;
          current.right.parent = current;
          current = current.right;
        }
        else {
          current.left = node;
          current.left.parent = current;
          current = current.left;
        }
      }
    }
    public void increase_tree_height() {
      height++;
    }
    
    public void decrease_tree_height() {
      height--;
    }
    
    public void backtrack() {
      current = current.parent;
    }
    
    public boolean not_in_branch(String Attr_name) {
      Node temp = current;
      while(temp != null) {
        if( temp.Attr_name.equals(Attr_name) ) {
          return false;
        }
        temp = temp.parent;
      }
      return true;
    }
  }

  public class Node {
    public String Attr_name;
    public double pivot;
    public Node right;    
    public Node left;
    public Node parent;
    
    // 0 la khong phai node la, -1 la ko thuoc class nao
    public int class_number;

    public Node() {
      Attr_name = "";
      right = null;
      left = null;
      parent = null;
      class_number = 0; 
      pivot = -1;
    }
  }
  
  public class MyDB {
    private Database data;
    private String table_name;
    private String[] column_name;
    private int round_attr;
    private String target_attr;
    private String select_all;
    public String where;
    public String parent_where;
    public int row_count;
    public int column_count;
    
    // constructors
    public MyDB(Database in_data) {
      parent_where = " WHERE ";
      where = " WHERE ";
      round_attr = 0;
      data = in_data;
      table_name = data.getTableName();
      select_all = "SELECT * FROM " + table_name;
      ResultSet temp = null;
     
      try {
        
        temp = data.query(count_str(where));
        temp.first();
        row_count = temp.getInt(1);
        
        temp = data.query( select_all );
        ResultSetMetaData meta_temp = temp.getMetaData();
        column_count = meta_temp.getColumnCount() - 1;
        column_name = new String[column_count];
        for( int i = 0;  i < column_count; i++ ) {
          column_name[i] = meta_temp.getColumnName(i+1);
        }
        target_attr = meta_temp.getColumnName(column_count + 1);
       
        data.close_conection();
        temp.close();
        
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    public MyDB(String Parent_part, String Where_part, Database in_data) {
      parent_where = Parent_part;
      where = Where_part;
      round_attr = 0;
      data = in_data;
      table_name = data.getTableName();
      select_all = "SELECT * FROM " + table_name;
      ResultSet temp = null;
      
      try {
        
        temp = data.query(count_str(where));
        temp.first();
        row_count = temp.getInt(1);
        
        temp = data.query(select_all + where);
        ResultSetMetaData meta_temp = temp.getMetaData();
        column_count = meta_temp.getColumnCount() - 1;
        column_name = new String[column_count];
        for( int i = 0;  i < column_count; i++ ) {
          column_name[i] = meta_temp.getColumnName(i+1);
        }
        target_attr = meta_temp.getColumnName(column_count + 1);
        
        data.close_conection();
        temp.close();
        
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    
    // initialize target classes
    public void initialize_target_classes() {
      ResultSet temp;
      double max = 0;
      double min = 0;
      double delta;
      
      try {
        
        temp = data.query( max_str(target_attr) );  
        temp.first();
        max = temp.getDouble(1);
        temp = data.query( min_str(target_attr) );
        temp.first();
        min = temp.getDouble(1);
        
        temp.close();
        
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
      
      delta = (max - min)/number_of_target_classes;
      target_classes = new double[number_of_target_classes + 1];
      
      for(int i = 0; i <= number_of_target_classes; i++) {
        target_classes[i] = min + delta * i;
      }
    }
    
    // use to get the percentage of the current node base on the parent node
    public double percentage() {
      double result = 0;
      String str;
      str = count_str(parent_where);      
      ResultSet parent = data.query(str);
      str = count_str(where);
      ResultSet current = data.query(str);
      
      try {
      
        parent.first();
        current.first();
        result = current.getDouble(1)/parent.getDouble(1);
        
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
      
      return result;
    }
    
    // use to get the percentage of one specific class in current node
    public double percentage_of_target_attr(int i) {
      double result = 0;
      
      String sql_query = where;
      if(!" WHERE ".equals(where)) {
        sql_query = where + " AND ";
      }
      
      sql_query = sql_query + target_attr + " >= " + target_classes[i-1] + " AND " + target_attr + " <= " + target_classes[i];
      
      ResultSet current = data.query(count_str(where));
      ResultSet potion_of_target_attr = data.query(count_str(sql_query));
     
      try {
        
        current.first();   
        potion_of_target_attr.first();
        double temp = potion_of_target_attr.getDouble(1);
        temp = current.getDouble(1);
        result = potion_of_target_attr.getDouble(1)/current.getDouble(1);
        
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
      
      return result;
    }
    
    // get the best pivot value to split the database 
    public double best_pivot(String Attr_name) {
      ResultSet dataset = data.query(select_by_and_sort(Attr_name));
      data.close_conection();
      double k = 0;
      double max_gain = 0;
      double temp_pivot;
      double temp;
      
      try {
        for(int i = 1; i <= row_count; i++) {
          dataset.absolute(i);
          temp_pivot = dataset.getDouble(1);
          temp = Information_Gain(Attr_name, temp_pivot, this);

          if(max_gain == 0) {
            max_gain = temp;
            k = temp_pivot;
          }
          else {
            if(max_gain < temp) {
              max_gain = temp;
              k = temp_pivot;
            }
          }
        }
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
      
      return k;
    }
    
    // split database base on attribute and value
    public MyDB split(String Attr_name, double f, int part) {
      String sql_query = where;
      
      if(!" WHERE ".equals(where)) {
        sql_query = where + " AND ";
      }
     
      if( part == 1 ) {
        sql_query = sql_query + Attr_name  + " <= " + f;
      }
      else {
        sql_query = sql_query + Attr_name  + " > " + f;
      }
      
      return new MyDB(where, sql_query, data);
    } 

    // get a next attribute in table
    public String get_attr() {
      if(round_attr >= column_count) {
        round_attr = 0;
        return null;
      }
      else {
        String result = column_name[round_attr];
        round_attr++;
        return result;
      }
    }

    // return the class with most percentage
    public int largest_percentage_class() {
      double max_percent = 0;
      int k = 0;
      String result = "";
      
      if(" WHERE OPEN <= 118.31 AND HIGH <= 115.237 AND LOW > 112.932".equals(where)) {
        k = 0;
      }
      
      for(int i = 1; i <= number_of_target_classes; i++) {
        double percentage = percentage_of_target_attr(i);
        if( max_percent < percentage) {
          max_percent = percentage;
          k = i;
        }
      }
      
      return k == 0 ? -1 : k;
    }

    // SQL strings
    public String select_by_and_sort(String Attr_name) {
      String where_str;
      where_str = " WHERE ".equals(where) ? "" : where;
      return "SELECT " + Attr_name + " FROM " + table_name + where_str + " ORDER BY " + Attr_name;
    }
    
    public String max_str(String Attr_name) {
      return ("SELECT MAX(" + Attr_name + ") FROM " + table_name);
    }

    public String min_str(String Attr_name) {
      return ("SELECT MIN(" + Attr_name + ") FROM " + table_name);
    }
    
    private String count_str(String in_where) {
      String where_str;
      where_str = " WHERE ".equals(in_where) ? "" : in_where;
      return ("SELECT COUNT(*) AS COUNT FROM " + table_name + where_str);
    }

    public int attr_count() {
      return column_count;
    }
  }
}
