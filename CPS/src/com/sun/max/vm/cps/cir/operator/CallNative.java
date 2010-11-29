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
package com.sun.max.vm.cps.cir.operator;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.cps.b.c.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.type.*;

public class CallNative extends JavaOperator {
    private final SignatureDescriptor signatureDescriptor;
    private final ClassMethodActor classMethodActor;

    public CallNative(ConstantPool constantPool, int nativeFunctionDescriptorIndex, ClassMethodActor classMethodActor) {
        super(CALL_STOP);
        this.signatureDescriptor = SignatureDescriptor.create(constantPool.utf8At(nativeFunctionDescriptorIndex, "native function descriptor"));
        this.classMethodActor = classMethodActor;
    }

    public Kind resultKind() {
        return signatureDescriptor.resultKind();
    }

    @Override
    public void acceptVisitor(CirVisitor visitor) {
        visitor.visitHCirOperator(this);
    }

    @Override
    public void acceptVisitor(HCirOperatorVisitor visitor) {
        visitor.visit(this);
    }

    public SignatureDescriptor signatureDescriptor() {
        return signatureDescriptor;
    }

    public ClassMethodActor classMethodActor() {
        return classMethodActor;
    }

    @Override
    public Kind[] parameterKinds() {
        Kind[] kinds = new Kind[signatureDescriptor.numberOfParameters() + 1];
        signatureDescriptor.copyParameterKinds(kinds, 0);
        return kinds;
    }

    @Override
    public String toString() {
        return "CallNative:" + classMethodActor.name;
    }
}
