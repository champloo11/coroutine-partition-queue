import java.util.Random
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.delay
import org.junit.Test as test

class TestCoroutinePartitionQueue() {
    @test fun `It should execute every task in the correct order, according to their partition key`() {
        // Setup conditions to make any race condition likely
        val numberOfKeys = 50
        val numberOfTestCases = 5000
        val maxTaskDelayMilliseconds = 100
        val partitionQueue = CoroutinePartitionQueue(numberOfKeys * 2)

        val keys = arrayListOf<String>()
        for (i in 1..numberOfKeys) {
            keys.add(generateUUID())
        }

        val testCases = arrayListOf<Pair<String, Int>>()
        val randomNumGenerator = Random()
        for (order in 1..numberOfTestCases) {
            val key = keys[Math.abs(randomNumGenerator.nextInt()) % keys.size]
            testCases.add(Pair(key, order))
        }

        // Add the tasks and atomically add the order they were executed into a queue
        val outputOrder = ConcurrentLinkedQueue<Pair<String, Int>>()
        testCases.forEach {
            partitionQueue.enqueue(it.first) {
                randomDelay(maxTaskDelayMilliseconds)
                outputOrder.add(it)
            }
        }

        // Wait for the execution to finish
        partitionQueue.await()

        // Validate every task was run
        assert(outputOrder.size == testCases.size)

        // Now check our results
        val prevHashMap = mutableMapOf<String, Int>()
        for (pair in outputOrder) {
            val key = pair.first
            val order = pair.second
            prevHashMap.putIfAbsent(key, order)
            val prevValue = prevHashMap.getOrDefault(key, -1)
            assert(prevValue <= order)
        }
    }

    private suspend fun randomDelay(maxMilliseconds: Int) {
        delay(Random().nextLong() % maxMilliseconds)
    }

    private fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }
}
