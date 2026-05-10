package com.example.wanotification.queue

import java.util.LinkedList
import java.util.Queue

object SpeechQueueManager {

    private val queue: Queue<String> =
        LinkedList()

    fun enqueue(
        text: String
    ) {

        queue.add(text)
    }

    fun dequeue(): String? {

        return queue.poll()
    }

    fun hasItems(): Boolean {

        return queue.isNotEmpty()
    }
}