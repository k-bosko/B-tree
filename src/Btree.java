/*
 * CS7280 Special Topics in Database Management
 * Project 1: B-tree implementation.
 *
 * You need to code for the following functions in this program
 *   1. Lookup(int value) -> nodeLookup(int value, int node)
 *   2. Insert(int value) -> nodeInsert(int value, int node)
 *   3. Display(int node)
 *
 */

import java.util.Arrays;

final class Btree {

  /* Size of Node. */
  private static final int NODESIZE = 5;

  /* Node array, initialized with length = 1. i.e. root node */
  private Node[] nodes = new Node[1];

  /* Number of currently used nodes. */
  private int cntNodes = 0;

  /* Pointer to the root node. */
  private int root;

  /* Number of currently used values. */
  private int cntValues = 0;

  private final int nodeSize;

  private final int mid;
  /*
   * B tree Constructor.
   */
  public Btree() {
    nodeSize = NODESIZE;
    root = createNode();
    nodes[root].isLeaf = false;
    nodes[root].children[0] = createLeaf();
    mid = ((nodeSize + 1) % 2 == 0)? (nodeSize + 1)/2 - 1:  Math.floorDiv(nodeSize + 1, 2);
  }

  //overloaded constructor
  public Btree(int nodeSize) {
    this.nodeSize = nodeSize;
    root = createNode();
    nodes[root].isLeaf = false;
    nodes[root].children[0] = createLeaf();
    mid = ((nodeSize + 1) % 2 == 0)? (nodeSize + 1)/2 - 1:  Math.floorDiv(nodeSize + 1, 2);
  }

  /*********** B tree functions for Public ******************/

  /*
   * Lookup(int value)
   *   - True if the value was found.
   */
  public boolean lookup(int value) {
    return nodeLookup(value, root);
  }

  /*
   * Insert(int value)
   *    - If -1 is returned, the value is inserted and increase cntValues.
   *    - If -2 is returned, the value already exists.
   */
  public void insert(int value) {
    if(nodeInsert(value, nodes[root]) == -1) cntValues++;
  }


  /*
   * CntValues()
   *    - Returns the number of used values.
   */
  public int getCntValues() {
    return cntValues;
  }

  /*********** B-tree functions for Internal  ******************/

  /*
   * nodeLookup(int value, int pointer)
   *    - True if the value was found in the specified node.
   */
  private boolean nodeLookup(int value, int pointer) {
    Node curr = nodes[pointer];
    //if the current node is a leaf node:
    if (curr.isLeaf){
      //return if the value is found in the leaf node (true/false)
      return found(curr, value);
    }
    //else... if the node is no leaf node:
    else {
      //find the child node for the current node
      int childPtr = findChild(curr, value);
      return nodeLookup(value, childPtr);
    }
  }

  public void displayTree(){
    displayNode(root, 0);
  }

  /* Display per requirement */
  public void display(int nodePtr){
    displayNode(nodePtr, 0);
  }

  public void displayNode(int nodePtr, int level){
    Node node = nodes[nodePtr];
    System.out.print("  ".repeat(level));
    for (int i = 0; i < node.size; i++) {
      System.out.print(node.values[i] + " ");
    }
    System.out.println();
    if (!node.isLeaf){
      level++;
      int numChildren = node.size + 1;
      for (int i = 0; i < numChildren; i++){
        displayNode(node.children[i], level);
      }
    }
  }

  /*
   * nodeInsert(int value, int pointer)
   *    - -2 if the value already exists in the specified node
   *    - -1 if the value is inserted into the node or
   *            something else if the parent node has to be restructured
   */
  private int nodeInsert(int value, Node curr) {
    //if the current node is a leaf node
    if (curr.isLeaf) {
      if (found(curr, value)){
        return -2;
      }
      //if there is space -> insert value into leaf node in sorted order
      if (curr.size < nodeSize) {
        insertValue(curr, value);
        return -1;
      }
      //there is no space
      else {
        // create new leaf node & return pointer to it
        int newLeafPtr = createLeaf();
        //checkout the newly created node based on its pointer
        Node newLeaf = nodes[newLeafPtr];
        //redistribute values in the current and new node
        redistribute(curr, value, newLeaf);
        return newLeafPtr;
      }
    }
    //NOT LEAF
    else {
      //find the child node for the current node
      int childPtr = findChild(curr, value);
      Node child = nodes[childPtr];
      int newChildPtr = nodeInsert(value, child);
      //no split -> done
      if (newChildPtr == -2 || newChildPtr == -1){
        return newChildPtr;
      }
      // after split
      //if there is space inside current node
      else if (curr.size < nodeSize) {
        //promote mid value via left bias -> insert child's last value (i.e. mid) into current node //TODO add to readme
        curr.values[curr.size] = child.values[child.size]; //mid invisible at index equal to size
        curr.size++;
        //insert the new child pointer into the current node
        curr.children[curr.size] = newChildPtr;
        return -1;
      }
      //no space
      else {
        //create a new node
        int newNodePtr = createNode();
        Node newNode = nodes[newNodePtr];
        //distribute values
        int midVal = child.values[child.size];
        redistribute(curr, midVal, newNode);
        //update child pointers -> all children after mid go to newNode
        int i;
        //numChildrenToTransfer is different depending on nodeSize - even or odd //TODO add to readme
        int numChildrenToTransfer = (nodeSize % 2 == 0)? mid: mid + 1;
        for (i = 0; i < numChildrenToTransfer; i++){
          newNode.children[i] = curr.children[i + mid + 1];
        }
        newNode.children[i] = newChildPtr;

        if (curr != nodes[root]){
          return newNodePtr;
        }
        else {
          //create and set a new root node
          int newRootPtr = createNode();
          Node newRoot = nodes[newRootPtr];
          //update pointers to children
          newRoot.children[0] = root;
          newRoot.children[1] = newNodePtr;
          root = newRootPtr;
          //initialize the root node with the values of the current and new node
          newRoot.values[0] = curr.values[curr.size]; //mid is invisible at index size
          newRoot.size++;
        }
        return -1;
      }
    }
  }

  private boolean found(Node curr, int value){
    // check  if value exists
    for (int i = 0; i < nodeSize; i++) {
      if (curr.values[i] == value) {
        return true; // exit
      }
    }
    return false;
  }

  private void insertValue(Node curr, int value){
    int i = 0;
    for (i = curr.size - 1; i >= 0 && value < curr.values[i]; i--){
      curr.values[i + 1] = curr.values[i];
    }
    curr.values[i + 1] = value;
    curr.size += 1;
  }

  private int findChild(Node curr, int value){
    int i = 0;
    while (i < curr.size && curr.values[i] < value){
      i += 1;
    }
    return curr.children[i];
  }

  private void redistribute(Node curr, int value, Node newNode){

    int[] arr = new int[nodeSize + 1];
    for (int i = 0; i < nodeSize; i++){
      arr[i] = curr.values[i];
    }
    arr[nodeSize] = value;
    arr = Arrays.stream(arr).sorted().toArray();
    //redistribute into current node -> including mid
    int newSize = 0;
    for (int i = 0; i < mid + 1; i++){
      curr.values[i] = arr[i];
      newSize++;
    }
    // don't overwrite no longer needed values, instead control with size //TODO add to readme
    // -1 so not to count mid
    curr.size = newSize - 1;
    int j = 0;
    //redistribute into new node
    for (int i = mid + 1; i < arr.length; i++){
      newNode.values[j] = arr[i];
      newNode.size++;
      j += 1;
    }

  }

  /*********** Functions for accessing node  ******************/

  private int createLeaf(){
    int nodePtr = createNode();
    nodes[nodePtr].isLeaf = true;
    return nodePtr;
  }

  private int createNode(){
    Node node = new Node();
    node.values = new int[nodeSize];
    node.children = new int[nodeSize + 1];
    node.size = 0;

    checkSize();
    nodes[cntNodes] = node;
    return cntNodes++;
  }

  /*
   * checkSize(): Resizes the node array if necessary.
   */
  private void checkSize() {
    if(cntNodes == nodes.length) {
      Node[] tmp = new Node[cntNodes << 1];
      System.arraycopy(nodes, 0, tmp, 0, cntNodes);
      nodes = tmp;
    }
  }

  public static void main(String[] args) {
    Btree bt = new Btree(2);
    bt.insert(8);
    bt.insert(5);
    bt.insert(1);
    bt.insert(7);
    bt.insert(3);
    bt.insert(12);
    bt.insert(9);
    bt.insert(6);
    bt.insert(13);
    bt.insert(14);
    bt.displayTree();
//    bt.display(2);

//    Btree b = new Btree(3);
//    b.insert(20);
//    b.insert(30);
//    b.insert(10);
//    b.insert(40);
//    b.insert(50);
//    b.insert(60);
//    b.insert(70);
//    b.insert(80);
//    b.insert(90);
//    b.insert(100);
//    b.insert(101);
//    b.insert(102);
//    b.insert(103);
//    b.insert(104);
//    b.insert(105);
//    b.insert(106);
//    b.insert(107);
//    b.insert(108);
//    b.insert(109);
//    b.insert(110);
//    b.insert(111);
//    b.insert(112);
//    b.insert(113);
//    b.insert(114);
//    b.displayTree();
  }

}


