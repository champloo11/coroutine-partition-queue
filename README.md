# Kotlin Coroutine Partition Queue

Partition IO-bound tasks by a key and execute each within a coroutine, maintaining FIFO-order per group and increased 
throughput compared to thread-based approaches.

![Partition Queue HLO](https://github.com/champloo11/coroutine-partition-queue/raw/master/src/main/resources/partitionQueue.png)

## Usage

```kotlin

```

## Purpose

Often (particularly in batch processing) we find ourselves needing to process a batch of records where:

- The records need to be processed in a particular order
- **But** the records do not need to be processed in a global order 

This means each record can be divied up into a group of tasks (partitioned), and as long as all the groups are processed order respective to all the records in the group, we will have properly processed them. 

Often (but not always) the records are:

- Being streamed into our code (via a database, object store, or client)
- Being written back out through the network to a database, object store, or client

This library has uses in:

- Extract-Transform-Load (ETL) processing
- Server side request/response queuing
- etc...
