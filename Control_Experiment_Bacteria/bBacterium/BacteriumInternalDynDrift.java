package bBacterium;

import bsim.ode.BSimOdeSystem;
import java.lang.Math;
import java.util.Random;

public class BacteriumInternalDynDrift implements BSimOdeSystem {

	// Number of equations in the system
	protected int numEq;
			
	// External level of inducer - can get this from the chemical field
	protected double aTCext;	// [uM]
	protected double IPTGext;	
	
	
	//Model Parameters
	private double klm0=3.2e-2;
	private double klm=8.3;
	private double thetaAtc=11.65;
	private double etaAtc=2.00;
	private double thetaTet=30;
	private double etaTet=2;
	private double glm=1.386e-1;
	private double ktm0=1.19e-1;
	private double ktm=2.06;   
	private double thetaIptg=9.06e-2;
	private double etaIptg=2.00;
	private double thetaLac=31.94;
	private double etaLac=2.00;
	private double gtm=1.386e-1;
	private double klp=9.762e-1;
	private double glp=1.65e-2;
	private double ktp=1.17;
	private double gtp=1.65e-2;
	private double kaTC=1.62e-1;
	private double kaTC2=2.00e-2;
	private double kIPTG=2.75e-2;
	private double kIPTG2=1.11e-1;
	
	public BacteriumInternalDynDrift() {
		
		//set the state space dimension
		numEq = 6;
		
		
		//Initial external inducer values
		aTCext=0;
		IPTGext=0;
		
	}
	
	
	@Override
	public double[] derivativeSystem(double t, double[] x) {
		
		double[] dx = new double[numEq];
		

		//System dynamics (Drift part)
		
	    dx[0]=klm0+klm*HillFunc(x[3]*HillFunc(x[4],thetaAtc,etaAtc),thetaTet,etaTet)-glm*x[0];
	    
	    dx[1]=ktm0+ktm*HillFunc(x[2]*HillFunc(x[5],thetaIptg,etaIptg),thetaLac,etaLac)-gtm*x[1];

	    dx[2]=klp*x[0]-glp*x[2];
	    
	    dx[3]=ktp*x[1]-gtp*x[3];
	    
	    if(aTCext>x[4]) {
	    	dx[4]=kaTC*(aTCext-x[4]);
	    }else {
	    	dx[4]=kaTC2*(aTCext-x[4]);
	    }
	    
	    if(IPTGext>x[5]) {
	    	dx[5]=kIPTG*(IPTGext-x[5]);
	    }else {
	    	dx[5]=kIPTG2*(IPTGext-x[5]);
	    }
	    
	    
	    
		return dx;
		
		
	}

	
	//Auxiliary Function implementing H^-(x,th,eta)
	private double HillFunc(double x, double th, double eta) {
		double hill=1/(1+Math.pow((x/th),eta));
		return hill;
	}


	//Initial Conditions for the GRN (Near the unstable equilibrium)
	@Override
	public double[] getICs() {
		
		
		Random rng=new Random();
		
		double[] x0=new double[numEq];
		
		
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

	
	//Function to change parameters every time a bacterium divides
	public void change_parameters(double perc) {
		
		Random rng=new Random();
		
		
		//Model Parameters change (new parameters are extracted from U(n-perc*n,n+perc*n) where n is the nominal value
		klm0=change_parameter(3.20e-2, perc,rng);
		klm=change_parameter(8.30, perc,rng);
		thetaAtc=change_parameter(11.65, perc,rng);
		etaAtc=change_parameter(2.00, perc,rng);
		thetaTet=change_parameter(30.00, perc,rng);
		etaTet=change_parameter(2.00, perc,rng);
		glm=change_parameter(1.386e-1, perc,rng);
		ktm0=change_parameter(1.19e-1, perc,rng);
		ktm=change_parameter(2.06, perc,rng);   
		thetaIptg=change_parameter(9.06e-2, perc,rng);
		etaIptg=change_parameter(2.00, perc,rng);
		thetaLac=change_parameter(31.94, perc,rng);
		etaLac=change_parameter(2.00, perc,rng);
		gtm=change_parameter(1.386e-1, perc,rng);
		klp=change_parameter(9.726e-1, perc,rng);
		glp=change_parameter(1.65e-2, perc,rng);
		ktp=change_parameter(1.170, perc,rng);
		gtp=change_parameter(1.65e-2, perc,rng);
		kaTC=change_parameter(1.62e-1, perc,rng);
		kIPTG=change_parameter(2.00e-2, perc,rng);
		kaTC2=change_parameter(2.75e-2, perc,rng);
		kIPTG2=change_parameter(1.11e-1, perc,rng);
		
	}
	


	//Extraction from U(n-perc*n,n+perc*n) where n is the nominal value
	private double change_parameter(double m, double p,Random rng) {
		
		return m-p/2*m+rng.nextDouble()*p*m;
		
		
	}

	//Setters

	
	public void setfaTc(double val){
		aTCext=val;
	}
	
	public void setfIPTG(double val){
		IPTGext=val;
	}
	
	
	
	//Getters
	
	public double getIPTGext() {
		return IPTGext;
	}


	public double getKlm0() {
		return klm0;	
	}



	public double getKlm() {
		return klm;
	}


	public double getThetaAtc() {
		return thetaAtc;
	}


	public double getEtaAtc() {
		return etaAtc;
	}


	public double getThetaTet() {
		return thetaTet;
	}


	public double getEtaTet() {
		return etaTet;
	}


	public double getGlm() {
		return glm;
	}


	public double getKtm0() {
		return ktm0;
	}


	public double getKtm() {
		return ktm;
	}


	public double getThetaIptg() {
		return thetaIptg;
	}


	public double getEtaIptg() {
		return etaIptg;
	}


	public double getThetaLac() {
		return thetaLac;
	}


	public double getEtaLac() {
		return etaLac;
	}


	public double getGtm() {
		return gtm;
	}


	public double getKlp() {
		return klp;
	}


	public double getGlp() {
		return glp;
	}


	public double getKtp() {
		return ktp;
	}


	public double getGtp() {
		return gtp;
	}


	public double getKaTC() {
		return kaTC;
	}


	public double getkIPTG() {
		return kIPTG;
	}
	
	
}
