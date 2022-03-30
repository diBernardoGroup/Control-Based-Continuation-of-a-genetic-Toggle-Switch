package bsimMain;

import bBacterium.BSimControlledBacterium;
import bControl.Controller;
import bControl.Controller_MPC;
import bControl.Controller_Open_Loop;
import bControl.Controller_P;

import java.util.Random;
import java.util.Vector;

import javax.vecmath.Vector3d;

import bDrawer.ChamberDrawer;
import bDrawer.My3DDrawer;
import bField.ControlledFIeld;
import bLogger.*;
import bTicker.MyTicker;
import bsim.BSim;
import bsim.BSimChemicalField;
import bsim.BSimUtils;
import bsim.capsule.BSimCapsuleBacterium;
import bsim.draw.BSimDrawer;
import bsim.export.BSimLogger;
import bsim.export.BSimMovExporter;
import bsim.export.BSimPngExporter;

/**
 * Ratiometric Control Problem on a Genetic Toggle Switch 
 */
public class BSim_Lugagne_Main {

	public static void main(String[] args) {
		
		/**
		 * Step1: Initializing all the needed variables 
		 */
		
		
		BSim sim = new BSim();			// New simulation object
		sim.setDt(0.6); 				// Global dt (time step)
		sim.setSimulationTime(300*60*60);// Total simulation time [sec]
		sim.setTimeFormat("0.00");		// Time format (for display etc.)
		sim.setBound(1.,50/3,1.);		// Simulation boundaries [um]
		sim.setSolid(true, false, true);//To make the bacteria do not cross the borders (Except y axis)
		MyTicker mytick;				// Definition of the Ticker Class
		
		//Control Parameters

		double Ts=5; 			//Sampling Time
		double Tc=5;			//Control Time
		double Td=2.0/3.0;		//Diffusion Time
		
		double ref=1800;		//Control Reference (Initial value)
		double upper_ref=1800.0;//Upper bound for the reference 
		double lower_ref=0.0;	//Lower bound for the reference 
		int pts=30;				//Number of points to be collected

		
		//Controllers initialization
		double k_p=0.0008*2;
		
		Controller ctrl=new Controller_P(ref,k_p); //Bang Bang Controller Initialization
//		Controller ctrl=new Controller_MPC(ref,1,sim); //Bang Bang Controller Initialization
//		Controller ctrl=new Controller_Open_Loop(sim);

		
		// Set up a list of bacteria that will be present in the simulation
		final Vector<BSimCapsuleBacterium> moveBacteria = new Vector<BSimCapsuleBacterium>(); 		//Bacteria that need to be moved
		final Vector<BSimControlledBacterium> activeBacteria= new Vector<BSimControlledBacterium>();//The bacteria currently alive
		final Vector<BSimControlledBacterium> allBacteria= new Vector<BSimControlledBacterium>();	//All bacteria ever existed in the simulation
		
		
		// Parameters for the chemical fields
		double kaTC=1.62e-1;
		double kaTC2=2.00e-2;
		double kIPTG=2.75e-2;
		double kIPTG2=1.11e-1;
		
		// Chemical Fields
		final BSimChemicalField faTC = new ControlledFIeld(sim, new int[] {1,10,1}, 1, 0,activeBacteria,kaTC,kaTC2,4) ;	//aTc Field Initialization
		final BSimChemicalField fIPTG = new ControlledFIeld(sim, new int[] {1,10,1}, 1, 0,activeBacteria,kIPTG,kIPTG2,5);	//IPTG Field Initialization
		
		
		//Population Parameters
		int populationLimit=1000; 				//the maximum number of cells currently alive (Computational Reasons)
		int populationInitialSize=1; 			//The initial population number
		Random rnd=new Random(); 				//Just to make the population initial position random
		
		
		
		//Population generation
		while(activeBacteria.size() < populationInitialSize) {		
			double bL = 1 + 0.1 * (rnd.nextDouble() - 0.5); 		//The initial length of a Bacterium
			double angle = 0 * rnd.nextDouble() * 2 * Math.PI; 		//The starting angle
	   
			
            
			BSimControlledBacterium b;								//Bacterium creation
            Vector3d pos = new Vector3d(0.5*sim.getBound().x, 0.2 *sim.getBound().y, sim.getBound().z / 2.0); //Center initial position
            Vector3d p1 = new Vector3d(pos.x - 0.5 * bL * Math.sin(angle), pos.y - 0.5 * bL * Math.cos(angle), pos.z);			//Extreme 1 initial position
            Vector3d p2 = new Vector3d(0.5 * bL * Math.sin(angle) + pos.x, 0.5 * bL * Math.cos(angle) + pos.y, pos.z);			//Extreme 2 initial position
           	b= new BSimControlledBacterium(sim, 
    					p1,p2,faTC,fIPTG,activeBacteria,populationLimit);  //Bacterium Initialization (Dynamical system)
           	b.initialise(bL, p1, p2);									   //Bacterium Initialization (biomechanics)
            //Add the bacterium to all lists
			activeBacteria.add(b);
			moveBacteria.add(b);
			allBacteria.add(b);
			
		}

		/*********************************************************
		 * Step 2: Implement tick() on a BSimTicker and add the ticker to the simulation	  
		 */

		
		mytick=new MyTicker(activeBacteria,faTC,fIPTG,sim,moveBacteria,allBacteria,populationLimit,ctrl,Ts,Tc,Td,upper_ref,lower_ref,pts);
		sim.setTicker(mytick);
		
		
		/*********************************************************
		 * Step 3: Adding the drawer to see the results
		 */
		
		BSimDrawer dr=new My3DDrawer(sim,1300 ,600 ,activeBacteria,faTC,fIPTG,ref );//I set up the drawer for the simulation
		sim.setDrawer(dr);
		BSimDrawer chdr= new ChamberDrawer(sim, 1300, 600, activeBacteria);
		
		/*********************************************************
		 * Step 3: Adding the exporters to see the results
		 */
		
		
		// Create a new directory for the simulation results
		String resultsDir = BSimUtils.generateDirectoryPath("./results/");			

		//This is to export the scene in a movie
	/*	BSimMovExporter movExporter = new BSimMovExporter(sim, dr, resultsDir + "BSim.mov"); //Exports a movie of the experiment
		movExporter.setDt(60*1);
		movExporter.setSpeed(60*40);//60*100
		sim.addExporter(movExporter);*/	
	
		BSimPngExporter imageExporter = new BSimPngExporter(sim, chdr, resultsDir + "images" ); //Exports the images (to create a video in matlab)
		imageExporter.setDt(60*5);
		sim.addExporter(imageExporter);
	
		//These exporters are allowed to collect data (time,states and population number)
		BSimLogger Tlogger= new TimeLogger(sim,resultsDir + "TimeValues.csv"); // Time logger
		Tlogger.setDt(60*5); 
		sim.addExporter(Tlogger);
		BSimLogger bacteriaLogger= new BacteriaLogger(sim,resultsDir + "BacteriaValues.csv",activeBacteria); // Bacteria state logger
		bacteriaLogger.setDt(60*5);
		sim.addExporter(bacteriaLogger);
		BSimLogger poplogger= new PopulationLogger(sim,resultsDir + "PopulationValues.csv",activeBacteria); // Population size, controlled bacterium logger
		poplogger.setDt(60*5);
		sim.addExporter(poplogger);
		/*BSimLogger concl=new ConcLogger(sim,resultsDir + "FieldValues.csv",faTC);
		concl.setDt(60*10);
		sim.addExporter(concl);*/
		BSimLogger contl=new ControlLogger(sim,resultsDir + "ControlInput.csv",faTC,fIPTG,ctrl); // Control input and Reference logger
		contl.setDt(60*5); 
		sim.addExporter(contl);
		BSimLogger slopel=new SlopeLogger(sim,resultsDir + "SlopeCurves.csv",mytick); // Control input and Reference logger
		slopel.setDt(60*5); 
		sim.addExporter(slopel);
		
		/*********************************************************
		 * Step 4: Starting the simulation
		 */
		
		//sim.preview();
		sim.export();
	}
}

