/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.asm.target.armv7;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.asm.target.armv7.ARMV7Assembler.*;

public class ARMV7Label extends Label {

    public static class BranchInfo {


        public  enum BranchType { JCC, JMP, TABLESWITCH, BRANCH, UNKNOWN; }

        private BranchType type;
        private ConditionFlag flag;
        private boolean instrumented;

        public BranchInfo(BranchType type, ConditionFlag flag, boolean instrumented) {
            this.type = type;
            this.flag = flag;
            this.instrumented = instrumented;
        }

        public BranchType getBranchType() {
            return type;
        }

        public ConditionFlag getConditionFlag() {
            return flag;
        }

        public static BranchType fromValue(int value) {
            switch (value) {
                case 0xbeef:
                    return BranchType.JCC;
                case 0xdead:
                    return BranchType.JMP;
                case 0xd0d0:
                    return BranchType.BRANCH;
                default:
                    return BranchType.TABLESWITCH;
            }
        }

        public boolean isInstrumented() {
            return instrumented;
        }

    }

    public ARMV7Label() {

    }

    public ARMV7Label(Label label) {
        super(label.getPatchPositions(), label.positionCopy());
    }

    private ArrayList<BranchInfo> branchInfo = new ArrayList<BranchInfo>(4);

    public void addPatchAt(int branchLocation, BranchInfo type) {
        assert !isBound() : "Label is already bound";
        addPatchAt(branchLocation);
        branchInfo.add(type);
    }

    protected void patchInstructions(ARMV7Assembler masm) {
        assert isBound() : "Label should be bound";
        int target = position;
        for (int i = 0; i < patchPositions.size(); ++i) {
            int pos = patchPositions.get(i);
            masm.patchJumpTarget(pos, target, branchInfo.get(i));
        }
    }
}
