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
    return (-result);
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

  public void Recursive_Build_Decision_Tree(MyDB MyDatabase, Branch branch) {
    double max_ig = 0;
    String max_ig_attr = "";
    double pivot = 0;
    double ig;
    
    System.out.println("+++ At node: " + MyDatabase.where);
    System.out.println("--- Node on Path: " + branch.get_count() + " / " + MyDatabase.attr_count());
    
    if( branch.get_count() == MyDatabase.attr_count()) {
      //TODO Prunning here
    }
    else {
      while(true){
        String Attr_name = MyDatabase.get_attr();
        if( Attr_name != null ) {
          if( branch.not_in_branch(Attr_name) ){
            double temp_pivot = MyDatabase.best_pivot(Attr_name);
            ig = Information_Gain(Attr_name, temp_pivot, MyDatabase);
            System.out.println("$$$ " + Attr_name + " has IG: " + ig);
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

      branch.add( max_ig_attr );
      Recursive_Build_Decision_Tree( MyDatabase.split(max_ig_attr, pivot, 1), branch.clone() );     
      Recursive_Build_Decision_Tree( MyDatabase.split(max_ig_attr, pivot, 2), branch.clone() );
    }
  }

  public void Build_Decision_Tree(String filename) {
    //Recursive here
    Branch branch = new Branch();
    Database database = new Database(filename);
    MyDB MyDatabase = new MyDB(database);
    MyDatabase.initialize_target_classes();
    Recursive_Build_Decision_Tree(MyDatabase, branch);
  }

  public class Branch {
    // danh sach lien ket dong
    public Node head;
    public Node tail;
    public int count;
    public Branch() {
      head = null;
      tail = null;
      count = 0;
    }
    
    public Branch clone() {
      Branch clone_branch = new Branch();
      Node temp = head;
      while(temp != null) {
        clone_branch.add(temp.Attr_name);
        temp = temp.next;
      }
      return clone_branch;
    }
    
    public void add(String Attr_name) {
      if(head == null) {
        head = new Node(Attr_name);
        tail = head;
      }
      else {
        tail.next = new Node(Attr_name);
        tail = tail.next;
      }
      count++;
    }

    public boolean not_in_branch(String Attr_name) {
      Node temp = head;
      while( temp!=null ) {
        if( temp.Attr_name.equals(Attr_name) ){
          return false;
        }
        temp = temp.next;
      }
      return true;
    }

    public void add_final_result(String result) {
      tail.add_result( result );
    }
    
    public int get_count() {
      return count;
    }
  }

  public class Node {
    public String Attr_name;
    public String value;
    public Node next;
    public Node(String v) {
      Attr_name = v;
      value = "";
      next = null;
    }

    public void add_result(String result) {
      value = result;
    }
  }
  
  public class MyDB {
    private Database data;
    private String target_attr;
    private String table_name;
    private String select_all;
    private String[] column_name;
    private int round_attr;
    public String where;
    public String parent_where;
    public int row_count;
    public int column_count;

    public MyDB(Database in_data) {
      parent_where = " WHERE ";
      where = " WHERE ";
      round_attr = 0;
      data = in_data;
      table_name = data.getTableName();
      select_all = "SELECT * FROM " + table_name;
      
      try {
        ResultSet temp = data.query( count_str(where) );
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
      
      try {
        ResultSet temp = data.query(count_str(where));
        temp.first();
        row_count = temp.getInt(1);
        temp = data.query( select_all + where);
        ResultSetMetaData meta_temp = temp.getMetaData();
        column_count = meta_temp.getColumnCount() - 1;
        column_name = new String[column_count];
        for( int i = 0;  i < column_count; i++ ) {
          column_name[i] = meta_temp.getColumnName(i+1);
        }
        target_attr = meta_temp.getColumnName(column_count + 1);
        data.close_conection();
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    public void initialize_target_classes() {
      ResultSet temp;
      float max = 0;
      float min = 0;

      try {
        temp = data.query( max_str(target_attr) );  
        temp.first();
        max = temp.getFloat(1);
        temp = data.query( min_str(target_attr) );
        temp.first();
        min = temp.getFloat(1);
      
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
      
      double delta = (max - min)/5.0;
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

        result = potion_of_target_attr.getDouble(1)/current.getDouble(1);
        
      } catch (SQLException ex) {
        Logger.getLogger(Process.class.getName()).log(Level.SEVERE, null, ex);
      }
      
      return result;
    }
    
    // 
    public double best_pivot(String Attr_name) {
      ResultSet dataset = data.query(select_by_and_sort(Attr_name));
      data.close_conection();
      double k = 0;
      double max_gain = 0;
      
      try {
        for(int i = 1; i <= row_count; i++) {
            dataset.absolute(i);
            double temp_pivot = dataset.getDouble(1);
            double temp = Information_Gain(Attr_name, temp_pivot, this);
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
      
      for(int i = 1; i <= number_of_target_classes; i++) {
        double percentage = percentage_of_target_attr(i);
        if( max_percent < percentage) {
          max_percent = percentage;
          k = i;
        }
      }
      
      return k;
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
    
    public String count_str(String in_where) {
      String where_str;
      where_str = " WHERE ".equals(in_where) ? "" : in_where;
      return ("SELECT COUNT(*) AS COUNT FROM " + table_name + where_str);
    }

    public int attr_count() {
      return column_count;
    }
  }
}
