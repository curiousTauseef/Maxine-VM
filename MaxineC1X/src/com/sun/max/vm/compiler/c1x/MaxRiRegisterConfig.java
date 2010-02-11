/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import java.util.*;

import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.amd64.*;

/**
 * This class implements the register configuration for Maxine supplied to the C1X compiler.
 *
 * @author Ben L. Titzer
 */
public class MaxRiRegisterConfig implements RiRegisterConfig {

    private final CiRegister safepointRegister;
    private final CiRegister stackPointerRegister;
    private final CiRegister scratchRegister;
    private final CiRegister returnRegisterInt;
    private final CiRegister returnRegisterFloat;
    private final CiRegister[] allocatableRegisters;
    private final CiRegister[] registerReferenceMapOrder;
    private final HashMap<CiRegister, Integer> calleeSaveOffset;
    private final CiRegister[] generalParameterRegisters;
    private final CiRegister[] xmmParameterRegisters;

    public MaxRiRegisterConfig(VMConfiguration vmConfiguration) {
        if (vmConfiguration.platform.instructionSet() != InstructionSet.AMD64) {
            FatalError.unimplemented();
        }
        InstructionSet isa = vmConfiguration.platform().processorKind.instructionSet;
        CiArchitecture arch = CiArchitecture.findArchitecture(isa.name().toLowerCase());

        // get the unallocatable registers
        Set<String> unallocatable = new HashSet<String>();
        HashMap<String, CiRegister> regMap = buildRegisterMap(arch);
        calleeSaveOffset = new HashMap<CiRegister, Integer>();

        // set up well known registers
        TargetABI abi = vmConfiguration.targetABIsScheme().optimizedJavaABI;
        Class<TargetABI<AMD64GeneralRegister64, AMD64XMMRegister>> type = null;
        TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> amd64Abi = StaticLoophole.cast(type, abi);

        RegisterRoleAssignment roles = abi.registerRoleAssignment;
        safepointRegister = markUnallocatable(unallocatable, regMap, VMRegister.Role.SAFEPOINT_LATCH, roles);
        stackPointerRegister = markUnallocatable(unallocatable, regMap, VMRegister.Role.CPU_STACK_POINTER, roles);
        markUnallocatable(unallocatable, regMap, VMRegister.Role.CPU_FRAME_POINTER, roles);
        scratchRegister = markUnallocatable(unallocatable, regMap, VMRegister.Role.ABI_SCRATCH, roles);
        markUnallocatable(unallocatable, regMap, VMRegister.Role.LITERAL_BASE_POINTER, roles);
        returnRegisterInt = regMap.get(roles.integerRegisterActingAs(VMRegister.Role.ABI_RETURN).name().toLowerCase());
        returnRegisterFloat = regMap.get(roles.floatingPointRegisterActingAs(VMRegister.Role.ABI_RETURN).name().toLowerCase());

        assert safepointRegister != null;
        assert stackPointerRegister != null;
        assert scratchRegister != null;
        assert returnRegisterInt != null;
        assert returnRegisterFloat != null;

        // allocate the array which indicates the reference map order
        registerReferenceMapOrder = new CiRegister[AMD64GeneralRegister64.ENUMERATOR.length()];
        // configure the allocatable registers
        List<CiRegister> allocatable = new ArrayList<CiRegister>(arch.registers.length);
        int index = 0;
        int offset = 0;
        for (AMD64GeneralRegister64 reg : AMD64GeneralRegister64.ENUMERATOR) {
            CiRegister r = regMap.get(reg.name().toLowerCase());
            if (!unallocatable.contains(r.name.toLowerCase())) {
                allocatable.add(r);
            }
            registerReferenceMapOrder[index++] = r;
            calleeSaveOffset.put(r, offset);
            offset += arch.wordSize;
        }

        // setup callee saving locations for xmm registers
        for (AMD64XMMRegister xmm : AMD64XMMRegister.ENUMERATOR) {
            CiRegister r = regMap.get(xmm.name().toLowerCase());
            if (!unallocatable.contains(r.name.toLowerCase())) {
                allocatable.add(r);
            }
            calleeSaveOffset.put(r, offset);
            offset += 2 * arch.wordSize;
        }
        allocatableRegisters = allocatable.toArray(new CiRegister[allocatable.size()]);

        List<CiRegister> generalList = new ArrayList<CiRegister>();
        List<CiRegister> xmmList = new ArrayList<CiRegister>();

        // add the general parameters to the parameter list
        for (AMD64GeneralRegister64 reg : amd64Abi.integerIncomingParameterRegisters) {
            generalList.add(regMap.get(reg.name().toLowerCase()));
        }
        // add the floating parameters to the parameter list
        for (AMD64XMMRegister xmm : amd64Abi.floatingPointParameterRegisters) {
            xmmList.add(regMap.get(xmm.name().toLowerCase()));
        }

        generalParameterRegisters = generalList.toArray(new CiRegister[generalList.size()]);
        xmmParameterRegisters = xmmList.toArray(new CiRegister[xmmList.size()]);
    }

    public CiRegister getReturnRegister(CiKind kind) {
        if (kind == CiKind.Float || kind == CiKind.Double) {
            return returnRegisterFloat;
        }
        if (kind == CiKind.Void || kind == CiKind.Illegal) {
            return CiRegister.None;
        }
        return returnRegisterInt;
    }

    public CiRegister getStackPointerRegister() {
        return stackPointerRegister;
    }

    public CiRegister getScratchRegister() {
        return scratchRegister;
    }

    public CiRegister getSafepointRegister() {
        return safepointRegister;
    }

    public CiRegister getThreadRegister() {
        return safepointRegister;
    }

    public CiLocation[] getJavaParameterLocations(CiKind[] types, boolean outgoing) {
        return callingConvention(types, outgoing);
    }

    public CiLocation[] getRuntimeParameterLocations(CiKind[] types) {
        return callingConvention(types, true);
    }

    public CiRegister[] getAllocatableRegisters() {
        return allocatableRegisters;
    }

    public CiRegister[] getCallerSaveRegisters() {
        return allocatableRegisters;
    }

    public int getMinimumCalleeSaveFrameSize() {
        return AMD64TrapStateAccess.TRAP_STATE_SIZE_WITHOUT_RIP;
    }

    public int getCalleeSaveRegisterOffset(CiRegister register) {
        Integer i = calleeSaveOffset.get(register);
        if (i != null) {
            return i;
        }
        return -1;
    }

    public CiRegister[] getRegisterReferenceMapOrder() {
        return registerReferenceMapOrder;
    }

    public CiLocation[] callingConvention(CiKind[] types, boolean outgoing) {
        CiLocation[] result = new CiLocation[types.length];

        int currentGeneral = 0;
        int currentXMM = 0;
        int currentStackSlot = 0;
        final int wordSize = Word.size();

        for (int i = 0; i < types.length; i++) {
            final CiKind kind = types[i];

            switch (kind) {
                case Byte:
                case Boolean:
                case Short:
                case Char:
                case Int:
                case Long:
                case Word:
                case Object:
                    if (currentGeneral < generalParameterRegisters.length) {
                        CiRegister register = generalParameterRegisters[currentGeneral++];
                        if (kind == CiKind.Long) {
                            result[i] = new CiRegisterLocation(kind, register, register);
                        } else {
                            result[i] = new CiRegisterLocation(kind, register);
                        }
                    }
                    break;

                case Float:
                case Double:
                    if (currentXMM < xmmParameterRegisters.length) {
                        CiRegister register = xmmParameterRegisters[currentXMM++];
                        if (kind == CiKind.Float) {
                            result[i] = new CiRegisterLocation(kind, register);
                        } else {
                            result[i] = new CiRegisterLocation(kind, register, register);
                        }
                    }
                    break;

                default:
                    throw Util.shouldNotReachHere();
            }

            if (result[i] == null) {
                // TODO: this is probably not 32-bit safe.
                result[i] = new CiStackLocation(kind, currentStackSlot, wordSize, !outgoing);
                currentStackSlot += wordSize;
            }
        }

        return result;
    }

    private HashMap<String, CiRegister> buildRegisterMap(CiArchitecture arch) {
        HashMap<String, CiRegister> regMap = new HashMap<String, CiRegister>();
        for (CiRegister r : arch.registers) {
            regMap.put(r.name.toLowerCase(), r);
        }
        return regMap;
    }

    private CiRegister markUnallocatable(Set<String> unallocatable, HashMap<String, CiRegister> map, VMRegister.Role register, RegisterRoleAssignment roles) {
        Symbol intReg = roles.integerRegisterActingAs(register);
        if (intReg != null) {
            unallocatable.add(intReg.name().toLowerCase());
            return map.get(intReg.name().toLowerCase());
        }
        Symbol floatReg = roles.floatingPointRegisterActingAs(register);
        if (floatReg != null) {
            unallocatable.add(floatReg.name().toLowerCase());
            return map.get(floatReg.name().toLowerCase());
        }
        return null;
    }

}