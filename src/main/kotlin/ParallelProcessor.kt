package com.h0tk3y.spbsu.parallel

/**
 * Return your implementation of [ParallelProcessor]. To enroll for the advanced task, make it implement
 * [AdvancedParallelProcessor] as well.
 */
fun solution(numberOfThreads: Int): ParallelProcessor = TODO()

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