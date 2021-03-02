package com.h0tk3y.spbsu.parallel

import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.math.max

/**
 * Return your implementation of [ParallelProcessor]. To enroll for the advanced task, make it implement
 * [AdvancedParallelProcessor] as well.
 */
fun solution(numberOfThreads: Int): ParallelProcessor {
    class MyProc(override val numberOfThreads: Int) : AdvancedParallelProcessor {

        private fun <T, R> withSlices(list: List<T>, block: List<T>.(Int) -> R) {
            val subListSize = list.size / numberOfThreads + 1
            val threadList = mutableListOf<Thread>()
            for (currentThread in 0 until numberOfThreads) {
                threadList.add(thread {
                    val startIndex = currentThread * subListSize
                    val maxIndex = minOf(startIndex + subListSize - 1, list.size - 1)
                    list
                        .slice(startIndex..maxIndex)
                        .block(currentThread)
                })
            }
            threadList.forEach { it.join() }
        }

        override fun <T> filter(list: List<T>, predicate: (T) -> Boolean): List<T> {
            TODO("Not yet implemented")
        }

        override fun <T, R> map(list: List<T>, function: (T) -> R): List<R> {
            TODO("Not yet implemented")
        }

        override fun <T> joinToString(list: List<T>, separator: String): String {

        }

        override fun <T : Any> minWithOrNull(list: List<T>, comparator: Comparator<T>): T? {
            val localResults = MutableList(numberOfThreads) {list[0]}
            val block: List<T>.(Int) -> Unit = { currentThread ->
                this
                    .minWithOrNull(comparator)
                    ?.let { localResults[currentThread] = minOf(it, localResults[currentThread], comparator) }
            }
            return localResults.minWithOrNull(comparator)

            /*if (list.isEmpty())
                return null
            val subListSize = list.size / numberOfThreads + 1
            val threadList = mutableListOf<Thread>()
            val localResults = MutableList(numberOfThreads) {list[0]}
            for (currentThread in 0 until numberOfThreads) {
                threadList.add(thread {
                    val startIndex = currentThread * subListSize
                    val maxIndex = minOf(startIndex + subListSize - 1, list.size - 1)
                    list
                        .slice(startIndex..maxIndex)
                        .minWithOrNull(comparator)
                        ?.let {localResults[currentThread] = minOf(it, localResults[currentThread], comparator)}
                })
            }
            threadList.forEach { it.join() }
            return localResults.minWithOrNull(comparator)*/
        }

        override fun <T> all(list: List<T>, predicate: (item: T) -> Boolean): Boolean {
            var result = true
            val subListSize = list.size / numberOfThreads + 1
            val threadList = mutableListOf<Thread>()
            for (currentThread in 0 until numberOfThreads) {
                threadList.add(thread {
                    val startIndex = currentThread * subListSize
                    val maxIndex = minOf(startIndex + subListSize - 1, list.size - 1)
                    val localResult = list
                        .slice(startIndex..maxIndex)
                        .all(predicate)
                    if (!localResult) {
                        result = false
                        return@thread
                    }
                    //Thread collision is not a problem since result can only change from true to false once
                })
            }
            threadList.forEach { it.join() }
            return result
        }
    }
    return MyProc(numberOfThreads)
}

/**
 * Basic task: implement parallel processing of a list of elements, with the same semantics of the operations as the
 * standard library's extensions.
 *
 * The implementation should use [numberOfThreads] threads when processing the list in order to reduce the latency.
 *
 * It is also safe to assume that the list is not modified concurrently when the operations are running.
 *
 * You should not assume that the comparators and functions passed to the operations work fast.
 *
 * In this task, you are not allowed to use any declarations from the `java.util.concurrent` package nor any other
 * high-level concurrency utilities from any libraries. The goal is to play with low-level primitives.
 */
interface ParallelProcessor {
    val numberOfThreads: Int

    fun <T : Any> minWithOrNull(list: List<T>, comparator: Comparator<T>): T?
    fun <T : Any> maxWithOrNull(list: List<T>, comparator: Comparator<T>): T? =
        minWithOrNull(list, comparator.reversed())

    fun <T> all(list: List<T>, predicate: (item: T) -> Boolean): Boolean
    fun <T> any(list: List<T>, predicate: (item: T) -> Boolean): Boolean =
        !all(list) { !predicate(it) }
}

fun <T : Comparable<T>> ParallelProcessor.minWithOrNull(list: List<T>) = this.minWithOrNull(list, naturalOrder())
fun <T : Comparable<T>> ParallelProcessor.maxWithOrNull(list: List<T>) = this.maxWithOrNull(list, naturalOrder())

/**
 * Advanced task: implement additional operations.
 */
interface AdvancedParallelProcessor : ParallelProcessor {
    fun <T> filter(list: List<T>, predicate: (T) -> Boolean): List<T>
    fun <T, R> map(list: List<T>, function: (T) -> R): List<R>
    fun <T> joinToString(list: List<T>, separator: String): String
}