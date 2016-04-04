/**
 * SLightly modified from the "Hello, world!" MM plugin.
 * 
 */

import javax.swing.SwingUtilities;

import mmcorej.CMMCore;

import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

public class StageCamControl implements MMPlugin {
	public static final String menuName = "Stage Cam Control";
	public static final String tooltipDescription = "Displays a dual cam stage control window";

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

		SwingUtilities.invokeLater(new StageCamControlFrame(core_));

	}

	@Override
	public String getInfo() {
		return "Displays a simple greeting.";
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
