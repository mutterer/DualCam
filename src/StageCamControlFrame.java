import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.process.ImageProcessor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mmcorej.CMMCore;
import mmcorej.DeviceType;
import mmcorej.StrVector;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

public class StageCamControlFrame extends JFrame implements Runnable, WebcamListener, WindowListener, UncaughtExceptionHandler {

	private static final long serialVersionUID = 1L;

	private static final int RIGHT = 1;
	private static final int LEFT = 2;
	private static final int UP = 4;
	private static final int DOWN = 8;
	private static final int SMALL = 16;
	private static final int MEDIUM = 32;
	private static final int LARGE = 64;
	private static final int PANEL = 128;

	protected static Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

	private Webcam webcam = null;
	private Webcam webcam2 = null;
	private WebcamPanel panel = null;
	private WebcamPanel panel2 = null;
	private int c1 = 0;
	private int c2 = 0;
	private int xDir = 1;
	private int yDir = 1;
	private int zDir = 1;
	private int rDir = 1;
	
	private int w = 320;
	private int lastCmd;

	private JLabel statusLine = new JLabel("status");
	private JButton startStop = new JButton("Start/Stop");
	private JButton btn2 = new JButton("pos");

	private CMMCore mmc;
	private final ExecutorService zStageExecutor_;

	private String zStageName;
	private String twisterName;

	public static int sdx;
	public static int sdy;
	public static int sdz;
	public static int sdr;

	public static Image front;
	public static Image side;

	public static boolean simulation = false;
	

	public StageCamControlFrame(CMMCore core_) {
		this.mmc = core_;
		zStageExecutor_ = Executors.newFixedThreadPool(1);
	}

	@Override
	public void run() {

		c1 = Prefs.getInt(".twocams.firstcam", 0);
		c2 = Prefs.getInt(".twocams.secondcam", 0);
		zStageName = Prefs.getString(".twocams.zstage", "Picard Z Stage");
		twisterName = Prefs.getString(".twocams.rstage", "Picard Twister");
		xDir = Prefs.getInt(".twocams.xdir", 1);
		yDir = Prefs.getInt(".twocams.ydir", 1);
		zDir = Prefs.getInt(".twocams.zdir", 1);
		rDir = Prefs.getInt(".twocams.rdir", 1);
		simulation = Prefs.getBoolean(".twocams.simmode", false);
		
		
		String path = "/imgs/front.png";
		URL imageUrl = getClass().getResource(path);
		front = new ImageIcon(imageUrl).getImage();
		ImagePlus imp = new ImagePlus("front", front);
		IJ.run(imp, "Flip Horizontally", "");
		IJ.run(imp, "Add...", "value=20");
		side = imp.getBufferedImage();
		
		setTitle("Stage Cam Control");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;

		addWindowListener(this);

		MouseMotionListener customCursors = new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {

				int cmd = 0;
				WebcamPanel p = (WebcamPanel) e.getSource();
				if (p.getName() == "Side")
					cmd += PANEL;
				double d = Math.sqrt((e.getX() - p.getWidth() / 2) * (e.getX() - p.getWidth() / 2)
						+ (e.getY() - p.getHeight() / 2) * (e.getY() - p.getHeight() / 2));
				if (d > 100) {
					cmd += LARGE;
				} else if (d > 50) {
					cmd += MEDIUM;
				} else if (d > 20) {
					cmd += SMALL;
				} else
					cmd = 0;
				if (Math.abs(e.getX() - p.getWidth() / 2) < 20) {
					// on est proche de l'axe vertical
					if (e.getY() > p.getHeight() / 2) {
						cmd += DOWN;
					} else if (e.getY() < p.getHeight() / 2) {
						cmd += UP;
					}
				} else if (Math.abs(e.getY() - p.getHeight() / 2) < 20) {
					// on est proche de l'axe horizontal
					if (e.getX() > p.getWidth() / 2) {
						cmd += RIGHT;
					} else if (e.getX() < p.getWidth() / 2) {
						cmd += LEFT;
					}

				}

				if ((d <= 20)
						|| ((Math.abs(e.getX() - p.getWidth() / 2) > 20) && (Math.abs(e.getY() - p.getHeight() / 2) > 20)))
					cmd = 0;

				if (cmd != lastCmd) {
					lastCmd = cmd;
					switchCursor((JPanel) p, cmd);
				}

			}

			@Override
			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		};

		webcam = Webcam.getWebcams().get(c1);
		webcam2 = Webcam.getWebcams().get(c2);

		if (webcam == null) {
			System.out.println("No webcams found...");
			return;
		}

		Dimension d = new Dimension(w, (int) (WebcamResolution.VGA.getSize().getHeight() * w / WebcamResolution.VGA
				.getSize().getWidth()));
		webcam.setViewSize(d);
		webcam2.setViewSize(d);

		webcam.addWebcamListener(StageCamControlFrame.this);
		webcam2.addWebcamListener(StageCamControlFrame.this);

		panel = new WebcamPanel(webcam, d, false);
		panel.setName("Front");
		panel.setPainter(new MyPainter());

		panel.setFitArea(true);
		panel.addMouseMotionListener(customCursors);

		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				processCommand(lastCmd);
			}
		});

		panel2 = new WebcamPanel(webcam2, d, false);
		panel2.setName("Side");
		panel2.setPainter(new MyPainter());

		panel2.setFitArea(true);

		panel2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				processCommand(lastCmd);
			}
		});
		panel2.addMouseMotionListener(customCursors);
		
		panel2.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent we) {
				int r = we.getWheelRotation();
				double step = we.isAltDown()?1.0:5.0;
				if ( simulation ) {
					sdr -= step*r*rDir;
				} else
				setRelativeStagePosition(twisterName,step*r*rDir);
			}
		});
		
		panel.addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent we) {
				int r = we.getWheelRotation();
				double step = we.isAltDown()?1.0:5.0;
				if ( simulation ) {
					sdr -= step*r*rDir;
				} else
				setRelativeStagePosition(twisterName,step*r*rDir);
			}
		});

		
		startStop.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				if (panel.isStarted()) {
					panel.stop();
					panel2.stop();
				} else {
					panel.start();
					panel2.start();
				}
			}
		});
		
		btn2.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Point2D.Double pos = mmc.getXYStagePosition();
					IJ.log("X: "+pos.x);
					IJ.log("Y: "+pos.y);
				} catch (Exception e1) {
					IJ.error("Error getting XY stage position");
				}
				try {
					double z = mmc.getPosition(zStageName);
					IJ.log("Z: "+z);
				} catch (Exception e1) {
					IJ.error("Error getting Z stage position");
				}
				try {
					double z = mmc.getPosition(twisterName);
					IJ.log("R: "+z);
				} catch (Exception e1) {
					IJ.error("Error getting Theta stage position");
				}
			}
		});
		
		c.gridx = 0;
		c.gridy = 0;
		add(new JLabel("Front View"), c);
		c.gridx = 1;
		c.gridy = 0;
		add(new JLabel("Side View"), c);
		c.gridx = 0;
		c.gridy = 1;
		add(panel, c);
		c.gridx = 1;
		c.gridy = 1;
		add(panel2, c);

		c.gridx = 0;
		c.gridy = 3;
		add(statusLine, c);

		c.gridx = 0;
		c.gridy = 2;
		JPanel bp = new JPanel();
		bp.add(startStop);
		bp.add(btn2);

		add(bp, c);

		pack();
		setVisible(true);

		// setup Z and theta axis
		StrVector zDrives = mmc.getLoadedDevicesOfType(DeviceType.StageDevice);
		for (int i = 0; i < zDrives.size(); i++) {
			String drive = zDrives.get(i);
		}

		Thread t = new Thread() {

			@Override
			public void run() {
				panel.start();
				panel2.start();
			}
		};
		t.setName("example-starter");
		t.setDaemon(true);
		t.setUncaughtExceptionHandler(this);
		t.start();
	}

	public void processCommand(int c) {
		// decode command and take actions here
		String debug = "";
		// panel
		if ((c & PANEL) != 0) {
			debug += "Side:";
		} else
			debug += "Front:";

		// direction
		if ((c & LEFT) != 0)
			debug += "LEFT:";
		else if ((c & RIGHT) != 0)
			debug += "RIGHT:";
		else if ((c & UP) != 0)
			debug += "UP:";
		else if ((c & DOWN) != 0)
			debug += "DOWN:";
		// speed
		if ((c & SMALL) != 0)
			debug += "SMALL";
		else if ((c & MEDIUM) != 0)
			debug += "MEDIUM";
		else if ((c & LARGE) != 0)
			debug += "LARGE";
		statusLine.setText(debug);
		double step = (((c & SMALL) > 0) ? 1 : 0) * 1.0 + (((c & MEDIUM) > 0) ? 1 : 0) * 10
				+ (((c & LARGE) > 0) ? 1 : 0) * 100;
		double dx = 0;
		if ((c&PANEL)==0) dx = (((c & LEFT) > 0) ? 1 : 0) * step - (((c & RIGHT) > 0) ? 1 : 0) * step;
		double dy = (((c & UP) > 0) ? 1 : 0) * step - (((c & DOWN) > 0) ? 1 : 0) * step;
		double dz = 0;
		if ((c&PANEL)!=0) dz = (((c & LEFT) > 0) ? 1 : 0) * step - (((c & RIGHT) > 0) ? 1 : 0) * step;
		if (dz!=0) {
			if ( simulation ) {
				sdz -= dz*zDir;
			} else
			setRelativeStagePosition(zStageName, dz*zDir);
		}
		else { 
			if ( simulation ) {
				sdx -= dx*xDir;
				sdy -= dy*yDir;
			} else 
			setRelativeXYStagePosition(dx*xDir, dy*yDir);
		}
	}

	private void switchCursor(JPanel p, int cmd) {

		int cursor = cmd & ~PANEL; // get rid of the panel flag
		if (cursor == 0) {
			p.setCursor(defaultCursor);

		} else {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			String path = "/imgs/cursor" + cursor + ".png";
			URL imageUrl = getClass().getResource(path);

			ImageIcon icon = new ImageIcon(imageUrl);
			Image image = icon.getImage();
			if (image == null) {
				IJ.error("img null");
				return;
			} else {
				int width = icon.getIconWidth();
				int height = icon.getIconHeight();
				Point hotSpot = new Point(width / 2, height / 2);
				Cursor crosshairCursor = toolkit.createCustomCursor(image, hotSpot, "custom");
				p.setCursor(crosshairCursor);
			}
		}
	}

	private void setRelativeXYStagePosition(double x, double y) {
		try {
			if (!mmc.deviceBusy(mmc.getXYStageDevice()))
				mmc.setRelativeXYPosition(mmc.getXYStageDevice(), x, y);

		} catch (Exception e) {
			IJ.error(e.toString());
		}
	}

	private void setRelativeStagePosition(String drive, double z) {
		try {
			if (!mmc.deviceBusy(drive)) {
				StageThread st = new StageThread(drive, z);
				zStageExecutor_.execute(st);
			}
		} catch (Exception ex) {
			IJ.error(ex.toString());
		}
	}

	private class StageThread implements Runnable {
		final String drive;
		final double z_;

		public StageThread(String d, double z) {
			drive = d;
			z_ = z;
		}

		public void run() {
			try {
				sdz += z_;
				mmc.waitForDevice(drive);
				mmc.setRelativePosition(drive, z_);
				mmc.waitForDevice(drive);
			} catch (Exception ex) {
				IJ.error(ex.toString());
			}
		}
	}

	@Override
	public void webcamOpen(WebcamEvent we) {
		System.out.println("webcam open");
	}

	@Override
	public void webcamClosed(WebcamEvent we) {
		System.out.println("webcam closed");
	}

	@Override
	public void webcamDisposed(WebcamEvent we) {
		System.out.println("webcam disposed");
	}

	@Override
	public void webcamImageObtained(WebcamEvent we) {
		// do nothing
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowClosed(WindowEvent e) {
		webcam.close();
		webcam2.close();
	}

	@Override
	public void windowClosing(WindowEvent e) {
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		System.out.println("webcam viewer resumed");
		panel.resume();
	}

	@Override
	public void windowIconified(WindowEvent e) {
		System.out.println("webcam viewer paused");
		panel.pause();
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		System.err.println(String.format("Exception in thread %s", t.getName()));
		e.printStackTrace();
	}

	private static class MyPainter implements WebcamPanel.Painter {
		BufferedImage img;

		@Override
		public void paintImage(WebcamPanel p, BufferedImage image, Graphics2D g2) {
			img = image;
			if (simulation==true) {
				if (p.getName() == "Side") {
					g2.drawImage(side, 0, 0, null);
					g2.setColor(Color.GREEN);
					Stroke sampleHolder = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 3 }, 0);
					g2.setStroke(sampleHolder);
					for (int i = 0; i<10; i++)
					g2.drawLine(p.getWidth() / 2+sdz+i, -20+i*2+sdr%6, p.getWidth() / 2 + sdz+i, p.getHeight() / 2+sdy);
				} else {
					g2.drawImage(front, 0, 0, null);
					g2.setColor(Color.GREEN);
					Stroke sampleHolder = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 3 }, 0);
					g2.setStroke(sampleHolder);
					for (int i = 0; i<10; i++)
					g2.drawLine(p.getWidth() / 2+sdx+i, -20+i*2+sdr%6, p.getWidth() / 2 + sdx+i, p.getHeight() / 2+sdy);

					
				}			
			}
			else {
				g2.drawImage(image, 0, 0, null);
			}

			Font font = p.getFont();
			FontMetrics metrics = g2.getFontMetrics(font);

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setFont(font);
			g2.setColor(Color.YELLOW);
			g2.drawString("+Y", p.getWidth() / 2, metrics.getHeight());
			g2.drawString("-Y", p.getWidth() / 2, p.getHeight() - metrics.getHeight());
			if (p.getName() == "Side") {
				g2.drawString("-Z", 20, p.getHeight() / 2);
				g2.drawString("+Z", p.getWidth() - 20, p.getHeight() / 2);
			} else {
				g2.drawString("-X", 20, p.getHeight() / 2);
				g2.drawString("+X", p.getWidth() - 20, p.getHeight() / 2);

			}
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

			Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 2 }, 0);
			g2.setStroke(dashed);
			g2.setColor(Color.YELLOW);
			g2.drawLine(10, p.getHeight() / 2, p.getWidth() / 2 - 10, p.getHeight() / 2);
			g2.drawLine(p.getWidth() / 2 + 10, p.getHeight() / 2, p.getWidth() - 10, p.getHeight() / 2);
			g2.drawLine(p.getWidth() / 2, 10, p.getWidth() / 2, p.getHeight() / 2 - 10);
			g2.drawLine(p.getWidth() / 2, p.getHeight() / 2 + 10, p.getWidth() / 2, p.getHeight() - 10);

		}

		@Override
		public void paintPanel(WebcamPanel p, Graphics2D g2) {
			if (img != null)
				g2.drawImage(img, 0, 0, null);
			else
				p.getDefaultPainter().paintPanel(p, g2);
		}

	}

}
