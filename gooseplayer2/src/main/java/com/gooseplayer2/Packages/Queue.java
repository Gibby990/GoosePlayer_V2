package com.gooseplayer2.Packages;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class Queue<E> implements Iterable<E> {
    private Node<E> front;
    private Node<E> rear;
    
    
    @SuppressWarnings("hiding")
    public class Node<E> {
        private E item = null;
        private Node<E> next = null;
    }

    public void enqueue (E item) {
        Node<E> newNode = new Node<>();
        newNode.item = item;
        if (rear == null) {
            front = newNode;
        } else {
            rear.next = newNode;
        }
        rear = newNode;
    }

    public E dequeue() throws NoSuchElementException {
        if(front == null) throw new NoSuchElementException();
        E item = front.item;
        front = front.next;
        if(front == null) rear = null;
        return item;
    }

    public boolean isEmpty() {
        return front == null;
    }

    public E peek() {
        return front.item;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private Node<E> current = front;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                E item = current.item;
                current = current.next;
                return item;
            }
        };
    }
}