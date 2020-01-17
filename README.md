# Kotlin Coroutine Partition Queue
[![Build Status](https://travis-ci.com/champloo11/coroutine-partition-queue.svg?branch=master)](https://travis-ci.com/champloo11/coroutine-partition-queue)

Partition IO-bound tasks by a key and execute each within a coroutine, maintaining FIFO-order per group and increased 
throughput compared to thread-based approaches.

![Partition Queue HLO](https://github.com/champloo11/coroutine-partition-queue/raw/master/src/main/resources/partitionQueue.png)

## Usage

```kotlin
val partitionQueue = CoroutinePartitionQueue(5) // Configure number of partitions
val processRow: suspend (row: Row) -> Unit = {} // Any suspendable function

for(row in data) {
  partitionQueue.enqueue(row.key) {
    // This callback will be executed within a coroutine context
    proccessRow(row)
  }
}

// Now wait for all rows to be processed
partitionQueue.await()
```

## Purpose

Often (particularly in batch processing) we find ourselves needing to process a batch of records where:

- The records need to be processed in a particular order
- **But** the records do not need to be processed in a global order 

This means each record can be divied up into a group of tasks (partitioned), and as long as all the groups are processed
order respective to all the records in the group, we will have properly processed them. 

Often (but not always) the records are:

- Being streamed into our code (via a database, object store, or client)
- Being written back out through the network to a database, object store, or client

This library has uses in:

- Extract-Transform-Load (ETL) processing
- Server side request/response queuing
- etc...

This library is **NOT** meant for processing large amounts of CPU-intensive data, and is better suited towards workloads that
send a lot of data over IO.

## Implementation

There are four primary components CoroutinePartitionQueue

### Tasks

Tasks are units of execution that can be partitioned into groups, known as a key.

### Executor

A thread that loops through each partition queue and pops off a task when that partition is ready for more work, and
executes that task in a coroutine.

### Task Distribution

Task distribution is handled by the high quality, fast, non-cryptographic hash function MurmurHash3 modulus the number of
partitions.

### Task Partition Queue

A ConcurrentLinkedQueue that can be written to safely while the executor is pulling work off of the queue.
