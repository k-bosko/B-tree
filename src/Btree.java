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
  private int cntNodes;

  /* Pointer to the root node. */
  private int root;

  /* Number of currently used values. */
  private int cntValues;

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
    if(nodeInsert(value, root) == -1) cntValues++;
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
  private int nodeInsert(int value, int pointer) {
    Node curr = nodes[pointer];
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
        // create new node
        int newNodePtr = createLeaf();
        redistribute(curr, value, newNodePtr); //TODO
        return newNodePtr;
      }
    }
    //NOT LEAF
    else {
      //find the child node for the current node
      int child = findChild(root, value);
      int newChild = nodeInsert(value, child);
      //no split -> done
      if (newChild == -2 || newChild == -1){
        return newChild;
      }
      // splitted
      //if there is space inside current node
      else if (curr.size < nodeSize) {
        //insert the new child pointer into the the current node
        curr.children[child] = newChild;
        // insert the new child's first value (i.e. mid) into the the current node --> promote? //TODO check
        //left bias -> insert first child's last value (i.e. mid) into current node
        curr.values[curr.size] = nodes[child].values[nodeSize - 1];
        curr.size++;
        return -1;
      }
      //no space
      else {
        //create a new node
        int newNodePtr = createNode(); //TODO or leaf?
        //distribute values and child pointerS in the current and new node
        //redistribute();
        if (curr != nodes[root]){
          return newNodePtr;
        }
        else {
          //create and set a new root node
          //initialize the root node with the pointers and values of the current and new node
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

  private int findChild(int currPtr, int value){
    int i = 0;
    while (i < nodes[currPtr].size && nodes[currPtr].values[i] < value){
      i += 1;
    }
    return nodes[currPtr].children[i];
  }

  private void redistribute(Node curr, int value, int newNodePtr){
    Node newNode = nodes[newNodePtr];
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
    Btree bt = new Btree(2);
    bt.Insert(8);
    bt.Insert(5);
    bt.Insert(1);
    bt.Insert(7);
    bt.Insert(3);
    bt.Insert(12);
    bt.Insert(9);
    bt.Insert(6);
    bt.Insert(13);

  }

}


