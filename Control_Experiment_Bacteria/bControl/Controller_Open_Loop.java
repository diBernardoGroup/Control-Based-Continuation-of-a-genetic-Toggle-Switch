package bControl;

import java.util.Vector;

import bBacterium.BSimControlledBacterium;
import bTicker.MyTicker;
import bsim.BSim;

public class Controller_Open_Loop implements Controller {

	
	private double[] open_loop;
	private BSim sim;
	private double last_change=-10;
	int ctrl_ind;
	
	public Controller_Open_Loop(BSim sim) {
		
		
		this.sim=sim;
		
		
		open_loop=new double [30];
		double step=1.0/15.0;
		int j=0;
		for(double i=0;i<=1;i=i+step) {
			open_loop[j]=i;
			j++;
		}
		for(double i=1-step;i>=step;i=i-step) {
			open_loop[j]=i;
			j++;
		}
		last_change=-10;
		ctrl_ind=0;
		
	}
	
	
	
	@Override
	public double Evaluate_Control(double[] e) {
		
		double curr_t=sim.getTime()/60;
		
		
		if(curr_t/60>=last_change+10) {
			last_change=curr_t/60;
			ctrl_ind++;
		}
		
		return open_loop[ctrl_ind-1];
		
	}

	@Override
	public double[] Evaluate_Error(Vector<BSimControlledBacterium> bacteria) {
		// TODO Auto-generated method stub
		return new double[2];
	}

	@Override
	public void setRef(double ref) {
		// TODO Auto-generated method stub

	}

	@Override
	public double getRef() {
		// TODO Auto-generated method stub
		return 0;
	}

}
