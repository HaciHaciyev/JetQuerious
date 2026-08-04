package io.github.hacihaciyev.types.internal;

import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BytecodeTypeInterpreter {

    public sealed interface Event {
        record FieldRead(ClassDesc owner, String fieldName, ClassDesc fieldType) implements Event {}
    
        record FieldWrite(ClassDesc owner, String fieldName, ClassDesc fieldType) implements Event {}
    
        record MethodCall(ClassDesc owner, String methodName, MethodTypeDesc descriptor, List<SymbolicType> arguments) implements Event {}
    }

    private final Deque<SymbolicType> stack = new ArrayDeque<>();
    
    private final Map<Integer, SymbolicType> locals = new HashMap<>();
    
    private final List<Event> events = new ArrayList<>();

    public List<Event> run(List<CodeElement> code) {
        for (var element : code) {
            if (element instanceof Instruction instr) step(instr);
        }
        return List.copyOf(events);
    }

    private void step(Instruction instr) {
        switch (instr) {
            case ConstantInstruction ci           -> push(constantType(ci));
            case LoadInstruction li               -> push(locals.getOrDefault(li.slot(), SymbolicType.UNKNOWN));
            case StoreInstruction si              -> locals.put(si.slot(), pop());
            case FieldInstruction fi              -> handleField(fi);
            case NewObjectInstruction ni          -> push(new SymbolicType.Known(ni.className().asSymbol()));
            case NewReferenceArrayInstruction nai -> handleNewArray(nai.componentType().asSymbol());
            case ArrayStoreInstruction asi        -> handleArrayStore();
            case TypeCheckInstruction tci         -> handleCheckcast(tci);
            case InvokeInstruction ii             -> handleInvoke(ii);
            case ArrayLoadInstruction i           -> {}
            case BranchInstruction i              -> {}
            case ConvertInstruction i             -> {}
            case DiscontinuedInstruction i        -> {}
            case IncrementInstruction i           -> {}
            case InvokeDynamicInstruction i       -> {}
            case LookupSwitchInstruction i        -> {}
            case MonitorInstruction i             -> {}
            case NewMultiArrayInstruction i       -> {}
            case NewPrimitiveArrayInstruction i   -> {}
            case NopInstruction i                 -> {}
            case OperatorInstruction i            -> {}
            case ReturnInstruction i              -> {}
            case StackInstruction i               -> {}
            case TableSwitchInstruction i         -> {}
            case ThrowInstruction i               -> {}
        }
    }

    private SymbolicType constantType(ConstantInstruction ci) {
        var value = ci.constantValue();
        return switch (value) {
            case Integer i     -> new SymbolicType.KnownInt(ClassDesc.of("java.lang.Integer"), i);
            case Long l        -> new SymbolicType.Known(ClassDesc.of("java.lang.Long"));
            case Float f       -> new SymbolicType.Known(ClassDesc.of("java.lang.Float"));
            case Double d      -> new SymbolicType.Known(ClassDesc.of("java.lang.Double"));
            case String s      -> new SymbolicType.Known(ClassDesc.of("java.lang.String"));
            case null, default -> new SymbolicType.Known(ClassDesc.of("java.lang.Object"));
        };
    }

    private void handleField(FieldInstruction fi) {
        var fieldType = fi.typeSymbol();
        switch (fi.opcode()) {
            case GETSTATIC -> {
                var fr = new Event.FieldRead(fi.owner().asSymbol(), fi.name().stringValue(), fieldType);
                events.add(fr);
                push(new SymbolicType.FromField(fr.owner(), fr.fieldName(), fieldType));
            }
            case GETFIELD  -> {
                pop();
                push(new SymbolicType.Known(fieldType));
            }
            case PUTSTATIC -> {
                events.add(new Event.FieldWrite(fi.owner().asSymbol(), fi.name().stringValue(), fieldType));
                pop();
            }
            case PUTFIELD  -> { 
                pop();
                pop(); 
            }
            default        -> {}
        }
    }

    private void handleNewArray(ClassDesc elementType) {
        var sizeType = pop();
        var size = sizeType instanceof SymbolicType.KnownInt(var _, var v) ? v : 0;
        push(new SymbolicType.ArrayBuild(elementType, new SymbolicType[Math.max(size, 0)]));
    }

    private void handleArrayStore() {
        var value    = pop();
        var index    = pop();
        var arrayRef = pop();

        if (!(arrayRef instanceof SymbolicType.ArrayBuild build)) {
            push(arrayRef);
            return;
        }

        if (!(index instanceof SymbolicType.KnownInt(var _, var idx)) || !isValidSlot(idx, build)) {
            push(build);
            return;
        }

        build.elements()[idx] = value;
        push(build);
    }

    private static boolean isValidSlot(int idx, SymbolicType.ArrayBuild build) {
        return idx >= 0 && idx < build.elements().length;
    }

    private void handleCheckcast(TypeCheckInstruction tci) {
        pop();
        push(new SymbolicType.Known(tci.type().asSymbol()));
    }

    private void handleInvoke(InvokeInstruction ii) {
        var desc     = ii.typeSymbol();
        var argCount = desc.parameterCount();
        var args     = new ArrayList<SymbolicType>(argCount);

        for (int i = 0; i < argCount; i++) args.add(SymbolicType.UNKNOWN);
        for (int i = argCount - 1; i >= 0; i--) args.set(i, pop());
        if (ii.opcode() != Opcode.INVOKESTATIC) pop();

        events.add(new Event.MethodCall(ii.owner().asSymbol(), ii.name().stringValue(), desc, args));
        if (!desc.returnType().descriptorString().equals("V")) push(new SymbolicType.Known(desc.returnType()));
    }

    private void push(SymbolicType t) {
        stack.push(t);
    }

    private SymbolicType pop() {
        return stack.isEmpty() ? SymbolicType.UNKNOWN : stack.pop();
    }
}