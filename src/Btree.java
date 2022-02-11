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

  private int nodeSize;

  private int mid;
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
  public boolean Lookup(int value) {
    return nodeLookup(value, root);
  }

  /*
   * Insert(int value)
   *    - If -1 is returned, the value is inserted and increase cntValues.
   *    - If -2 is returned, the value already exists.
   */
  public void Insert(int value) {
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
   *
   */
  private boolean nodeLookup(int value, int pointer) {
    //
    //
    // To be coded .................
    //
    //
    return false; //TODO
  }

  /*
   * nodeInsert(int value, int pointer)
   *    - -2 if the value already exists in the specified node
   *    - -1 if the value is inserted into the node or
   *            something else if the parent node has to be restructured
   */
  private int nodeInsert(int value, Node curr) {
    if (curr.isLeaf) {
      if (Arrays.asList(curr.values).contains(value)) {
        return -2;
      }
      //if there is space -> insert value into leaf node in sorted order
      else if (curr.size < nodeSize) {
        insertValue(curr, value);
        return -1;
      }
      //if there is no space
      else {
        // create new leaf node
        int newNodePtr = createLeaf();
        Node newNode = nodes[newNodePtr];
        redistribute(curr, value, newNode);
        return newNodePtr;
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
      // splitted
      //if there is space inside current node
      else if (curr.size < nodeSize) {
        // insert the new child's first value (i.e. mid) into the the current node --> promote? //TODO check
        //left bias -> insert first child's last value (i.e. mid) into current node
        curr.values[curr.size] = child.values[child.size]; //mid invisible at index equal to size
        curr.size++;
        //insert the new child pointer into the the current node
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
        //pop last value -> reduce size of curr
//        curr.size--;
        //copy over mid value into new node
//        newNode.values[0] = child.values[child.size]; //mid value is last value that is not counted in size
//        newNode.size++;
        //update child pointerS for new node
        int i;
        for (i = 0; i < mid; i++){
          newNode.children[i] = curr.children[i + mid + 1];
        }
        newNode.children[i] = newChildPtr;
        //add curr's last child (size already decremented, but link still there --> +1) to newNode
//        newNode.children[0] = curr.children[curr.size + 1];
//        //link newChild with newNode
//        newNode.children[1] = newChildPtr;

        if (curr != nodes[root]){
          redistribute(curr, midVal, newNode);
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
      newNode.size += 1;
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
//    Btree bt = new Btree(2);
//    bt.Insert(8);
//    bt.Insert(5);
//    bt.Insert(1);
//    bt.Insert(7);
//    bt.Insert(3);
//    bt.Insert(12);
//    bt.Insert(9);
//    bt.Insert(6);
//    bt.Insert(13);

    Btree b = new Btree(2);
    b.Insert(20);
    b.Insert(30);
    b.Insert(10);
    b.Insert(40);
    b.Insert(50);
    b.Insert(60);
    b.Insert(70);
    b.Insert(80);
    b.Insert(90);
    b.Insert(100);
    b.Insert(101);
    b.Insert(102);
    b.Insert(103);
    b.Insert(104);
    b.Insert(105);
    b.Insert(106);
    b.Insert(107);
    b.Insert(108);
    b.Insert(109);
    b.Insert(110);
    b.Insert(111);
    b.Insert(112);
    b.Insert(113);
    b.Insert(114);
  }

}


