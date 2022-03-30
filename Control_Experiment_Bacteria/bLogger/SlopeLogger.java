package bLogger;

import java.util.Vector;

import bBacterium.BSimControlledBacterium;
import bTicker.MyTicker;
import bsim.BSim;
import bsim.export.BSimLogger;

public class SlopeLogger extends BSimLogger {
	
	private MyTicker tick;

	public SlopeLogger(BSim sim, String filename, MyTicker tick) {
		super(sim, filename);
		this.tick=tick;
	}

	@Override
	public void during() {
		
		write(tick.getSl_e()+","+tick.getSl_u()+",");
		
		
	}

}
