package bBacterium;

import java.util.Vector;

import javax.vecmath.Vector3d;


import bSolver.BSimEulerMaruyama;
import bsim.BSim;
import bsim.BSimChemicalField;
import bsim.capsule.BSimCapsuleBacterium;


/*********************************************************
 * Step 2: Bacterium Emulating the Toggle Switch Gene Regulatory Network (GRN)
 */		


public class BSimControlledBacterium extends BSimCapsuleBacterium {

	//definition of the internal dynamics
	protected BSimEulerMaruyama dyn;
	
	//external Inducer Fields
	protected BSimChemicalField AHLField;
	protected BSimChemicalField IPTGField;
	
	//current ODE variables
	public double[] y;
	public double[] y_fields; //It stores the old value to feed the field update (unless it overritten the old one and the field is fed with the next value)
	protected double[] yNew;
	
	//List of active bacteria
	private Vector<BSimControlledBacterium> bacteria;

	
	//Limit set to the bacteria population
	private int populationLimit;
	
	//Unique id of the bacterium
	private static int curr_id=0;
	private int id;
	
	//Bacterium dynamics
	private BacteriumInternalDynDrift drift;
	private BacteriumInternalDynDiffusion diff;
	
	//Current simulation
	private BSim sim;

	
	public BSimControlledBacterium(BSim sim, Vector3d x1,Vector3d x2, BSimChemicalField fAHL, BSimChemicalField fIPTG,Vector<BSimControlledBacterium> bacteria,int populationLimit) {
		super(sim, x1, x2);
		
		//Set the growth to make the bacterium replicate about every 25 min
		this.k_growth=0.002;
		
		
		//Chemical fields initialization
		AHLField=fAHL;
		IPTGField=fIPTG;
		this.sim=sim;
		
		
		//Initialization of the dynamics (Drift and Diffusion of the SDE)
		drift = new BacteriumInternalDynDrift(); 
		diff= new BacteriumInternalDynDiffusion(sim,drift);
		dyn= new BSimEulerMaruyama(drift,diff,sim.getDt()/60);
		
		
		//set the initial conditions
		y = drift.getICs();
		y_fields=new double [6];
		for (int i=0;i<6;i++) {
			y_fields[i]=y[i];
		}
		
		//Set the other variables
		this.bacteria=bacteria;
		this.populationLimit=populationLimit;
		
		//Setting the ID
		id=curr_id;
		curr_id++;
		
		
	}
	
	// Movement, GRN Evolution (and growth/replication if growth rate > 0)
	@Override
	public void action() {
		
		
		super.action();	// Movement (and growth/replication if growth rate > 0)
		
		// Drift External Fields Setting
		drift.setfaTc(AHLField.getConc(position));
		drift.setfIPTG(IPTGField.getConc(position));
		
		
		// Solve the SDE system
		// IMPORTANT: re-scale the time units correctly (Dyn equations are in minutes, BSim works in seconds)
		y_fields=y; 										//the current state value
		yNew = dyn.Euler_Maruyama(y, sim.getTime()/60);		//update the state value of the TS
		y = yNew; 											//substitute the current state value
		
		
	}

	
	/*********************************************************
	 * Growth and replication.
	 */

	// Function For the division of the bacterium
	@Override
	public BSimControlledBacterium divide() {
		//BSIM ORIGINAL CODE START
		
		/*
        this           this    child
        x1 ->u x2  ->  x1  x2  x1  x2
        o------o       o---o | o---o
        */

        // TODO: refactor u into the main class. (As a method? Could then be up-to-date each time it is required...)
        // Total length is actually L + 2*r

        Vector3d u = new Vector3d(); u.sub(this.x2, this.x1);

        // Uniform Distn; Change to Normal?
        double divPert = 0.1*L_max*(rng.nextDouble() - 0.5);

        double L_actual = u.length();

        double L1 = L_actual*0.5*(1 + divPert) - radius;
        double L2 = L_actual*0.5*(1 - divPert) - radius;

        /// TODO::: Check that these are computed correctly...!
        Vector3d x2_new = new Vector3d();
        x2_new.scaleAdd(L1/L_actual, u, this.x1);
        x2_new.add(new Vector3d(0.05*L_initial*(rng.nextDouble() - 0.5),
                                0.05*L_initial*(rng.nextDouble() - 0.5),
                                0.05*L_initial*(rng.nextDouble() - 0.5)));

        Vector3d x1_child = new Vector3d();
        x1_child.scaleAdd(-(L2/L_actual), u, this.x2);
        x1_child.add(new Vector3d(0.05*L_initial*(rng.nextDouble() - 0.5),
                                  0.05*L_initial*(rng.nextDouble() - 0.5),
                                  0.05*L_initial*(rng.nextDouble() - 0.5)));

        /*
        This is dangerous.
        Ideally initialise all four co-ordinates, otherwise this operation is order-dependent
        (this.xi will be overwritten before being passed to child for ex.)
         */
        
        
      //BSIM ORIGINAL CODE END
        
        
        //Child Bacterium Initialization
        BSimControlledBacterium child = new BSimControlledBacterium(sim,x1_child,new Vector3d(this.x2),AHLField,IPTGField,bacteria,populationLimit);
        //Initialization of the state variables
        for (int i=0;i<6;i++) { //Its state is the same of the mother
			child.y[i]=this.y[i];
			child.y_fields[i]=this.y_fields[i];
		}
        
        //Initialize the child
        child.L = L2;
        child.initialise(L2, x1_child, this.x2);
        
        //Reinitialize the mother
        this.initialise(L1, this.x1, x2_new);


        //Change the parameters of the child (extracting them from an uniform distribution around their nominal values) 
        child.drift.change_parameters(0); //Change parameters in the drift variable...
        child.diff.copy_new_parameters();	//... Copy them to the diffusion variable
        
        
        return child;
		
		
	}


	
	public int getId() {
		return id;
	}

	
	
	//To make the bacteria flow out both in the upper and the lower border 
	@Override
    public void computeWallForce(){
//      System.out.println("Wall Force");

      // TODO::: Ideally, there should also be a bounds check on the side NEXT to the one from which bacs can exit
      /**
       * i.e.,
       *
       * open, flow - - - - - - - ->
       *            |            |  should have a bounds check here @ top so that bacs being pushed by the 'flow'
       *  closed    |            |  are allowed to continue moving right, above the RHS wall, rather than being
       *            .            .  *stopped* by the RHS bound check!
       *
       */
      wallBelow(x1.x, x1force, new Vector3d(1,0,0));
      wallBelow(x1.y, x1force, new Vector3d(0,1,0)); // TOP // (IF I WANT THE CELLS TO EXIT ON THE TOP I HAVE TO COMMENT THIS LINE)
      wallBelow(x1.z, x1force, new Vector3d(0,0,1));

      wallAbove(x1.x, x1force, new Vector3d(-1, 0, 0), sim.getBound().x);

//      wallAbove(x1.y, x1force, new Vector3d(0, -1, 0), sim.getBound().y); // BOTTOM //
      wallAbove(x1.z, x1force, new Vector3d(0, 0, -1), sim.getBound().z);

      wallBelow(x2.x, x2force, new Vector3d(1,0,0));
      wallBelow(x2.y, x2force, new Vector3d(0,1,0)); // TOP // (IF I WANT THE CELLS TO EXIT ON THE TOP I HAVE TO COMMENT THIS LINE)
      wallBelow(x2.z, x2force, new Vector3d(0,0,1));

      wallAbove(x2.x, x2force, new Vector3d(-1,0,0), sim.getBound().x);

//      wallAbove(x2.y, x2force, new Vector3d(0, -1, 0), sim.getBound().y); // BOTTOM //
      wallAbove(x2.z, x2force, new Vector3d(0, 0, -1), sim.getBound().z);
  }
  
	
	
}
