# Gitlet Design Document

**Name**: Jeffrey Huang

## Classes and Data Structures

### Commit

1. Variables 
- message (instance) - the give message
- timestamp (instance) - time of creation
- parent (instance) - the parent of the commit
- SHA1code (instace) - SHA1code of this commit
- commitMap (static) - Holds filename -> SHA1code


2. Methods
- Commit() - Constructor that takes in String message, String timestamp, String parent
- getDate() - Returns the current timestamp
- getSHA1(byte[]) - Makes a SHA1 code for the commit
- getCommitSHA1code - Returns SHA1code of commit (makes this not naked)
- commitSHA1(Object) - Makes a SHA1 code of the Object
 - getCommitSHA1code() - Returns SHA1code of commit (makes this not naked).
 - saveCommmit() - Saves a commit to a file for future use
- fromFile(SHA1code) - Turns a file to a commit.
- getLogMessage - Returns the log message for this commit.
- getCommitMap() - Returns the commitMap.
- getParent() - Returns the parent.

### Repository

1. Variables
- CWD (static) - current working directory
- GITLET_DIR (static) - .gitlet


2. Methods
- init() - creates Gitlet directory, initial commit, master branch
- add(filename) - add filename to staging area
- commit(message) - makes a commit and clears staging area (transfers info to commit)
- log() - returns a log of the commits
- checkout1, checkout2, checkout3 - different forms of checking out a file

### StagingArea

1. Variables
- stagingArea (static) - file pathway
- additionMap (static) - Hashmap that contains filename -> blob SHA1 for addition
- deletionMap (static) - Hashmap that contains filename -> blob SHA1 for deletion

2. Methods
- saveStagingArea() - Saves the stagingArea to a file for future use.
- fromFile() - Returns the StagingArea
- addToAdditon - add file name and blob to  addition ArrayList
- addToDeletion - add file name and blob to deletion ArrayList
- clear() - Clears the StagingArea by resetting the additionMap and deletionMap
### Blobs

1. Variables
- contents (instance) - String contents of a file
- blobSHA1code (instance) - SHA1code of a blob

2. Methods
- Blob() constructor - makes contents and SHA1code
- saveBlob() - Saves a blob to a file for future use
- fromFile(SHA1code) - Turns file to Blobs.
- checkoutRewrite - Helps checkout #1 rewrite the file in current directory.


### Trees

1. Variables
- master (static) - master branch
- head (static) - head branch
- treePath (static) - Pathway for the tree
- allCommits (static) - contains all commits ever made SHA1code -> Commit

2. Methods
- Tree() - empty constructor
- saveTree() - Saves a commit to a file for future use
- fromFile() - Turns a file to a tree
- getAllCommitsMap() - Returns the AllCommitsMap of this Commit.
- getCommit(SHA1code) - Returns the Commit given a SHA1code

### Class 5

1. Field 1
2. Field 2


## Algorithms

## Persistence

