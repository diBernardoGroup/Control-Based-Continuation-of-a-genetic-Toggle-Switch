package bBacterium;

import bsim.BSim;
import bsim.ode.BSimOdeSystem;
import java.lang.Math;
import java.util.Random;

public class BacteriumInternalDynDiffusion implements BSimOdeSystem {

	// Number of equations in the system
	protected int numEq;
	
	// Bacterium Drift (copy new parameters extracted during the division)
	private BacteriumInternalDynDrift drift;
	
	//Random number generator for the Weiner process
	private Random gaus;
	
	//RNumber of reaction involved
	private int nreact;
	
	//time step
	private double dt;
	
	//Parameters model
	private double klm0=3.045e-1;
	private double klm=13.01;
	private double thetaAtc=35.98;
	private double etaAtc=2.00;
	private double thetaTet=76.40;
	private double etaTet=2.152;
	private double glm=1.386e-1;
	private double ktm0=3.313e-1;
	private double ktm=5.055;   
	private double thetaIptg=2.926e-1;
	private double etaIptg=2.00;
	private double thetaLac=124.9;
	private double etaLac=2.00;
	private double gtm=1.386e-1;
	private double klp=6.606e-1;
	private double glp=1.65e-2;
	private double ktp=5.098e-1;
	private double gtp=1.65e-2;
	
	
	
	public BacteriumInternalDynDiffusion(BSim sim,BacteriumInternalDynDrift drift) {
		//Set the state space dimention
		numEq = 6;

		//Initialization of the gaussian noise
		gaus=new Random();
		
		//Initialize the number of reactions
		nreact=8;
		dt=sim.getDt()/60;
		
		//Initialize drift variable (Copy parameters)
		this.drift=drift;
	}
	
	
	@Override
	public double[] derivativeSystem(double t, double[] x) {
		
		double[] dx = new double[numEq];
		double[] w= new double[nreact];
		
		for (int i=0;i<nreact;i++) {
			w[i]=Math.sqrt(dt)*gaus.nextGaussian();
		}
		

		//Diffusion of the system
		
	    dx[0]=Math.sqrt(klm0+klm*HillFunc(x[3]*HillFunc(x[4],thetaAtc,etaAtc),thetaTet,etaTet))*w[6]-Math.sqrt(glm*x[0])*w[0];
	    	    
	    dx[1]=Math.sqrt(ktm0+ktm*HillFunc(x[2]*HillFunc(x[5],thetaIptg,etaIptg),thetaLac,etaLac))*w[7]-Math.sqrt(gtm*x[1])*w[1];

	    dx[2]=Math.sqrt(klp*x[0])*w[4]-Math.sqrt(glp*x[2])*w[2];
	    
	    dx[3]=Math.sqrt(ktp*x[1])*w[5]-Math.sqrt(gtp*x[3])*w[3];
	    
	    dx[4]=0;
	    
	    dx[5]=0;
	    
	    
		return dx;
		
		
	}

	//Utility function to get the derivative of the system
	private double HillFunc(double x, double th, double eta) {
		double hill=1/(1+Math.pow((x/th),eta));
		return hill;
	}


	//Initial conditions of the state
	@Override
	public double[] getICs() {
		double[] x0=new double[numEq];
		
		Random rng=new Random();
		
		
		x0[0]=2.55166047363230;
		x0[1]=38.7108543679906;
		x0[2]=102.155003051775;
		x0[3]=1196.05604522200;
		x0[4]=25; //aTc
		x0[5]=1; //IPTG
		
		
		
		/*
		
		x0[0]=3+6*rng.nextDouble();
		x0[1]=3+6*rng.nextDouble();
		x0[2]=150+300*rng.nextDouble();
		x0[3]=200+400*rng.nextDouble();
		x0[4]=0;
		x0[5]=0;
		
		*/
	
		
		return x0;
	}

	@Override
	public int getNumEq() {
		
		return numEq;
	}

	
	//Function to copy the drift parameters
	public void copy_new_parameters() {
		klm0=drift.getKlm0();
		klm=drift.getKlm();
		thetaAtc=drift.getThetaAtc();
		etaAtc=drift.getEtaAtc();
		thetaTet=drift.getThetaTet();
		etaTet=drift.getEtaTet();
		glm=drift.getGlm();
		ktm0=drift.getKtm0();
		ktm=drift.getKtm();   
		thetaIptg=drift.getThetaIptg();
		etaIptg=drift.getEtaIptg();
		thetaLac=drift.getThetaLac();
		etaLac=drift.getEtaLac();
		gtm=drift.getGtm();
		klp=drift.getKlp();
		glp=drift.getGlp();
		ktp=drift.getKtp();
		gtp=drift.getGtp();
	}
	
	
}
