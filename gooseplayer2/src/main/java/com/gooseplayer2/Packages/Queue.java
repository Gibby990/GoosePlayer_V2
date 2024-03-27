package com.gooseplayer2.Packages;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Queue<E> implements Iterable<E> {
    private E[] Queue;
    private int front=0;
    private int rear=-1;
    private int size=0;

    @SuppressWarnings("unchecked")
    public Queue() {
        Queue = (E[])new Object[10];
    }

    public void enqueue (E item) {
        if (size == Queue.length) reallocate();
        rear = (rear + 1) % Queue.length;
        Queue[rear] = item;
        size++;
    }

    public E dequeue() throws NoSuchElementException {
        if (size == 0) throw new NoSuchElementException();
        E item = Queue[front];
        front = (front + 1) % Queue.length;
        size--;
        return item;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    private void reallocate() {
        @SuppressWarnings("unchecked")
        E[] newQueue = (E[]) new Object[Queue.length * 2];
        for (int i = 0; i < size; i++) {
            int j = (front + i) % Queue.length;
            newQueue[i] = Queue[j];
        }
        Queue = newQueue;
        front = 0;
        rear = size - 1;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private int current = front;
            private int count = 0;

            @Override
            public boolean hasNext() {
                return count < size;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                E item = Queue[current];
                current = (current + 1) % Queue.length;
                count++;
                return item;
            }
        };
    }
}