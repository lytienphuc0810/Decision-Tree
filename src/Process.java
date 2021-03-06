import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
 
public class Process {
  
  public double target_classes[];
  
  public int number_of_target_classes;
  
  public int error_row_count = 0;
  
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
    
    MyTree.current.rows_number = MyDatabase.row_count;
    largest_percentage largest_class = MyDatabase.largest_percentage_class();
    
    if( MyTree.height == MyDatabase.attr_count() || MyDatabase.row_count == 0 || largest_class.percentage >= 0.99) {
      //TODO Prunning here
      int i = largest_class.class_n;
      if (i != 0) {
        System.out.println("Class: " + i + " with percentage: "+ largest_class.percentage);
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
//            System.out.println("$$$ " + Attr_name + " has IG: " + ig + " splited at: " + temp_pivot);
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
      MyTree.current.information_gain = max_ig;
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
  
  public void Recursive_Apply_Test(MyDB MyDatabase, Tree MyTree) {
    if(MyTree.current.class_number == 0) {
      String Attr_name = MyTree.current.Attr_name;
      double pivot = MyTree.current.pivot;
      
      MyTree.current = MyTree.current.left;
      Recursive_Apply_Test(MyDatabase.split(Attr_name, pivot, 1), MyTree);
      MyTree.backtrack();
      
      MyTree.current = MyTree.current.right;
      Recursive_Apply_Test(MyDatabase.split(Attr_name, pivot, 2), MyTree);
      MyTree.backtrack();
    }
    else {
      if(MyTree.current.class_number > 0) {
        error_row_count += MyDatabase.error_rows(MyTree.current.class_number);
      }
    }
  }
  
  public void Apply_Test_Dataset(Tree MyTree, String filename) {
    Database database = new Database(filename);
    MyDB MyDatabase = new MyDB(database);
    
    System.out.println();   
    System.out.println();
    System.out.println();
    
    Recursive_Apply_Test(MyDatabase, MyTree);
    
    System.out.println("Number of Error Rows:" + error_row_count + ", percentage: " + ((double) error_row_count)/ ((double) MyDatabase.row_count));
  }

  public Tree Build_Decision_Tree(String filename, int n) {
    
    number_of_target_classes = n;
    
    Tree MyTree = new Tree();
    Database database = new Database(filename);
    MyDB MyDatabase = new MyDB(database);
    
    MyDatabase.initialize_target_classes();
    
    for(int i = 1; i <= number_of_target_classes; i++) {
      System.out.println("Class " + i + " from " + target_classes[i-1] + " to " + target_classes[i]);
    }
    
    Recursive_Build_Decision_Tree(MyDatabase, MyTree);
    
    return MyTree;
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
//      System.out.println(sql_query);
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
    public largest_percentage largest_percentage_class() {
      double max_percent = 0;
      int k = 0;
      String result = "";     
      
      for(int i = 1; i <= number_of_target_classes; i++) {
        double percentage = percentage_of_target_attr(i);
        if( max_percent < percentage) {
          max_percent = percentage;
          k = i;
        }
      }
      
      return new largest_percentage(k, max_percent);
    }

    public int error_rows(int class_number) {
      String sql_query = where;
      String sql_error = sql_query;
      int n = 0;
      
      if(!" WHERE ".equals(where)) {
        sql_query = where + " AND ";
      }
      
      sql_query = sql_query + "(" +  target_attr + " < " + target_classes[class_number-1] + " OR " + target_attr + " >= " + target_classes[class_number] + ")";
      sql_query = count_str(sql_query);

      ResultSet current = data.query(sql_query);
      
      try {
        
        current.first();
        n = current.getInt(1);
        
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
      if(n > 0) {
        System.out.println("There are " + n + " error rows at: " + sql_error);
      }
              
      return n;
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
  
  public class largest_percentage {
    public int class_n;
    public double percentage;

    public largest_percentage(int class_number, double percentage_in) {
      class_n = class_number;
      percentage = percentage_in;
    }
  }
}
