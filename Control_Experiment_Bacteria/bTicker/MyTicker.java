package bTicker;

import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import com.mathworks.engine.MatlabEngine;

import bBacterium.*;
import bControl.Controller;
import bControl.Controller_MPC;
import bsim.BSim;
import bsim.BSimChemicalField;
import bsim.BSimTicker;
import bsim.capsule.BSimCapsuleBacterium;
import bsim.capsule.Mover;
import bsim.capsule.RelaxationMover;
import bsim.capsule.RelaxationMoverGrid;
import bField.*;

public class MyTicker extends BSimTicker{
	
	//Bacteria Vectors
	private Vector<BSimControlledBacterium> bacteria;
	private Vector<BSimControlledBacterium> childBacteria;
	private Vector<BSimCapsuleBacterium> moveBacteria;
	private Vector<BSimControlledBacterium> removedBacteria;
	private Vector<BSimControlledBacterium> allBacteria;
	
	//Chemical Fields
	private ControlledFIeld faTc;
	private ControlledFIeld fIPTG;
	
	//Simulation Parameters
	private BSim sim;
	private int poplim;
	
	//Auxiliary Class to move Bacteria
	private Mover mover;
	
	//Constraints Parameters
	private double Ts;
	private double Tc;
	private double Td;
	
	//Controller Class
	private Controller ctrl;
	
	
	//Auxiliary Variables 
	private double L_s;
	private double L_c;
	private boolean F_c;
	private double u_a;
	private double u_p;
	private double [] curr_e;
	private int pts;
	
	
	//Auxiliary valiables (Adaptive reference)
	private double [] err_his;
	private int err_ind;
	private double [] u_his;
	private int u_ind;
	public MatlabEngine eng;
	private double u_th;
	private double e_th;
	private double sl_e;
	private double sl_u;
	
	
	//Auxiliary vriables (reference vector)
	private double [] refs;
	private double last_change;
	private int ref_ind;
	
	
	
	
	
	public MyTicker(Vector<BSimControlledBacterium> bacteria,BSimChemicalField faTc,BSimChemicalField fIPTG,BSim simul,Vector<BSimCapsuleBacterium> moveBacteria,Vector<BSimControlledBacterium> allBacteria,int poplim,Controller ctrl,double Ts,double Tc,double Td, double upper_ref, double lower_ref, int pts) {
		this.bacteria=bacteria; 									//The current bacteria
		this.faTc=(ControlledFIeld)faTc; 							//The aTC field
		this.fIPTG=(ControlledFIeld)fIPTG;	 						//the IPTG field
		sim=simul;													//Main simulation class
		this.moveBacteria=moveBacteria;								//Bacteria to move
		this.childBacteria=new Vector<BSimControlledBacterium>();	// Temporary array to storage new bacteria
		this.removedBacteria=new Vector<BSimControlledBacterium>(); // Temporary array to storage flushed out bacteria
		this.allBacteria=allBacteria;								// All bacteria ever created in this simulation
		this.poplim=poplim;											//Population limit (Computational burden)
		mover=new RelaxationMoverGrid(moveBacteria, sim);			// Mover class (moves bacteria)
		refs=new double[(pts+1)];
		
		
		
		//MPC 1200
		//P 1800 / 1500 stoch
		this.pts=pts;
		
/*		double step=(upper_ref-lower_ref)/(pts-1);					//Step for the creation of the reference vector
		int j=0;													//Variable to change the ref
		for(double i=upper_ref;i>lower_ref;i=i-step) {				//Initialization of the reference vector (linear)
			refs[j]=i;
			j++;
		}	
*/
		double step=(1800.0-850.0)/(9-1);					//Step for the creation of the reference vector
		double setp=1200;									//Variable to change the ref
		for(int i=0;i<10;i++) {				//Initialization of the reference vector (linear)
			refs[i]=setp;
			setp-=step;
		}	
		step=(800.0-250.0)/(17-1);					//Step for the creation of the reference vector
		setp=800;									//Variable to change the ref
		for(int i=10;i<10+15;i++) {				//Initialization of the reference vector (linear)
			refs[i]=setp;
			setp-=step;
		}	
		step=(200.0-0.0)/(4-1);					//Step for the creation of the reference vector
		setp=200;									//Variable to change the ref
		for(int i=25;i<10+15+5;i++) {				//Initialization of the reference vector (linear)
			refs[i]=setp;
			setp-=step;
		}	
		
	
		refs[pts]=1;
		
/*		double step=(1500.0-0.0)/29.0;								//Step for the creation of the reference vector
		int j=0;
		for(double i=1500;i>0;i=i-step) {
			refs[j]=i;
			j++;
		}	
*/		
/*		for(double i=0;i<1500;i=i+step) {
			refs[j]=i;
			j++;
		}
*/
		
		
		//CONSTRAINTS PARAMETERS
		this.Ts=Ts;
		this.Tc=Tc;
		this.Td=Td;
		
		//Controller class
		this.ctrl=ctrl;
		
		
		//AUXILIARY VARIABLES
		L_s=-Ts;
		L_c=-Tc;
		F_c=false;
		u_a=0;
		u_p=0;
		last_change=-10;
		ref_ind=0;
		
		
		//Auxiliary Variables (Adaptive change of the reference)
		err_ind=0;
		err_his=new double [12];
		u_ind=0;
		u_his=new double [12];
		try {
			eng = MatlabEngine.startMatlab();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		u_th=0.01;
		e_th=0.1;
		
		
		
	}
	
	
	//Get the current reference index
	public int getRefInd() {
		return ref_ind;
	}
	
	
	// This will be called once at each global time step
				@Override
				public void tick() {
					
			
					
					/** Step 1: Update all the dynamics correctly   */
					
					
					for (BSimControlledBacterium b : bacteria) {
                        b.action();
                    }
					
					
					
					double curr_t=sim.getTime()/60;
					
					
					//Cycle to avoid outOfBound Exception
					if(ref_ind>pts) {
						ref_ind=pts;
					}
					
					

					
					//Something I do every sampling time
					if (curr_t>=L_s+Ts) {
						
						
						
						
						//Condition to chage the reference (Adaptive)
						if(curr_t/60>=last_change+10) { 		//if 10h have passed from the previous reference change
							last_change=curr_t/60;				//Reference is changing
							ctrl.setRef(refs[ref_ind]);			//Change the reference
							ref_ind++;							//Point to the next reference
						}else {					
						
							//Adaptive reference change
							if(curr_t/60>=last_change+3) { 		//if at least 3h have passsed
							
								double [] slope_err=new double [12];	//Ordered last hour error
								double [] slope_u=new double[12];		//Ordered last hour control
							
								//NOTE: err_ind is the next free space in the circular vector of the error
								
								//First spaces are the oldest ones (from err_ind to the end of the vector)
								for(int i=0;i<12-err_ind;i++) {			
									slope_err[i]=err_his[err_ind+i];
								}
								//Then i fill the remaining with the elements from the first to err_ind-1
								for(int i=12-err_ind;i<12;i++) {
									slope_err[i]=err_his[i-(12-err_ind)];
								}
								//I do the same with the control 
								for(int i=0;i<12-u_ind;i++) {
									slope_u[i]=u_his[u_ind+i];
								}
								for(int i=12-u_ind;i<12;i++) {
									slope_u[i]=u_his[i-(12-u_ind)];
								}
							
									
								try {
									
									//Computation of the slope of the error and of the control input
									sl_e=eng.feval("Slope", slope_err);	
									sl_u=eng.feval("Slope", slope_u);
									
									
									
									//if both the slopes are below their thresholds (we are at steady state)
									if((Math.abs(sl_e)<=e_th)&Math.abs(sl_u)<=u_th) {
										
										last_change=curr_t/60;					//The reference is changing
										
										System.out.println("Time Change reference : " + curr_t/60 + " with slopes : slu" +sl_u+ " , sle: "+ sl_e);
										
										ctrl.setRef(refs[ref_ind]);				//I change the reference 
										ref_ind++;								//Update the pointer to the next reference
									}
								
								
								} catch (RejectedExecutionException | InterruptedException | ExecutionException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							
							
							}
						
						}
						
						
						
						curr_e=ctrl.Evaluate_Error(bacteria);				//Compute the control error
						System.out.println("eh: "+curr_e[0]+" time:"+(sim.getFormattedTimeHours())+"ref:"+ctrl.getRef()+"Control="+u_p);
						u_p=ctrl.Evaluate_Control(curr_e);					//Compute the control input (u_p)
						u_a=25;												//Compute u_a (fixed)
						
						
						//Store the error and the control in circular vectors
						err_his[err_ind]=curr_e[0];
						err_ind=(err_ind+1)%12;
						u_his[u_ind]=u_p;
						u_ind=(u_ind+1)%12;
						
						
						
						L_s=curr_t;											//Update the last time it has been sampled
						
						
					}
					
					//Something I do every control time
					if (curr_t>=L_c+Tc) {
						
						L_c=curr_t;											//Update the last time it has been updated the control		
						F_c=true;
						
					}
					
					
					//Wait Tdiff and update the actual control provided to cells 
					if (F_c==true) {
						
						if(curr_t>=L_c+Td) {
							faTc.setControl(u_a);
							fIPTG.setControl(u_p);
							F_c=false;
						}
						
						
					}
					
					
					
					
					
					// Update our chemical fields values with their ODE
					faTc.updateValues();
					fIPTG.updateValues();

					
					/** Step 2: Update all the position and performs the diffusion */
					
					//Update by diffusion and degradation
					faTc.update();
					fIPTG.update();


					for (BSimControlledBacterium b : bacteria) {
                        if(bacteria.size()+childBacteria.size()<poplim) {
                        	b.grow();
                            // Divide if grown past threshold
                            if (b.L > b.L_th) {
                            	childBacteria.add(b.divide()); // The initialization is done in the divide()
                            }
                        }
                    }

					
					// Add freshly bred bacteria
					bacteria.addAll(childBacteria);
					moveBacteria.addAll(childBacteria);
					allBacteria.addAll(childBacteria);
					childBacteria.clear();
					
					
					//move the bacteria
					mover.move();
					
					//Remove the flushed out bacteria
					for (BSimControlledBacterium b : bacteria) {
                        if(b.position.x < 0 || b.position.x > sim.getBound().x || b.position.y < 0 || b.position.y > sim.getBound().y || b.position.z < 0 || b.position.z > sim.getBound().z){
                            removedBacteria.add(b);
                            for(int i=0;i<6;i++) { //Set their state to 0 (Distinguish from not flushed out bacteria)
                            	b.y[i]=0;
                            }
                        }
                    }
					bacteria.removeAll(removedBacteria);
					moveBacteria.removeAll(removedBacteria);
					removedBacteria.clear();
					

					
				}


				public double getSl_e() {
					return sl_e;
				}


				public double getSl_u() {
					return sl_u;
				}

				

}
