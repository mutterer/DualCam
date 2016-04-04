/**
 * SLightly modified from the "Hello, world!" MM plugin.
 * 
 * Copyright University of California
 * 
 * LICENSE:      This file is distributed under the BSD license.
 *               License text is included with the source distribution.
 *
 *               This file is distributed in the hope that it will be useful,
 *               but WITHOUT ANY WARRANTY; without even the implied warranty
 *               of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 *               IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *               CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *               INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.
 */

import ij.Prefs;
import ij.gui.GenericDialog;

import java.util.List;

import mmcorej.CMMCore;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

import com.github.sarxos.webcam.Webcam;

public class StageCamControlOptions implements MMPlugin {
	public static final String menuName = "Stage Cam Control Options";
	public static final String tooltipDescription = "Displays a dual cam stage control options window";

	// Provides access to the Micro-Manager Java API (for GUI control and high-
	// level functions).
	@SuppressWarnings("unused")
	private ScriptInterface app_;
	// Provides access to the Micro-Manager Core API (for direct hardware
	// control)
	private CMMCore core_;

	@Override
	public void setApp(ScriptInterface app) {
		app_ = app;
		core_ = app.getMMCore();
	}

	@Override
	public void dispose() {
		// We do nothing here as the only object we create, our dialog, should
		// be dismissed by the user.
	}

	@Override
	public void show() {

		int c1 = Prefs.getInt(".twocams.firstcam", 0);
		int c2 = Prefs.getInt(".twocams.secondcam", 0);
		String zStageName = Prefs.getString(".twocams.zstage", "Picard Z Stage");
		String twisterName = Prefs.getString(".twocams.rstage", "Picard Twister");
		int xDir = Prefs.getInt(".twocams.xdir", 1);
		int yDir = Prefs.getInt(".twocams.ydir", 1);
		int zDir = Prefs.getInt(".twocams.zdir", 1);
		int rDir = Prefs.getInt(".twocams.rdir", 1);
		boolean simulation = Prefs.getBoolean(".twocams.simmode", false);

		
			List<Webcam> webcams = Webcam.getWebcams();

			GenericDialog gd = new GenericDialog("StageCamControl Options");
			gd.addMessage("Available webcams");
			for (Webcam webcam : webcams) {
				gd.addMessage("* "+webcam.getName());
			}
			gd.addNumericField("Camera 1 (Front View): ", c1, 0);
			gd.addNumericField("Camera 2 (Side View): ", c2, 0);
			
			gd.addMessage("Stages");
			gd.addStringField("Z Stage name", zStageName, 25);
			gd.addStringField("Twister name", twisterName, 25 );
			
			gd.addMessage("Axis orientations");
			gd.addNumericField("X", xDir, 0);
			gd.addNumericField("Y", yDir, 0);
			gd.addNumericField("Z", zDir, 0);
			gd.addNumericField("Theta", rDir, 0);

			gd.addCheckbox("Simulation mode", simulation);
			gd.showDialog();
			if (gd.wasCanceled())
				return;

			c1 = (int) gd.getNextNumber();
			c2 = (int) gd.getNextNumber();
			zStageName = (String) gd.getNextString();
			twisterName = (String) gd.getNextString();
			xDir = (int) gd.getNextNumber();
			yDir = (int) gd.getNextNumber();
			zDir = (int) gd.getNextNumber();
			rDir = (int) gd.getNextNumber();
			simulation = (boolean) gd.getNextBoolean();
			
			Prefs.set("twocams.firstcam", c1);
			Prefs.set("twocams.secondcam", c2);
			Prefs.set("twocams.xdir", xDir);
			Prefs.set("twocams.ydir", yDir);
			Prefs.set("twocams.zdir", zDir);
			Prefs.set("twocams.rdir", rDir);
			Prefs.set("twocams.zstage", zStageName);
			Prefs.set("twocams.rstage", twisterName);
			Prefs.set("twocams.simmode", simulation);
	}

	@Override
	public String getInfo() {
		return "Displays a nice greeting.";
	}

	@Override
	public String getDescription() {
		return tooltipDescription;
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public String getCopyright() {
		return "IBMP-CNRS, 2016";
	}
}
