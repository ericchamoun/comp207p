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
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;



public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void optimizeMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method)
	{
		//Get the code of method - collection of byte code
		Code methodCode = method.getCode();
		
		//Now get actual bytecode data in bytearray
		//Initialise instruction List
		InstructionList instList = new InstructionList(methodCode.getCOde());

		// Initialise a method generator with the original method as the baseline	
		MethodGen methodGen = new MethodGen(method.getAccessFlags(), method.getReturnType(), method.getArgumentTypes(), null, method.getName(), cgen.getClassName(), instList, cpgen);

		// InstructionHandle is a wrapper for actual Instructions
		for (InstructionHandle handle : instList.getInstructionHandles())
		{
			// if the instruction inside is iconst
			if (handle.getInstruction() instanceof ICONST)
			{
				// insert new one with integer 5, and...
				instList.insert(handle, new ICONST(5));
				try
				{
					// delete the old one
					instList.delete(handle);

				}
				catch (TargetLostException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// setPositions(true) checks whether jump handles 
		// are all within the current method
		instList.setPositions(true);

		// set max stack/local
		methodGen.setMaxStack();
		methodGen.setMaxLocals();

		// generate the new method with replaced iconst
		Method newMethod = methodGen.getMethod();
		// replace the method in the original class
		cgen.replaceMethod(method, newMethod);

	}
	
	
	public void optimize()
	{
		//loads original class into a class generator
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Implement your optimization here
		
		//get the current constant pool
        	ConstantPool cp = cpgen.getConstantPool();
		// get the constants in the pool
		Constant[] constants = cp.getConstantPool();
		
		Method[] methods = cgen.getMethods();
		for(Method m : methods)
		{
			optimizeMethod(cgen, cpgen, m);
		}
		
		this.optimized = gen.getJavaClass();
	}

	
	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}
