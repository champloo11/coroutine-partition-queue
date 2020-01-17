import com.google.common.hash.Hashing
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class CoroutinePartitionQueue(private val partitionCount: Int) {
    private val murmur = Hashing.murmur3_128(Math.abs(Random().nextInt()))
    private var partitions = Array(partitionCount) { ConcurrentLinkedQueue<suspend () -> Unit>() }
    private var waitingToFinish: Boolean = false
    private val previousTaskStatus = Array(partitionCount) { TaskStatus.PAUSED }
    private val taskExecutor = thread(start = true) {
        var queueIndex = 0

        while (!allTasksFinished()) {
            val queue = partitions[queueIndex]

            if (previousTaskStatus[queueIndex] == TaskStatus.PAUSED) {
                val task: (suspend () -> Unit)? = queue.poll()
                task?.let {
                    runTask(task, queueIndex)
                }
            }

            queueIndex = (queueIndex + 1) % partitions.size
        }
    }

    private fun runTask(task: suspend () -> Unit, index: Int) {
        previousTaskStatus[index] = TaskStatus.RUNNING

        GlobalScope.async {
            task()
            previousTaskStatus[index] = TaskStatus.PAUSED
        }
    }
    private fun allTasksFinished(): Boolean {
        return partitions.all { it.isEmpty() } && previousTaskStatus.all { it == TaskStatus.PAUSED } && waitingToFinish
    }

    private fun hash(key: String): Long {
        return Math.abs(murmur.hashBytes(key.toByteArray()).asLong())
    }

    /**
     * Add a task to be executed in order based on the partition specified by the key.
     *
     * @param key
     * @param task
     * @return
     */
    fun enqueue(key: String, task: suspend () -> Unit): CoroutinePartitionQueue {
        val hash = hash(key)
        val partition = (hash % partitionCount).toInt()
        val queue = partitions[partition]
        queue.add(task)
        return this
    }

    /**
     * Await all tasks that are currently in the queue to be finished executing.
     *
     */
    fun await() {
        waitingToFinish = true
        taskExecutor.join()
    }
}
