
package vumeter;

/**
 * An implementation of a driven, damped mass on a spring,
 * which may be used to model an analogue meter.<p>
 * A simple numerical-integration with a dt of 1-millisecond is used.
 */
public class DampedMassOnSpring {
	private double x, v, m, k, d;

	/**
	 * Construct a model for a driven damped mass on a spring.
	 * @param mass the mass of the suspended object.
	 * @param spring the spring constant ( extension per unit force ).
	 * @param damping the damping coefficient ( critical = 2 * m * Math.sqrt( k / m ) )
	 */
	public DampedMassOnSpring( double mass, double spring, double damping ) {
		m = mass;
		k = spring;
		d = damping;
	}
	
	/**
	 * Model the motion of the mass with the specified constant driving force
	 * for the specified number of milliseconds, and return the resultant
	 * extension.
	 */
	public final double model( double force, int millis ) {
		for( int n = 0; n < millis; n++ ) {
			double a = ( force - k * x - d * v ) / m; // Acceleration due to force.
			v = v + a * 0.001; // Change in velocity due to acceleration.
			x = x + v * 0.001; // Change in displacement due to velocity.
		}
		return x;
	}
	
	/**
	 * Model the motion of the mass with the specified constant driving force
	 * for the specified number of milliseconds, and return the resultant
	 * extension. This version of the method has upper and lower bounds for
	 * the extension, which results in an elastic collision if reached.
	 */
	public final double modelBounded( double force, double lower, double upper, int millis ) {
		for( int n = 0; n < millis; n++ ) {
			if( x > upper ) {
				x = upper;
				v = -v;
			}
			if( x < lower ) {
				x = lower;
				v = -v;
			}
			double a = ( force - k * x - d * v ) / m; // Acceleration due to force.
			v = v + a * 0.001; // Change in velocity due to acceleration.
			x = x + v * 0.001; // Change in displacement due to velocity.
		}
		return x;
	}
	
	public final double getDisplacement() {
		return x;
	}
}
