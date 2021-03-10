package com.h0tk3y.spbsu.parallel

import java.util.concurrent.ConcurrentSkipListSet
import kotlin.concurrent.thread

/**
 * Return your implementation of [ParallelProcessor]. To enroll for the advanced task, make it implement
 * [AdvancedParallelProcessor] as well.
 */



fun solution(numberOfThreads: Int): ParallelProcessor {
    class MyProc(override val numberOfThreads: Int) : AdvancedParallelProcessor {

        inner class Counter {
            @Volatile
            var currentIndex = 0

            @Synchronized fun getAndIncrementCounter(increment: Int): Int {
                currentIndex+=increment
                return currentIndex-increment
            }
        }

        inner class IterationInfo(val currentThread: Int, val index: Int, val counter: Counter)

        private fun <T, R> withThreadQueue(list: List<T>, block: T.(IterationInfo) -> R) {
            val counter = Counter()
            val threadList = mutableListOf<Thread>()
            for (currentThread in 0 until numberOfThreads) {
                threadList.add(thread {
                    while (true) {
                        val index = counter.getAndIncrementCounter(1)
                        if (index >= list.size)
                            return@thread
                        list[index].block(IterationInfo(currentThread, index, counter))
                    }
                })
            }
            threadList.forEach { it.join() }
        }

        override fun <T> filter(list: List<T>, predicate: (T) -> Boolean): List<T> {
            if (list.isEmpty())
                return listOf()
            val localResults = MutableList(list.size) { false }
            withThreadQueue(list) {
                localResults[it.index] = predicate(this)
            }
            return list.filterIndexed { index, _ -> localResults[index] }
        }

        override fun <T, R> map(list: List<T>, function: (T) -> R): List<R> {
            if (list.isEmpty())
                return listOf()
            val localResults = MutableList<R?>(list.size) {null}
            withThreadQueue(list) {
                localResults[it.index] = function(this)
            }
            return localResults.map { it!! }
        }

        override fun <T> joinToString(list: List<T>, separator: String): String {
            val localResults = MutableList(list.size) {""}
            withThreadQueue(list) {
                localResults[it.index] = this.toString()
            }
            return localResults.joinToString(separator)
        }

        override fun <T : Any> minWithOrNull(list: List<T>, comparator: Comparator<T>): T? {
            if (list.isEmpty()) return null
            val resultList = MutableList(numberOfThreads) {list[0]}
            withThreadQueue(list) {
                resultList[it.currentThread] = minOf(this, resultList[it.currentThread], comparator)
            }
            return resultList.minWithOrNull(comparator)
        }

        override fun <T> all(list: List<T>, predicate: (item: T) -> Boolean): Boolean {
            var result = true
            withThreadQueue(list) {
                if (!predicate(this)) {
                    result = false
                    it.counter.getAndIncrementCounter(list.size) //All threads will exit on next iteration
                }
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