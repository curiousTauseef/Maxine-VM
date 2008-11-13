/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.actor.holder;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.vm.*;

/**
 * @author Bernd Mathiske
 */
public final class ClassID {

    private ClassID() {
    }

    private static VariableSequence<ClassActor> _idToClassActor = new ArrayListSequence<ClassActor>();

    private static BitSet _usedIDs = new BitSet();

    /**
     * Inspector support.
     */
    @PROTOTYPE_ONLY
    public static interface Mapping {
        ClassActor idToClassActor(int id);
    }

    @PROTOTYPE_ONLY
    private static Mapping _mapping;

    @PROTOTYPE_ONLY
    public static void setMapping(Mapping mapping) {
        _mapping = mapping;
    }

    public static synchronized ClassActor toClassActor(int id) {
        try {
            if (MaxineVM.isPrototyping() && _mapping != null) {
                final ClassActor classActor = _mapping.idToClassActor(id);
                if (classActor != null) {
                    return classActor;
                }
            }
            return _idToClassActor.get(id);
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            return null;
        }
    }

    static synchronized int create() {
        final int id = _usedIDs.nextClearBit(0);
        if (id == _idToClassActor.length()) {
            _idToClassActor.append(null);
        }
        _usedIDs.set(id);
        return id;
    }

    static synchronized void register(int id, ClassActor classActor) {
        _idToClassActor.set(id, classActor);
    }

    static synchronized void clear(int id) {
        _idToClassActor.set(id, null);
        _usedIDs.clear(id);
    }
}
