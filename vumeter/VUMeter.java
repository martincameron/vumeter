
package vumeter;

import java.awt.*;
import java.awt.event.*;

import javax.sound.sampled.*;

/**
 * A simple stereo VU Meter that displays the level of the default recording source.
 * The needle is modeled as a mass on a spring, so moves somewhat like a real voice-coil meter.
 * The scale is roughly in DBa, with each large tick-mark representing approximately double the amplitude.
 */
public class VUMeter extends Canvas implements Runnable {
	public static final String VERSION = "VU Meter 20190318 (c) mumart@gmail.com" ;
	private static final double SIX_DBA = Math.pow( 10, 0.3 );
	
	private Color bgColour, fgColour, peakColour;
	private DampedMassOnSpring leftModel, rightModel;
	private double leftForce, rightForce;
	private double leftDeflection, rightDeflection;
	private volatile boolean running;
	private int updateMillis, width;
	private long time;

	private Image background, foreground;

	/**
	 * Constructor.
	 * @param width the width of the component in pixels.
	 * @param bg the meter's background colour.
	 * @param fg the meter's foreground colour.
	 * @param peak the colour of the meter's "peak bar"
	 * @param hz the update frequency of the meter in hz. 60 is usually good enough.
	 */
	public VUMeter( int width, Color bg, Color fg, Color peak, int hz ) {
		this.width = width;
		bgColour = bg;
		fgColour = fg;
		peakColour = peak;
		leftModel = new DampedMassOnSpring( 0.005, 1.0, 0.08 );
		rightModel = new DampedMassOnSpring( 0.005, 1.0, 0.08 );
		updateMillis = Math.round( 1000f / hz );
	}
	
	public Dimension getPreferredSize()
	{
		return new Dimension( width, width / 4 );
	}

	public void paint( Graphics g ) {
		if( background == null ) {
			foreground = createImage( width, width / 2 );
			background = createImage( width / 2, width / 1 );
			drawMeter( background.getGraphics(), width / 2, bgColour.darker().darker(), bgColour, fgColour, peakColour );
		}
		Graphics graphics = foreground.getGraphics();
		graphics.drawImage( background, 0, 0, null );
		graphics.drawImage( background, width / 2, 0, null );
		drawNeedle( graphics, width / 2, fgColour, leftDeflection );
		graphics.translate( width / 2, 0 );
		drawNeedle( graphics, width / 2, fgColour, rightDeflection );
		g.drawImage( foreground, 0, 0, null );
	}
	
	public void update( Graphics g ) {
		long t = System.currentTimeMillis();
		int dt = ( int ) ( t - time );
		time = t;
		synchronized( this ){
			leftDeflection = leftModel.modelBounded( leftForce, 0, 1, dt );
			rightDeflection = rightModel.modelBounded( rightForce, 0, 1, dt );
		}
		paint( g );
	}
	
	public synchronized void setValue( double leftValue, double rightValue ) {
		leftForce = leftValue;
		rightForce = rightValue;
	}

	/**
	 * Begin updating the meter readings as quickly as possible upto the refresh
	 * frequency specified in the constructor.
	 */
	public void run() {
		try {
			time = System.currentTimeMillis();
			running = true;
			while( running ) {
				repaint();
				Thread.sleep( updateMillis );
			}
		} catch( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Signal the run method to stop execution.
	 */
	public void stop() {
		running = false;
	}
	
	public static double getForce( double amplitude ) {
		double force = Math.log( amplitude ) / Math.log( SIX_DBA ) + 8;
		if( force < 0 ) {
			force = 0;
		}
		return force / 8;
	}

	public static double getMaxAmplitude( byte[] audioBuf, int channel ) {
		int maxAmp = 0;
		for( int idx = 0; idx < audioBuf.length; idx += 4 ) {
			int off = channel * 2;
			int amp = ( audioBuf[ idx + off ] << 8 ) | ( audioBuf[ idx + off + 1 ] & 0xFF );
			if( amp < 0 ) {
				amp = -amp;
			}
			if( amp > maxAmp ) {
				maxAmp = amp;
			}
		}
		return maxAmp / 32768d;
	}
	
	public static void drawGradient( Graphics graphics, Color top, Color bottom, int width, int height ) {
		int r1 = top.getRed(), r2 = bottom.getRed();
		int g1 = top.getGreen(), g2 = bottom.getGreen();
		int b1 = top.getBlue(), b2 = bottom.getBlue();
		for( int y = 0; y < height; y++ ) {
			int r = r1 + ( r2 - r1 ) * y / height;
			int g = g1 + ( g2 - g1 ) * y / height;
			int b = b1 + ( b2 - b1 ) * y / height;
			graphics.setColor( new Color( r, g, b ) );
			graphics.fillRect( 0, y, width, 1 );
		}
	}

	public static void drawMeter( Graphics graphics, int width, Color bgColour1, Color bgColour2, Color fgColour, Color peakColour  ) {
		drawGradient( graphics, bgColour1, bgColour2, width, width / 2 );
		graphics.setColor( fgColour );
		graphics.drawRect( width * 5 / 16, width * 3 / 8, width * 6 / 16, width / 16 );
		drawRuler( graphics, width, fgColour, peakColour );
	}
	
	public static void drawNeedle( Graphics graphics, int width, Color colour, double deflection ) {
		double angle = deflection * Math.PI / 2 - Math.PI / 4;
		int x1 = (int)( width / 2 + width * 8 / 16 * Math.sin( angle ) );
		int y1 = (int)( width * 9 / 16 - width * 8 / 16 * Math.cos( angle ) );
		int x2 = (int)( width / 2 + width * 3 / 16 * Math.tan( angle ) );
		int y2 = width * 3 / 8;
		graphics.setColor( colour );
		graphics.drawLine( x1, y1, x2, y2 );
	}

	public static void drawRuler( Graphics graphics, int width, Color fgColour, Color peakColour ) {
		int x = width / 16;
		int y = width / 8;
		int w = width * 14 / 16;
		int h = width / 16;
		int segmentWidth = w / 7;
		// First black segment
		graphics.setColor( fgColour );
		graphics.fillRect( x, y, segmentWidth, h );
		// Ruler segments
		for( int n = x + segmentWidth; n < x + segmentWidth * 6; n += segmentWidth ){
			graphics.drawLine( n, y, n + segmentWidth, y );
			// Big marks
			graphics.drawLine( n, y, n, y + h - 1 );
			// Small marks
			if( segmentWidth >= 12 ) {
				for( int v = 1; v < 6; v++ )
					graphics.drawLine( n + segmentWidth * v / 6, y, n + segmentWidth * v / 6, y + h / 2 );
			}
		}
		// End red segment
		graphics.setColor( peakColour );
		graphics.fillRect( x + segmentWidth * 6, y, segmentWidth, h );
	}

	public static void main(String[] args) throws Exception {
		final VUMeter vuMeter = new VUMeter( 800, new Color( 255, 204, 102 ), Color.black, Color.red.darker(), 85 );
		final Frame frame = new Frame( VERSION );
		try {
			frame.addWindowListener( new WindowAdapter() {
				public void windowClosing( WindowEvent w ) {
					vuMeter.stop();
					frame.setVisible( false );
				}
			} );
			frame.setLayout( new BorderLayout() );
			frame.add( vuMeter, BorderLayout.CENTER );
			frame.pack();
			frame.setResizable( false );
			frame.setVisible( true );
			byte[] buffer = new byte[ 2048 ];
			TargetDataLine line = ( TargetDataLine ) AudioSystem.getLine( new Line.Info( TargetDataLine.class ) );
			line.open( new AudioFormat( 44100, 16, 2, true, true ), buffer.length );
			try {
				line.start();
				Thread meterThread = new Thread( vuMeter );
				meterThread.start();
				vuMeter.setValue( 1, 1 );
				Thread.sleep( 1000 );
				while( meterThread.isAlive() ){
					line.read( buffer, 0, buffer.length );
					vuMeter.setValue( getForce( getMaxAmplitude( buffer, 0 ) ), getForce( getMaxAmplitude( buffer, 1 ) ) );
				}
			} finally {
				line.close();
			}
		} finally {
			frame.dispose();
		}
	}
}
