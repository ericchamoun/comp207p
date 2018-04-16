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
		
		for(InstructionHandle handler : instList.getInstructionHandles()){
			
			if(handler.getInstruction() instanceof ArithmeticInstruction){
				success = this.arithmeticExpression(instList, handler);
			}
			else if(handler.getInstruction() instanceof IfInstruction){
				//success = this.ifStatements(instList, handler);
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
		
		if(prev2Instruction instanceof LDC){
			LDC ldc = (LDC) prev2Instruction;
			if(ldc.getValue(constantGen) instanceof Number){
				num1 = (Number) value;
			}
		}
		else if(prev2Instruction instanceof LDC2_W){
			LDC2 ldc2 = (LCD2_W) prev2Instruction;
			Type temp = lcd2.getType(constantGen);
			if(temp == TYPE.INT || temp == TYPE.FLOAT || temp == TYPE.LONG || temp == TYPE.DOUBLE){
				num1 = lcd2.getValue(constantGen);
			}
		}
		else if(prev2Instruction instanceof ConstantPushInstruction){
			ConstantPushInstruction cpi = (ConstantPushInstruction) prev2Instruction;
			num1 = cpi.getValue();
		}
		else{
			return 0;
		}
		
		
		if(prevInstruction instanceof LDC){
			LDC ldc = (LDC) prevInstruction;
			if(ldc.getValue(constantGen) instanceof Number){
				num2 = (Number) value;
			}
		}
		else if(prevInstruction instanceof LDC2_W){
			LDC2 ldc2 = (LCD2_W) prevInstruction;
			Type temp = lcd2.getType(constantGen);
			if(temp == TYPE.INT || temp == TYPE.FLOAT || temp == TYPE.LONG || temp == TYPE.DOUBLE){
				num2 = lcd2.getValue(constantGen);
			}
		}
		else if(prevInstruction instanceof ConstantPushInstruction){
			ConstantPushInstruction cpi = (ConstantPushInstruction) prevInstruction;
			num2 = cpi.getValue();
		}
		else{
			return 0;
		}
		
		int pos = this.binaryExpressions(num1,num2,type,instruction);
		
		Instruction newInstruction;
		
		if(type.equals(TYPE.INT) || type.equals(TYPE.FLOAT)){
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
			return binaryOperations(num1,num2,type,'0')
		}
		else if(operation.equals("ISUB") || operation.equals("FSUB") || operation.equals("LSUB") || operation.equals("DSUB")){
			return binaryOperations(num1,num2,type,'1')
		}
		else if(operation.equals("IMUL") || operation.equals("FMUL") || operation.equals("LMUL") || operation.equals("DMUL")){
			return binaryOperations(num1,num2,type,'2')
		}
		else if(operation.equals("IDIV") || operation.equals("FDIV") || operation.equals("LDIV") || operation.equals("DDIV")){
			return binaryOperations(num1,num2,type,'3')
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
							
			}
			
		}
		else if(type.equals(TYPE.FLOAT)){
			
			switch(action){
				
				case '0':	return this.constantGen.addFloat((float)num1 + (float)num2);
							break;
				case '1':	return this.constantGen.addFloat((float)num1 - (float)num2);
							break;
				case '2':	return this.constantGen.addFloat((float)num1 * (float)num2);
							break;
				case '3':	return this.constantGen.addFloat((float)num1 / (float)num2);
							break;
				
			}
			
		}
		else if(type.equals(TYPE.LONG)){
			
			switch(action){
				
				case '0':	return this.constantGen.addLong((long)num1 + (long)num2);
							break;
				case '1':	return this.constantGen.addLong((long)num1 - (long)num2);
							break;
				case '2':	return this.constantGen.addLong((long)num1 * (long)num2);
							break;
				case '3':	return this.constantGen.addLong((long)num1 / (long)num2);
							break;			
				
			}
			
		}
		else if(type.equals(TYPE.DOUBLE)){
			
			switch(action){
				
				case '0': 	return this.constantGen.addDouble((double)num1 + (double)num2);
							break;
				case '1': 	return this.constantGen.addDouble((double)num1 + (double)num2);
							break;
				case '2': 	return this.constantGen.addDouble((double)num1 + (double)num2);
							break;
				case '3': 	return this.constantGen.addDouble((double)num1 + (double)num2);
							break;
							
			}
			
		}
		else{
			return 0;
		}
		
	}
	
	/*private int ifStatements(InstructionList instList, InstructionHandle handler){
		
		IfInstruction ifStatement = (IfInstruction) handler.getInstruction();
		String name = ifStatement.getClass().getSimpleName();
		
		Type type1, type2 = null;
		
		InstructionHandle prevHandler = handler.getPrev();
		Instruction instruction = prevHandler.getInstruction();
		
		
		
		
	}*/
	
}
