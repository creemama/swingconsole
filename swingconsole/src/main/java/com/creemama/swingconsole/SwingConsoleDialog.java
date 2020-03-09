package com.creemama.swingconsole;

import java.awt.Window;

import javax.swing.JDialog;

/**
 * A {@code JDialog} that displays an interactive console with possible readline
 * functionality like tab completion and command history.
 */
public class SwingConsoleDialog extends JDialog {
	final private static long serialVersionUID = 3746242973444417387L;

	final private SwingConsoleWindow common;

	public SwingConsoleDialog(Window owner, String title) {
		super(owner, title);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
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
