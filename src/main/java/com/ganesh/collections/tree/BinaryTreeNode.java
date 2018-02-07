package com.ganesh.collections.tree;

public class BinaryTreeNode<T> {

    private BinaryTreeNode<T> left;
    private BinaryTreeNode<T> right;
    private T data;

    public BinaryTreeNode(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }

    public BinaryTreeNode<T> getLeft() {
        return left;
    }

    public void setLeft(BinaryTreeNode<T> left) {
        this.left = left;
    }

    public BinaryTreeNode<T> getRight() {
        return right;
    }

    public void setRight(BinaryTreeNode<T> right) {
        this.right = right;
    }

}
