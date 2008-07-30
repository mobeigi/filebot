
package net.sourceforge.tuned.ui.notification;


import javax.swing.SwingConstants;


class Factor implements SwingConstants {
	
	public final double fx;
	public final double fy;
	
	
	public Factor(double fx, double fy) {
		this.fx = fx;
		this.fy = fy;
	}
	

	static Factor getOrientationFactor(int orientation) {
		double fx = 0;
		double fy = 0;
		
		switch (orientation) {
			case NORTH_WEST:
				fx = 0;
				fy = 0;
				break;
			
			case NORTH:
				fx = 0.5;
				fy = 0;
				break;
			
			case NORTH_EAST:
				fx = 1;
				fy = 0;
				break;
			
			case WEST:
				fx = 0;
				fy = 0.5;
				break;
			
			case EAST:
				fx = 1;
				fy = 0.5;
				break;
			
			case SOUTH_WEST:
				fx = 0;
				fy = 1;
				break;
			
			case SOUTH:
				fx = 0.5;
				fy = 1;
				break;
			
			case SOUTH_EAST:
				fx = 1;
				fy = 1;
				break;
			
			case CENTER:
				fx = 0.5;
				fy = 0.5;
		}
		
		return new Factor(fx, fy);
	}
	

	static Factor getDirectionFactor(int direction) {
		double fx = 0;
		double fy = 0;
		
		switch (direction) {
			case NORTH_WEST:
				fx = -1;
				fy = -1;
				break;
			
			case NORTH:
				fx = 0;
				fy = -1;
				break;
			
			case NORTH_EAST:
				fx = 1;
				fy = -1;
				break;
			
			case WEST:
				fx = -1;
				fy = 0;
				break;
			
			case EAST:
				fx = 1;
				fy = 0;
				break;
			
			case SOUTH_WEST:
				fx = -1;
				fy = 1;
				break;
			
			case SOUTH:
				fx = 0;
				fy = 1;
				break;
			
			case SOUTH_EAST:
				fx = 1;
				fy = 1;
				break;
			
			case CENTER:
				fx = 0;
				fy = 0;
		}
		
		return new Factor(fx, fy);
	}
	
}
