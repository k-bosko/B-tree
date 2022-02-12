# B-tree
Recursive implementation of B-tree data structure for multi-level indexing in database management systems

### Supports:
1. Insert
2. Lookup
3. Display

### Implementation:
B-tree data structure consists of array of values and array of children that are integer pointers to nodes. Nodes array tracks all nodes inserted in B-tree.
 
The implementation supports both **odd and even number of node size**, i.e. maximum number of values that are allowed per node. <br>
**Note**: Mid position for even node size is determined by taking the first value to the left from the middle after splitting array in half.
 
The implementation relies on **left biasing** after split.
This means that during redistribution between the current node and new node all values before mid (including middle value) go to current node,  while all values after mid go to the new node.
The current node's size, however, doesn't count middle value. Therefore, middle value is 'invisible' and resides at position equal to current nodes’ size.
As a result, the middle value after split is in the left node (i.e. current node) as its last value, which allows to easily promote the 'invisible' middle value into parent node at later stage.
Such an implementation is more efficient because with right biasing  one would need to implement more steps -> pop the middle value from the new node when it's full.

### Limitations:
1) this implementation doesn't store a data pointer to a physical location on disk (like it would be normally done in DBMS)
2) inserted value cannot be 0 because the data structure used is simple array
that is initialized implicitly with 0s.
