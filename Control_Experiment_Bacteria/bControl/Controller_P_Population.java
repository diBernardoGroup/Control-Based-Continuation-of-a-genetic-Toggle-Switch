package bControl;

import java.util.Vector;

import bBacterium.BSimControlledBacterium;

public class Controller_P_Population implements Controller {

	private double ref;
	private double k_p;
	
	public Controller_P_Population (double ref,double k_p) {
		this.ref=ref;
		this.k_p=k_p;
	}
	
	
	
	public double Evaluate_Control(double [] e) {
	
        double u_a=k_p*e[0];	
	
        if (u_a<0) {
        	u_a=0;
        }
        if (u_a>1) {
        	u_a=1;
        }
        
        
        return u_a;
	}
	
	
	public double[] Evaluate_Error(Vector<BSimControlledBacterium> bacteria) {
		
		
		// Each bacterium performs its action
		
		
		double [] errs=new double [2];
		
		double tetR=0;
		
		for(BSimControlledBacterium b : bacteria) {
			tetR=tetR+b.y[3];
		}
		
		tetR=tetR/(Double.valueOf(bacteria.size()));
		
		errs[0]=ref-tetR;
		
		return errs;
		
		
	}
	

	
	public void setRef(double ref) {
		this.ref=ref;
	}
	
	public double getRef() {
		return ref;
	}
	
}
