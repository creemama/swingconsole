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
		setDefaultCloseOperation(SwingConsoleWindow.DEFAULT_CLOSE_OPERATION);
		setSize(SwingConsoleWindow.DEFAULT_SIZE);
		this.common = new SwingConsoleWindow();
	}

	@Override
	public void dispose() {
		common.dispose(getContentPane(), () -> super.dispose(), this);
	}

	public boolean isRunning() {
		return common.isRunning();
	}

	public void run(SwingConsole runnable) {
		run(runnable, true);
	}

	public void run(SwingConsole runnable, boolean visible) {
		common.run(getContentPane(), runnable, getTitle(), visible, this);
	}

	@Override
	public void setVisible(boolean visible) {
		common.setVisible(v -> super.setVisible(v), visible);
	}
}
