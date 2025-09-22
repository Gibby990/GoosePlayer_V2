package com.gooseplayer2.Packages;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.lang.IndexOutOfBoundsException;

public class Queue<E> implements Iterable<E> {
    private Node<E> front;
    private Node<E> rear;
    private ArrayList<E> history = new ArrayList<>();
    

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
        history.add(item);
        return item;
    }

    public boolean isEmpty() {
        return front == null;
    }

    public E peek() {
        return (front == null) ? null : front.item;
    }

    public void empty() {
        front = null;
        rear = null;
    }

    public java.util.List<E> getHistory() {
        return new ArrayList<>(history);
    }

    public void clearHistory() {
        history.clear();
    }

    public boolean removeHistoryAt(int index) {
        if (index < 0 || index >= history.size()) return false;
        history.remove(index);
        return true;
    }

    public boolean remove(E item) {
        if (front == null ) return false;
        if (front.item.equals(item)) return false;
    
        Node<E> current = front;
        while (current.next != null) {
            if (current.next.item.equals(item)) {
                current.next = current.next.next;
                if (current.next == null) {
                    rear = current; 
                }
                return true; 
            }
            current = current.next;
        }
    
        return false; 
    }
    
    public int size() {
        int count = 0;
        Node<E> current = front;
        while (current != null) {
            count++;
            current = current.next;
        }
        return count;
    }
    
    // Does not apply to the current song
    public void shuffle() {
        if (front == null || front.next == null) return;
        E first = front.item;
        ArrayList<E> rest = new ArrayList<>();
        Node<E> current = front.next;
        while (current != null) {
            rest.add(current.item);
            current = current.next;
        }
        if (rest.size() <= 1) return;
        Collections.shuffle(rest);
        front = null;
        rear = null;
        enqueue(first);
        for (E item : rest) {
            enqueue(item);
        }
    }
    
    public E get(int index) {
        if (index < 0 || isEmpty()) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }
        
        Node<E> current = front;
        for (int i = 0; i < index; i++) {
            if (current.next == null) {
                throw new IndexOutOfBoundsException("Index: " + index);
            }
            current = current.next;
        }
        return current.item;
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