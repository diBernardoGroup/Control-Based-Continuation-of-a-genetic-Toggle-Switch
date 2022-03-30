package bControl;

import java.util.Vector;

import bBacterium.BSimControlledBacterium;


//Inteface every controller must implement (Error computation, Control computation and update of the reference)
public interface Controller {

	public double Evaluate_Control(double [] e);								//Error computation method
	
	public double[] Evaluate_Error(Vector<BSimControlledBacterium> bacteria);	//Control computation method
	
	public void setRef(double ref);												//Setter for the reference 
	
	public double getRef();														//Getter for the reference 
	
}
