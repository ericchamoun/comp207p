package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.ConversionInstruction;

//import tokens
import org.apache.bcel.generic.DADD;
import org.apache.bcel.generic.DCMPG;
import org.apache.bcel.generic.DCMPG;
import org.apache.bcel.generic.FCMPG;
import org.apache.bcel.generic.FCMPL;
import org.apache.bcel.generic.FADD;
import org.apache.bcel.generic.FCMPG;
import org.apache.bcel.generic.FCMPL;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.IADD;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LADD;
import org.apache.bcel.generic.LCMP;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;

import javax.rmi.CORBA.Util;

public class SimpleFolding{

	ClassParser parser = null;
	ClassGen gen = null;

	ConstantPoolGen constantGen;

	public SimpleFolding(ClassGen gen, ConstantPoolGen constantGen){
		this.gen = gen;
		this.constantGen = constantGen;
	}

	public Method optimiseMethod(Method method, MethodGen mg, InstructionList instList) throws TargetLostException{

		int success = 0;
		//Simplify arithmetic expressions and any casting expressions.
		for(InstructionHandle handler : instList.getInstructionHandles()){

			if(handler.getInstruction() instanceof ArithmeticInstruction){
				success = this.arithmeticExpression(instList, handler);
			}
			else if(handler.getInstruction() instanceof ConversionInstruction){
				success = this.conversionExpression(instList, handler);
			}
		}

		if(success == 1){
			System.out.println("Simple folding done successfully");
			return mg.getMethod();
		}
		return null;

	}
	private int arithmeticExpression(InstructionList instList, InstructionHandle handler){

		ArithmeticInstruction instruction = (ArithmeticInstruction) handler.getInstruction();

		Type type = instruction.getType(this.constantGen);

		//Need previous two instructions to get numbers.

		InstructionHandle prevHandler = instruction.getPrev();
		Instruction prevInstruction = prevHandler.getInstruction();

		InstructionHandle prev2Handler = prevHandler.getPrev();
		Instruction prev2Instruction = prev2Handler.getInstruction();

		//Get Values of numbers.
		Number num1, num2 = null;

		//Get Value1
		if(prev2Instruction instanceof LDC){
			LDC ldc = (LDC) prev2Instruction;
			if(ldc.getValue(constantGen) instanceof Number){
				num1 = (Number) value;
			}
		}
		else if(prev2Instruction instanceof LDC2_W){
			LDC2 ldc2 = (LCD2_W) prev2Instruction;
			Type temp = lcd2.getType(constantGen);
			if(temp == Type.INT || temp == Type.FLOAT || temp == Type.LONG || temp == Type.DOUBLE){
				num1 = lcd2.getValue(constantGen);
			}
		}
		else if(prev2Instruction instanceof ConstantPushInstruction){
			ConstantPushInstruction cpi = (ConstantPushInstruction) prev2Instruction;
			num1 = cpi.getValue();
		}

		//Get value2
		if(prevInstruction instanceof LDC){
			LDC ldc = (LDC) prevInstruction;
			if(ldc.getValue(constantGen) instanceof Number){
				num2 = (Number) value;
			}
		}
		else if(prevInstruction instanceof LDC2_W){
			LDC2 ldc2 = (LCD2_W) prevInstruction;
			Type temp = lcd2.getType(constantGen);
			if(temp == Type.INT || temp == Type.FLOAT || temp == Type.LONG || temp == Type.DOUBLE){
				num2 = lcd2.getValue(constantGen);
			}
		}
		else if(prevInstruction instanceof ConstantPushInstruction){
			ConstantPushInstruction cpi = (ConstantPushInstruction) prevInstruction;
			num2 = cpi.getValue();
		}

		int pos;
		//Unary Operation
		if(num1 == null && num2 != null){

			if(instruction.getClass().getSimpleName.equals("INEG") || instruction.getClass().getSimpleName.equals("FNEG") || instruction.getClass().getSimpleName.equals("LNEG") || instruction.getClass().getSimpleName.equals("DNEG")){
				if(type.equals(Type.INT)){
					pos = this.constantGen.addInteger((int)num2 * -1);
				}
				else if(type.equals(Type.FLOAT)){
					pos = this.constantGen.addFloat((float)num2 * -1);
				}
				else if(type.equals(Type.LONG)){
					pos = this.constantGen.addLong((long)num2 * -1);
				}
				else if(type.equals(Type.DOUBLE)){
					pos = this.constantGen.addDouble((double)num2 * -1);
				}
				else{
					return 0;
				}
			}
			else{
				return 0;
			}

		}//Incorrect type or operation.
		else if(num1 == null && num2 == null){
			return 0;
		}//Binary Operation with two values.
		else{
			pos = this.binaryExpressions(num1,num2,type,instruction);
		}
		Instruction newInstruction;

		if(type.equals(Type.INT) || type.equals(Type.FLOAT)){
			newInstruction = new LDC(pos);
		}
		else{
			newInstruction = new LDC2_W(pos);
		}

		InstructionHandle newHandler = instList.insert(handler, newInstruction);
		instList.delete(handler);
		instList.setPositions(true);
		instList.delete(prevHandler);
		instList.setPositions(true);
		instList.delete(prev2Handler);
		instList.setPositions(true);

		return 1;

	}

	private int binaryExpressions(Number num1, Number num2, Type type, ArithmeticInstruction aInstruction){

		String operation = aInstruction.getClass().getSimpleName();

		if(operation.equals("IADD") || operation.equals("FADD") || operation.equals("LADD") || operation.equals("DADD")) {
			return binaryOperations(num1,num2,type,'0');
		}
		else if(operation.equals("ISUB") || operation.equals("FSUB") || operation.equals("LSUB") || operation.equals("DSUB")){
			return binaryOperations(num1,num2,type,'1');
		}
		else if(operation.equals("IMUL") || operation.equals("FMUL") || operation.equals("LMUL") || operation.equals("DMUL")){
			return binaryOperations(num1,num2,type,'2');
		}
		else if(operation.equals("IDIV") || operation.equals("FDIV") || operation.equals("LDIV") || operation.equals("DDIV")){
			return binaryOperations(num1,num2,type,'3');
		}
		else if(operation.equals("IREM") || operation.equals("FREM") || operation.equals("LREM") || operation.equals("DREM")){
			return binaryOperations(num1,num2,type,'4');
		}
	}

	private int binaryOperations(Number num1, Number num2, Type type, char action){

		if(type.equals(Type.INT)){

			switch(action){

				case '0': 	return this.constantGen.addInteger((int)num1 + (int)num2);
							break;
				case '1': 	return this.constantGen.addInteger((int)num1 - (int)num2);
							break;
				case '2': 	return this.constantGen.addInteger((int)num1 * (int)num2);
							break;
				case '3': 	return this.constantGen.addInteger((int)num1 / (int)num2);
							break;
				case '4': 	return this.constantGen.addInteger((int)num2 - (((int)num1 / (int)num2) * (int)num2));
							break;

			}

		}
		else if(type.equals(Type.FLOAT)){

			switch(action){

				case '0':	return this.constantGen.addFloat((float)num1 + (float)num2);
				case '1':	return this.constantGen.addFloat((float)num1 - (float)num2);
				case '2':	return this.constantGen.addFloat((float)num1 * (float)num2);
				case '3':	return this.constantGen.addFloat((float)num1 / (float)num2);
				case '4': 	return this.constantGen.addFloat((float)num2 - (((float)num1 / (float)num2) * (float)num2));
			}

		}
		else if(type.equals(Type.LONG)){

			switch(action){

				case '0':	return this.constantGen.addLong((long)num1 + (long)num2);
				case '1':	return this.constantGen.addLong((long)num1 - (long)num2);
				case '2':	return this.constantGen.addLong((long)num1 * (long)num2);
				case '3':	return this.constantGen.addLong((long)num1 / (long)num2);
				case '4': 	return this.constantGen.addLong((long)num2 - (((long)num1 / (long)num2) * (long)num2));
			}

		}
		else if(type.equals(Type.DOUBLE)){

			switch(action){

				case '0': 	return this.constantGen.addDouble((double)num1 + (double)num2);
				case '1': 	return this.constantGen.addDouble((double)num1 + (double)num2);
				case '2': 	return this.constantGen.addDouble((double)num1 + (double)num2);
				case '3': 	return this.constantGen.addDouble((double)num1 + (double)num2);
				case '4': 	return this.constantGen.addDouble((double)num2 - (((double)num1 / (double)num2) * (double)num2));
			}

		}
		else{
			return 0;
		}

	}

	private int conversionExpression(InstructionList instList, InstructionHandle handler){

		ConversionInstruction instruction = (ConversionInstruction) handler.getInstruction();

		Type type = instruction.getType(this.constantGen);

		//Only need previous instruction to get number.

		InstructionHandle prevHandler = instruction.getPrev();
		Instruction prevInstruction = prevHandler.getInstruction();


		//Get Values of numbers.
		Number num1 = null;

		//Get Value1
		if(prevInstruction instanceof LDC){
			LDC ldc = (LDC) prev2Instruction;
			if(ldc.getValue(constantGen) instanceof Number){
				num1 = (Number) value;
			}
		}
		else if(prevInstruction instanceof LDC2_W){
			LDC2_W ldc2 = (LCD2_W) prev2Instruction;
			Type temp = lcd2.getType(constantGen);
			if(temp == Type.INT || temp == Type.FLOAT || temp == Type.LONG || temp == Type.DOUBLE){
				num1 = lcd2.getValue(constantGen);
			}
		}
		else if(prevInstruction instanceof ConstantPushInstruction){
			ConstantPushInstruction cpi = (ConstantPushInstruction) prevInstruction;
			num1 = cpi.getValue();
		}
		else{
			return 0;
		}

		Instruction newInstruction;

		if(type.equals(Type.INT)){
			newInstruction = new LDC(constantGen.addInteger((int)num1));
		}
		else if(type.equals(Type.FLOAT)){
			newInstruction = new LDC(constantGen.addFloat((float)num1));
		}
		else if(type.equals(Type.LONG)){
			newInstruction = new LDC(constantGen.addInteger((long)num1));
		}
		else if(type.equals(Type.DOUBLE)){
			newInstruction = new LDC(constantGen.addInteger((double)num1));
		}
		else{
			return 0;
		}

		InstructionHandle newHandler = instList.insert(handler, newInstruction);
		instList.delete(handler);
		instList.setPositions(true);
		instList.delete(prevHandler);
		instList.setPositions(true);

		return 1;

	}
}
