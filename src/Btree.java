/*
 * CS7280 Special Topics in Database Management
 * Project 1: B-tree implementation.
 * Author: Katerina Bosko
 *
 * Recursive implementation of B-tree data structure
 * Supports:
 *   1. Insert
 *   2. Lookup
 *   3. Display
 *
 * The implementation supports both odd and even number of node size,
 * i.e. maximum number of values that are allowed per node.
 *
 * Limitations: inserted value cannot be 0 because the data structure used is simple array
 * that is initialized implicitly with 0s.
 *
 * The implementation relies on left biasing after split.
 * This means that during redistribution between the current node and new node
 *  all values before mid (including middle value) go to current node,
 *  while all values after mid go to the new node.
 * The current node's size, however, doesn't count middle value --> middle value is 'invisible'
 * and resides at position curr.size.
 * As a result, the middle value after split is in the left node (i.e. current node) as its last value,
   which allows to easily promote the 'invisible' middle value into parent node at later stage.
   Such an implementation is more efficient because with right biasing
   one would need to implement more steps -> pop the middle value from the new node when it's full.
 */


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class Btree {

  /* Default size of Node for simple constructor */
  private static final int NODESIZE = 5;

  /* Node array, initialized with length = 1. i.e. root node */
  private Node[] nodes = new Node[1];

  /* Number of currently used nodes. */
  private int cntNodes = 0;

  /* Pointer to the root node. */
  private int root;

  /* Number of currently used values. */
  private int cntValues = 0;

  /* Size of Node */
  private final int nodeSize;

  /* middle position in an array of nodeSize, differs if nodeSize is odd or even  */
  private final int mid;

  /**
   * B tree Constructor.
   */
  public Btree() {
    nodeSize = NODESIZE;
    root = createNode();
    nodes[root].isLeaf = false;
    nodes[root].children[0] = createLeaf();
    mid = ((nodeSize + 1) % 2 == 0)? (nodeSize + 1)/2 - 1:  Math.floorDiv(nodeSize + 1, 2);
  }

  /**
   * B tree overloaded constructor to specify node size
   */
  public Btree(int nodeSize) {
    this.nodeSize = nodeSize;
    root = createNode();
    nodes[root].isLeaf = false;
    nodes[root].children[0] = createLeaf();
    mid = ((nodeSize + 1) % 2 == 0)? (nodeSize + 1)/2 - 1:  Math.floorDiv(nodeSize + 1, 2);
  }

  /*********** B tree functions for Public ******************/

  /**
   * Lookup(int value)
   *   - True if the value was found.
   */
  public boolean lookup(int value) {
    return nodeLookup(value, root);
  }

  /**
   * Insert(int value)
   *    - If -1 is returned, the value is inserted and increase cntValues.
   *    - If -2 is returned, the value already exists.
   */
  public void insert(int value) {
    if(nodeInsert(value, nodes[root]) == -1) cntValues++;
  }

  /**
   * CntValues()
   *    - Returns the number of used values.
   */
  public int getCntValues() {
    return cntValues;
  }

  /**
   * displayTree()
   *    - prints all values stored in btree, indented by level for better comprehension
   */
  public void displayTree(){
    System.out.println("********************");
    System.out.println("\nDisplaying the B-tree");
    System.out.println("node size: " + nodeSize);
    System.out.println("********************");
    displayNode(root, 0);
    System.out.println();
  }

  /**
   *  display(int node) per requirement
   *  prints all values under specified node pointer, indents for better comprehension
   */
  public void display(int nodePtr){
    displayNode(nodePtr, 0);
  }

  /*********** B-tree functions for Internal  ******************/

  /**
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
    //else the node is no leaf node:
    else {
      //check current node
      if (found(curr, value)){
        return true;
      }
      //go down the tree
      else {
        //find the child node for the current node
        int childPtr = findChild(curr, value);
        //start recursion to go down the tree
        return nodeLookup(value, childPtr);
      }
    }
  }

  /**
   * displayNode(int nodePtr, int level)
   *    traverses recursively btree and prints its values level by level
   *    prints values with indentation based on level for better comprehension
   */
  private void displayNode(int nodePtr, int level){
    //checkout node based on its pointer
    Node curr = nodes[nodePtr];
    //indent based on level
    System.out.print("  ".repeat(level));
    //print values in this node
    for (int i = 0; i < curr.size; i++) {
      System.out.print(curr.values[i] + " ");
    }
    System.out.println();
    //go down the tree recursively if current node is not leaf node
    if (!curr.isLeaf){
      level++;
      int numChildren = curr.size + 1;
      for (int i = 0; i < numChildren; i++){
        displayNode(curr.children[i], level);
      }
    }
  }

  /**
   * nodeInsert(int value, int pointer)
   *    - -2 if the value already exists in the specified node
   *    - -1 if the value is inserted into the node or
   *    integer pointer to newly created child if the parent node has to be restructured
   */
  private int nodeInsert(int value, Node curr) {
    if (found(curr, value)){
      return -2;
    }
    //if the current node is a leaf node
    if (curr.isLeaf) {
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
        redistributeValues(curr, value, newLeaf);
        return newLeafPtr;
      }
    }
    //current node is not leaf, i.e. inner node
    else {
      //find the correct child pointer for the current node
      int childPtr = findChild(curr, value);
      //checkout child node based on its pointer
      Node child = nodes[childPtr];
      //insert the value into child
      int newChildPtr = nodeInsert(value, child);
      //no split -> done
      if (newChildPtr == -2 || newChildPtr == -1){
        return newChildPtr;
      }
      // after split
      //if there is space inside current node
      else if (curr.size < nodeSize) {
        //mid invisible at index equal to size, i.e. next value after 'visible' values
        int mid = child.size;
        int midValue = child.values[mid];
        //promote mid value via left bias -> copy over child's last value (i.e. mid) into current node
        curr.values[curr.size] = midValue;
        curr.size++;
        //link current node with new child
        curr.children[curr.size] = newChildPtr;
        //promoted middle value must be placed at correct position (currently added at the end)
        // -> sort values and children
        sortValuesAndChildren(curr, midValue);
        return -1;
      }
      //no space inside current node
      else {
        //create a new node
        int newNodePtr = createNode();
        //checkout new node
        Node newNode = nodes[newNodePtr];
        //find new middle value to be promoted
        int midVal = child.values[child.size];
        //redistribute values between current node and new node & return position at which value was inserted
        int insertPos = redistributeValues(curr, midVal, newNode);
        redistributeChildren(curr, newChildPtr, newNode, insertPos);

        //if current node is not root
        if (curr != nodes[root]){
          //return pointer to new node
          return newNodePtr;
        }
        else {
          //create and set a new root node
          int newRootPtr = createNode();
          //checkout new root
          Node newRoot = nodes[newRootPtr];
          //link new root with old root and new node created in the previous step
          newRoot.children[0] = root;
          newRoot.children[1] = newNodePtr;
          root = newRootPtr;
          //promote the mid value to newRoot
          int mid = curr.size; //mid is invisible at index size
          newRoot.values[0] = curr.values[mid];
          newRoot.size++;
        }
        return -1;
      }
    }
  }

  /**
   * found(int value)
   *   helper function to check if the value is stored in the current node
   *   - true if found
   *   - false if not found
   */
  private boolean found(Node curr, int value){
    // check  if value exists
    for (int i = 0; i < nodeSize; i++) {
      if (curr.values[i] == value) {
        return true; // exit of found
      }
    }
    return false;
  }

  /**
   * insertValue(int value)
   *   helper function to insert the value at correct position
   *   in array of values that has increasing order
   */
  private void insertValue(Node curr, int value){
    int i = 0;
    //find where to insert by moving other values to the right if needed
    for (i = curr.size - 1; i >= 0 && value < curr.values[i]; i--){
      curr.values[i + 1] = curr.values[i];
    }
    //insert value
    curr.values[i + 1] = value;
    curr.size += 1;
  }

  /**
   * findChild(int value)
   *   helper function to find the correct child of the current node
   *   where value will be processed
   *   returns pointer to a child
   */
  private int findChild(Node curr, int value){
    int i = 0;
    //find the correct index by comparing value to values in the current node
    while (i < curr.size && curr.values[i] < value){
      i += 1;
    }
    return curr.children[i];
  }

  /**
   * redistribute(int value, Node newNode)
   *   helper function to redistribute values between current node
   *   and newly created node based on mid position
   *   1. the values to be redistributed include the value to be inserted and are sorted
   *   2. the values are sorted to find the middle value that will be later promoted into parent
   *   3. values are redistributed based on left bias:
   *      - all values up to mid value (inclusive) stay in the current node
   *      - all values after mid value go to new node
   *   NOTE: mid position for even node size is determined by taking the first value
   *   to the left from the middle after splitting array in half
   *
   */

  private int redistributeValues(Node curr, int value, Node newNode){
    //create temporary array that will hold all values from current node and value to be added
    int[] arr = new int[nodeSize + 1];

    //copy over all values in the current node to temporary array
    int insertPos = 0;
    int i = 0;
    for (i = 0; i < curr.size; i++){
      if (curr.values[i] < value){
        arr[i] = curr.values[i];
      }
      else {
        arr[i] = value;
        insertPos = i;
        break;
      }
    }
    if (i < curr.size){
      for (int j = i; j < curr.size; j++){
        arr[j + 1] = curr.values[j];
      }
    }
    else {
      arr[i] = value;
      insertPos = i;
    }

    //redistribute into current node -> all values up to and including mid value
    int newSize = 0;
    for (int k = 0; k < mid + 1; k++){
      curr.values[k] = arr[k];
      newSize++;
    }
    // don't overwrite no longer needed values, instead control with size
    // -1 so not to count mid (to make it 'invisible')
    curr.size = newSize - 1;

    int j = 0;
    //redistribute into new node -> all values after mid
    for (int m = mid + 1; m < arr.length; m++){
      newNode.values[j] = arr[m];
      newNode.size++;
      j++;
    }
    return insertPos;
  }

  private void redistributeChildren(Node curr, int newChildPtr, Node newNode, int insertPos){
    int[] arr = new int[nodeSize + 2];
    int newChildPos = insertPos + 1;
    int midForChildren = mid + 1;
    //copy over all children of curr and newChildPtr (at correct position) into temporary array
    int j = 0;
    for (int i = 0; i < arr.length; i++){
      if (i == newChildPos){
        arr[i] = newChildPtr;
        //i++;
      }
      else {
        arr[i] = curr.children[j];
        j++;
      }
    }
    int numChildrenToTransfer = arr.length - midForChildren;

    //redistribute children into new node & reset children for current node
    for (int k = 0; k < numChildrenToTransfer; k++){
      newNode.children[k] = arr[k + midForChildren];
      curr.children[k + midForChildren - 1] = 0;
    }
    //redistribute children into current node
    for (int m = 0; m < midForChildren; m++){
      curr.children[m] = arr[m];
    }
  }

  private void sortValuesAndChildren(Node curr, int value){
    //if midValue is less than the last but one value in current node -> sort
    if (curr.size > 1 && value < curr.values[curr.size - 2]){
      int i;
      int lastChild = curr.children[curr.size];
      //find where to insert by moving other values
      for (i = curr.size - 2; i >= 0 && value < curr.values[i]; i--){
        curr.values[i + 1] = curr.values[i];
        curr.children[i + 2] = curr.children[i + 1];
      }
      //insert key
      curr.children[i + 2] = lastChild;
      curr.values[i + 1] = value;
    }
  }

  /*********** Functions for accessing node  ******************/

  /**
   * createLeaf(): Creates a new leaf node
   * @return node pointer to new leaf
   */
  private int createLeaf(){
    int nodePtr = createNode();
    nodes[nodePtr].isLeaf = true;
    return nodePtr;
  }

  /**
   * createNode(): Creates a new node
   * values are stored in array of size nodeSize
   * pointers to children nodes are stored in array of size 1 larger than nodeSize
   * @return total number of nodes in a btree
   */
  private int createNode(){
    Node node = new Node();
    node.values = new int[nodeSize];
    node.children = new int[nodeSize + 1];
    node.size = 0;

    checkSize();
    nodes[cntNodes] = node;
    return cntNodes++;
  }

  /**
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

    Btree b = new Btree(3);
    int numValues = 30;
    int[] randomNumArr = new int[numValues];
    System.out.println("Inserting: ");
    for (int i = 0; i < numValues; i++) {
      //generate random number in range 0 to 100
      int randomNum = 1 + (int) (Math.random() * ((100 - 1) + 1));
      randomNumArr[i] = randomNum;
      System.out.print(randomNum + ", ");
      b.insert(randomNum);
    }

    int[] nonRepeatingArr = Arrays.stream(randomNumArr).distinct().toArray();
    System.out.println(
        "\n\ntried to insert (potentially with duplicates): " + numValues + " values");
    System.out.println("must be inserted: " + nonRepeatingArr.length + " values");
    System.out.println("de facto inserted: " + b.getCntValues() + " values");

    if (numValues != b.getCntValues()) {
      List<Integer> randomNumList = Arrays.stream(randomNumArr).boxed()
          .collect(Collectors.toList());

      System.out.println("\nInserted without duplicates:");
      randomNumList.stream()
          .distinct()
          .forEach(e -> System.out.print(e + " "));
      System.out.println();
    }
    b.displayTree();

  }
}


