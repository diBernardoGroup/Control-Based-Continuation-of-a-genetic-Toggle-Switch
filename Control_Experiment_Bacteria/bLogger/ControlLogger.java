package bLogger;


import bControl.Controller;
import bsim.BSim;
import bsim.BSimChemicalField;
import bsim.export.BSimLogger;

public class ControlLogger extends BSimLogger {

	
	BSimChemicalField a;
	BSimChemicalField i;
	Controller ctrl;
	
	public ControlLogger(BSim sim, String filename,BSimChemicalField a,BSimChemicalField i, Controller ctrl) {
		super(sim,filename);
		this.a=a;
		this.i=i;
		this.ctrl=ctrl;
	}
	
	
	@Override
	public void during() {
		
		String buffer = new String();
		buffer=a.getConc(0,0,0)+","+i.getConc(0,0,0)+","+ctrl.getRef()+" ";
		write(buffer);
		
	}

}
