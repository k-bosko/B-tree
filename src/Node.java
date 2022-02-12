/*
 * CS7280 Special Topics in Database Management
 * Project 1: B-tree implementation.
 * Author: Katerina Bosko

 * Node data structure.
 */

final class Node {
  /* node values  */
  int[] values;

  /* node array pointing to the children nodes */
  int[] children;

  /* number of entries
   * (Rule in B Trees:  d <= size <= 2 * d).
   */
  int size;

  /* flag for a node if it is a leaf */
  boolean isLeaf;


  /**
   * insert(int value)
   *   inserts the value at correct position
   *   into node's array of values in an increasing order
   *   sorts node's children accordingly
   */
  public int insert(int value){
    int i;
    //find where to insert by moving other values
    for (i = this.size; i > 0 && value < this.values[i - 1]; i--){
      this.values[i] = this.values[i - 1];
      //sort children as well (relevant for inner nodes with space)
      this.children[i + 1] = this.children[i];
    }
    //insert value
    this.values[i] = value;
    this.size++;
    // update children after insert (relevant for inner nodes with space)
    this.children[i + 1] = this.children[i];

    return i;
  }

  /**
   * findChild(int value)
   *   finds the correct child of the current node
   *   where value will be processed
   *   returns pointer to a child
   */
  public int findChild(int value){
    int i = 0;
    //find the correct index by comparing value to values in the current node
    while (i < this.size && this.values[i] < value){
      i += 1;
    }
    return this.children[i];
  }

  /**
   * redistributeValues(int value, Node newNode)
   *   redistributes values between current node
   *   and newly created node based on mid position
   *   1. the values to be redistributed include all values of current node + value to be inserted
   *      stored into temporary array
   *   2. the values are sorted to find the middle value that will be later promoted into parent
   *   3. values are redistributed based on left bias:
   *      - all values up to mid value (inclusive) stay in the current node
   *      - all values after mid value go to new node
   *   NOTE: mid position for even node size is determined by taking the first value
   *   to the left from the middle after splitting array in half
   */

  public int redistributeValues(int value, Node newNode){
    //create temporary array that will hold all values from current node and value to be inserted
    //because we redistribute only when node is full, this.size = nodeSize
    int arrLen = this.size + 1;
    int[] arr = new int[arrLen];

    //calculate mid position of values -> depends on whether array is odd or even
    int mid = (arrLen % 2 == 0)? arrLen/2 - 1:  Math.floorDiv(arrLen, 2);

    //copy over all values of the current node and value to insert (at correct position)
    // to temporary array in increasing order
    int insertPos = 0;
    int i;
    for (i = 0; i < this.size; i++){
      if (this.values[i] < value){
        arr[i] = this.values[i];
      }
      else {
        arr[i] = value;
        insertPos = i;
        break;
      }
    }
    if (i < this.size){
      for (int j = i; j < this.size; j++){
        arr[j + 1] = this.values[j];
      }
    }
    else {
      arr[i] = value;
      insertPos = i;
    }

    //redistribute into current node -> all values up to and including mid value
    int newSize = 0;
    for (int k = 0; k < mid + 1; k++){
      this.values[k] = arr[k];
      newSize++;
    }
    // don't overwrite no longer needed values, instead control with size
    // -1 so not to count mid (to make it 'invisible')
    this.size = newSize - 1;

    int j = 0;
    //redistribute into new node -> all values after mid
    for (int m = mid + 1; m < arr.length; m++){
      newNode.values[j] = arr[m];
      newNode.size++;
      j++;
    }
    return insertPos;
  }

  /**
   * redistributeChildren(int newChildPtr, Node newNode, int insertPos)
   *   redistributes children between current node
   *   and newly created node based on mid position
   *   1. the children to be redistributed include all children of current node + child to be inserted
   *   2. all children are copied over into temporary array in special order. The position of new child
   *   is based on position of the inserted value into parent node during redistribution of values
   *   3. children are redistributed as following:
   *      - all children before mid value go to current node
   *      - all children after mid value go to new node
   *   NOTE: mid position for even node size is determined by taking the first value
   *   to the left from the middle after splitting array in half
   */
  public void redistributeChildren(int newChildPtr, Node newNode, int insertPos){
    //create temporary array to hold all children from current node + a child to be inserted (+1)
    int arrLen = this.children.length + 1;
    int[] arr = new int[arrLen];
    // position of new child is 1 greater than insert position of value into node
    int newChildPos = insertPos + 1;

    // calculate mid position for temporary array of children
    int mid = ((arrLen + 1) % 2 == 0)? (arrLen + 1)/2 - 1:  Math.floorDiv(arrLen + 1, 2);

    //copy over all children of this node and newChildPtr (at correct position) into temporary array
    int j = 0;
    for (int i = 0; i < arr.length; i++){
      if (i == newChildPos){
        arr[i] = newChildPtr;
      }
      else {
        arr[i] = this.children[j];
        j++;
      }
    }
    //all children after mid go to new node
    int numChildrenToTransfer = arrLen - mid;

    //redistribute children into new node
    for (int k = 0; k < numChildrenToTransfer; k++){
      newNode.children[k] = arr[k + mid];
      //reset children for current node
      this.children[k + mid - 1] = 0;
    }
    //redistribute children into current node
    for (int m = 0; m < mid; m++){
      this.children[m] = arr[m];
    }
  }
}