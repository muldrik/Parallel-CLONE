package com.h0tk3y.spbsu.parallel

import org.junit.Assume
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@OptIn(ExperimentalTime::class)
class Tests {
    private val solution = solution(8)

    private val random = Random(0)

    private val largeNData = 5_000
    private val smallNData = 200
    private val largeIntData = (1..largeNData).map { random.nextInt() }
    private val smallIntData = (1..smallNData).map { random.nextInt() }
    private val stressTestRuns = 20

    private fun <T> testSequentialAndConcurrent(
        sequential: () -> T,
        concurrent: () -> T
    ) {
        val (valueSequential, durationSequential) = measureTimedValue { sequential(); sequential() }
        println("Sequential: ${durationSequential.inMilliseconds.roundToInt()}ms")
        val (valueConcurrent, durationConcurrent) = measureTimedValue { concurrent(); concurrent() }
        println("Concurrent: ${durationConcurrent.inMilliseconds.roundToInt()}ms")
        assertEquals(valueSequential, valueConcurrent)
    }

    @Test
    fun testMinByOrNullLarge() = testMinByOrNull(largeIntData)

    @Test
    fun testMinByOrNullStress() = repeat(stressTestRuns) {
        testMinByOrNull(smallIntData)
    }

    private fun testMinByOrNull(data: List<Int>) {
        val comparator = compareBy<Int> { item ->
            val iterations = item.toString().length.let { it * it * it * it }
            buildString { repeat(iterations) { append(item) } }.hashCode()
        }
        testSequentialAndConcurrent(
            { data.minWithOrNull(comparator) },
            { solution.minWithOrNull(data, comparator) }
        )
    }

    @Test
    fun testAllLarge() = testAll(largeIntData)

    @Test
    fun testAllStress() = repeat(stressTestRuns) {
        testAll(smallIntData)
    }


    private fun testAll(data: List<Int>) {
        val mapping = { item: Int ->
            val iterations = item.toString().length.let { it * it * it * it }
            buildString { repeat(iterations) { append(item) } }
        }
        val testCases = listOf(
            3000,
            data[0],
            data[100],
            data[data.size / 20 * 11],
            data[data.size / 40 * 21]
        )
        testCases.forEach { expected ->
            println("expected = $expected, ${expected in data}")
            val predicate: (Int) -> Boolean = { item ->
                mapping(item) == mapping(expected)
            }
            testSequentialAndConcurrent(
                { data.any(predicate) },
                { solution.any(data, predicate) }
            )
        }
    }

    @Test
    fun testFilterLarge() = testFilter(largeIntData)

    @Test
    fun testFilterStress() = repeat(stressTestRuns) {
        testFilter(smallIntData)
    }

    private fun testFilter(data: List<Int>) {
        Assume.assumeTrue(solution is AdvancedParallelProcessor)
        solution as AdvancedParallelProcessor

        val predicate = { item: Int ->
            val iterations = item.toString().length.let { it * it * it * it }
            val string = buildString { repeat(iterations) { append(item) } }
            string.all { (it - '0') % 2 == 0 }
        }
        testSequentialAndConcurrent(
            { data.filter(predicate) },
            { solution.filter(data, predicate) }
        )
    }

    @Test
    fun testMapLarge() = testMap(largeIntData)

    @Test
    fun testMapStress() = repeat(stressTestRuns) {
        testMap(smallIntData)
    }

    fun testMap(data: List<Int>) {
        Assume.assumeTrue(solution is AdvancedParallelProcessor)
        solution as AdvancedParallelProcessor

        val mapping = { item: Int ->
            val iterations = item.toString().length.let { it * it }
            buildString { repeat(iterations) { append(item).reverse() } }
        }
        testSequentialAndConcurrent(
            { data.map(mapping) },
            { solution.map(data, mapping) }
        )
    }

    @Test
    fun testToString() {
        Assume.assumeTrue(solution is AdvancedParallelProcessor)
        solution as AdvancedParallelProcessor

        class SlowToString(val i: Long) {
            override fun toString(): String {
                Thread.sleep(i) // imitate active work :)
                return i.toString()
            }
        }

        val data = (1..100).map { SlowToString(random.nextLong(50)) }
        testSequentialAndConcurrent(
            { data.joinToString(", ") },
            { solution.joinToString(data, ", ") }
        )
    }

    @Test
    fun testCalledOnceOnEachElement() {
        val data = (1..10000).toList()
        val mapAll = ConcurrentHashMap<Int, Boolean>()
        val resultAll = solution.all(data) { mapAll.put(it, true) != true }
        assertEquals(mapAll.keys, data.toSet())
        assertTrue(resultAll)

        if (solution is AdvancedParallelProcessor) {
            val mapMap = ConcurrentHashMap<Int, Int>()
            val mapResult = solution.map(data) { mapMap.put(it, -it) ?: it }
            assertEquals(mapMap.keys, data.toSet())
            assertEquals(data, mapResult)
        }
    }

}