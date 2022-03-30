package bField;

import java.util.Vector;

import bBacterium.*;
import bsim.BSim;
import bsim.BSimChemicalField;
import bsim.ode.BSimOdeSolver;

public class ControlledFIeld extends BSimChemicalField {

	//Dynamics Vector
	private ExternalMediumDyn [][][] extDyn;
	
	
	//Field Values
	private double [][][][] fieldValues;
	private double [][][][] newValues;
	
	
	//Control input (External concentration imposed to the top and the bottom of the chamber (y axis))
	private double Control;
	
	
	
	public ControlledFIeld(BSim sim, int[] boxes, double diffusivity, double decayRate, Vector <BSimControlledBacterium> Bacteria,double k,double k2,int index) {
		
		
		super(sim, boxes, diffusivity, decayRate);						//Initialization of the BSimChemicalField Class	
		Control=0; 														//Initial concentration of the imposed concentration
		extDyn=new ExternalMediumDyn [boxes[0]] [boxes[1]] [boxes[2]];  //Vector of external dynamics for each box of the field

		
		//Initializing the dynamics in each box
		for (int i=0;i<boxes[0];i++) { 				//Axis x
			
			for (int j=0;j<boxes[1];j++) { 			//Axis y
				
				for (int kk=0;kk<boxes[2];kk++) { 	//Axis z
					
					int [] indbox= {i,j,kk};  		// Current box index
					if(j==0||j==(boxes[1]-1)) { 	// Check for the top or the bottom of the chamber
						extDyn[i][j][kk]=new ExternalMediumDyn(Bacteria,k,k2,index,indbox,this.box,sim); //Initialization of the dynamics
					}else {
						extDyn[i][j][kk]=new ExternalMediumDyn(Bacteria,k,k2,index,indbox,this.box,sim); //Initialization of the dynamics
					}
					
				}
				
			}
			
		}
		
		//Setting all the initial conditions of each box concentration
		fieldValues=new double [boxes[0]] [boxes[1]] [boxes[2]] [extDyn[0][0][0].getNumEq()];
		newValues=new double [boxes[0]] [boxes[1]] [boxes[2]] [extDyn[0][0][0].getNumEq()];
		
		//Setting the inital condition of each box
		for (int i=0;i<boxes[0];i++) {				//Axis x
			for (int j=0;j<boxes[1];j++) {			//Axis y
				for (int kk=0;kk<boxes[2];kk++) {	//Axis z
					fieldValues[i][j][kk]=extDyn[i][j][kk].getICs(); 	//Get the initial condition of the field 
					this.setConc(i, j, kk, fieldValues[i][j][kk][0]); 	//and sets its concentration (the dyn has only one value)
				}
			}
		}
		
		
	}
	
	
	
	//Update the concentration in each box due to the exchange through the membrane (and the control input)
	public void updateValues() {
		//Updating the concentrations of each box
				for (int i=0;i<boxes[0];i++) {					//Axis x
					for (int j=0;j<boxes[1];j++) {				//Axis y
						for (int kk=0;kk<boxes[2];kk++) {		//Axis z
							if(j==0||j==(boxes[1]-1)) {			//Check for the top of bottom of the chamber
								if(Control<=0) {				//Grant non-negative values for the control
									Control=0;
								}
								this.setConc(i, j, kk, Control);//Set concentration to the value dictated from Control
							}
							fieldValues[i][j][kk]=new double [] {this.getConc(i, j, kk)};// Get the current concentration (they are changed due to the diffusion and the degradation (update method))
							newValues[i][j][kk] = BSimOdeSolver.rungeKutta45(extDyn[i][j][kk], sim.getTime()/60, fieldValues[i][j][kk] , sim.getDt()/60); //Update the value with the diffusion dynamics in the cells
							if(newValues[i][j][kk][0]<0) {		//Grant non-negative values for the concentrations (physical constraints)
								newValues[i][j][kk][0]=0;
							}
							this.setConc(i,j,kk,newValues[i][j][kk][0]); //Set the concentration of each box to the new values (no need to update the fieldValues (it will be got from the field itself))
						}
					}
				}
	}
	
	
	public void setControl(double ctrl) {
		Control=ctrl;	//Set the control input to a given value 
	}
		
	
	

}
