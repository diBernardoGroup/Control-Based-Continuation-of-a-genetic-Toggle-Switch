package bControl;

import java.util.Vector;

import com.mathworks.engine.MatlabEngine;

import bBacterium.BSimControlledBacterium;
import bsim.BSim;

public class Controller_MPC_population implements Controller {

	public MatlabEngine eng;
	
	private double ref;

	
	public double [] curr_e;
	private double[] state_per;
	private int index;
	
	private double IPTG0;
	private double aTc0;
	private double [] x_hat_init;
	private double [] last_u;
	private int last_u_ind;
	private double u_previous_pt;
	
	private BSim sim;
	
	
	
	public Controller_MPC_population (double ref,double IPTG0,BSim sim) {
		this.ref=ref;
		state_per=new double[2];
		curr_e=new double[2];
		index=1;
		this.IPTG0=IPTG0;
		aTc0=25;
		x_hat_init=new double[4];
		last_u=new double[12];
		last_u_ind=0;
		u_previous_pt=-1;
		
		
		this.sim=sim;
		
		try {
			eng = MatlabEngine.startMatlab();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public double Evaluate_Control(double [] e) {
		
		double u_a;
		
			
		//System.out.println("Control Loop n"+index);
		try {
			double [] u_e_x=eng.feval("MPC_BSIM", state_per, ref, IPTG0, aTc0, x_hat_init, u_previous_pt );			
			
			IPTG0=u_e_x[0];
			x_hat_init[0]=u_e_x[1];
			x_hat_init[1]=u_e_x[2];
			x_hat_init[2]=u_e_x[3];
			x_hat_init[3]=u_e_x[4];
			last_u[last_u_ind]=IPTG0;
			last_u_ind=(last_u_ind+1)%12;
			
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		index++;
		
        return IPTG0;
	}
	
	
	public double[] Evaluate_Error(Vector<BSimControlledBacterium> bacteria) {
		
		double tetR = 0;
		double lacI = 0;
		
		for(BSimControlledBacterium b : bacteria) {
			tetR=tetR+b.y[3];
			lacI=lacI+b.y[2];
		}
		
		tetR=tetR/(Double.valueOf(bacteria.size()));
		lacI=lacI/(Double.valueOf(bacteria.size()));

		state_per[0]=lacI;
		state_per[1]=tetR;
		
		
		curr_e[0]=ref-tetR;
		
		return curr_e;
		
		
	}
	
	
	
	public void setRef(double ref) {
		this.ref=ref;
		
		if(sim.getTime()>0) {
			u_previous_pt=0;
			for(int i=0;i<12;i++) {
				u_previous_pt+=last_u[i];
			}
			u_previous_pt=u_previous_pt/12.0;
		}
		System.out.println("u_prev="+u_previous_pt);
		
	}
	
	
	public double getRef() {
		return ref;
	}
	
	
	
}
