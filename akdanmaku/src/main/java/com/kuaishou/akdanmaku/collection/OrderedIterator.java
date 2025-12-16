/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Kwai, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.kuaishou.akdanmaku.collection;

import java.util.Iterator;

/**
 * Defines an iterator that operates over an ordered container. Subset of {@link java.util.ListIterator}.
 * <p>
 * This iterator allows both forward and reverse iteration through the container.
 * </p>
 *
 * @param <E> the type of elements returned by this iterator.
 * @since 3.0
 */
public interface OrderedIterator<E> extends Iterator<E> {

    /**
     * Checks to see if there is a previous element that can be iterated to.
     *
     * @return {@code true} if the iterator has a previous element
     */
    boolean hasPrevious();

    /**
     * Gets the previous element from the container.
     *
     * @return the previous element in the iteration
     * @throws java.util.NoSuchElementException if the iteration is finished
     */
    E previous();

}
