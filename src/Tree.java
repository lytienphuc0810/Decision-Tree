/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author BLACKJACK
 */
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
  
  public Node getRoot() {
	  return root;
  }
}
