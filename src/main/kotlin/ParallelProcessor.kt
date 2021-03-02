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

        private fun subListSize(listSize: Int) =
            (listSize+numberOfThreads-1) / numberOfThreads

        //Number of threads that process at least one element. Important when list size is small
        private fun effectiveNumberOfThreads(listSize: Int) =
            (listSize+subListSize(listSize)-1) / (subListSize(listSize))

        private fun <T, R> withSlices(list: List<T>, block: List<T>.(Int) -> R) {
            val subListSize = subListSize(list.size)
            val effectiveNumberOfThreads = effectiveNumberOfThreads(list.size)
            val threadList = mutableListOf<Thread>()
            for (currentThread in 0 until effectiveNumberOfThreads) {
                threadList.add(thread {
                    val startIndex = currentThread * subListSize
                    val maxIndex = minOf(startIndex + subListSize - 1, list.size - 1)
                    list
                        .slice(startIndex..maxIndex)
                        .block(currentThread)
                })
                if (currentThread * subListSize + subListSize >= list.size)
                    break
            }
            threadList.forEach { it.join() }
        }

        override fun <T> filter(list: List<T>, predicate: (T) -> Boolean): List<T> {
            if (list.isEmpty())
                return listOf()
            val localResults = MutableList(effectiveNumberOfThreads(list.size)) { listOf<T>()}
            withSlices(list) { currentThread ->
                localResults[currentThread] = this.filter(predicate)
            }
            return localResults.flatten()
        }

        override fun <T, R> map(list: List<T>, function: (T) -> R): List<R> {
            if (list.isEmpty())
                return listOf()
            val localResults = MutableList(effectiveNumberOfThreads(list.size)) { listOf<R>()}
            withSlices(list) { currentThread ->
                localResults[currentThread] = this.map(function)
            }
            return localResults.flatten()
        }

        override fun <T> joinToString(list: List<T>, separator: String): String {
            val localResults = MutableList(effectiveNumberOfThreads(list.size)) {""}
            withSlices(list) { currentThread ->
                localResults[currentThread] = this.joinToString(separator)
            }
            return localResults.joinToString(separator)
        }

        override fun <T : Any> minWithOrNull(list: List<T>, comparator: Comparator<T>): T? {
            val localResults = MutableList(numberOfThreads) {list[0]}
            withSlices(list) { currentThread ->
                this
                    .minWithOrNull(comparator)
                    ?.let { localResults[currentThread] = minOf(it, localResults[currentThread], comparator) }
            }
            return localResults.minWithOrNull(comparator)
        }

        override fun <T> all(list: List<T>, predicate: (item: T) -> Boolean): Boolean {
            var result = true
            withSlices(list) {
                this
                    .all(predicate)
                    .let { if (!it) result = false }
            }
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