package com.creemama.swingconsole;

import javax.swing.JFrame;

/**
 * A {@link JFrame} that displays an interactive console with possible readline
 * functionality like tab completion and command history.
 */
public class SwingConsoleFrame extends JFrame {
	final static long serialVersionUID = 3746242973444417387L;

	final private SwingConsoleWindow common;

	public SwingConsoleFrame(String title) {
		super(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.common = new SwingConsoleWindow();
	}

	@Override
	public void dispose() {
		common.dispose(getContentPane(), () -> super.dispose(), this);
	}

	public boolean isRunning() {
		return common.isRunning();
	}

	public void run(String[] args, SwingConsoleModel model) {
		common.run(args, getContentPane(), model, getTitle(), this);
	}

	@Override
	public void setVisible(boolean visible) {
		common.setVisible(v -> super.setVisible(v), visible);
	}
}
