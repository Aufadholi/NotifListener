package com.example.wanotification.queue

import java.util.LinkedList
import java.util.Queue

object SpeechQueueManager {

    private val queue: Queue<String> =
        LinkedList()

    @Synchronized
    fun enqueue(
        text: String
    ) {

        if (text.isBlank()) {
            return
        }

        queue.add(text)
    }

    @Synchronized
    fun dequeue(): String? {

        return queue.poll()
    }

    @Synchronized
    fun hasItems(): Boolean {

        return queue.isNotEmpty()
    }

    @Synchronized
    fun clear() {

        queue.clear()
    }

    @Synchronized
    fun drain(): List<String> {

        val items = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            queue.poll()?.let { items.add(it) }
        }

        return items
    }
}