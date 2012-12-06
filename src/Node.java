/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author BLACKJACK
 */
public class Node {
  public String Attr_name;
  public double pivot;
  public double information_gain;
  public int rows_number;
  public int error_number;
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
    information_gain = -1;
    error_number = -1;
    rows_number = 0;
  }
}