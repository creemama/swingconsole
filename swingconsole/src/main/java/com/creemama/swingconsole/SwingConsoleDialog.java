package com.creemama.swingconsole;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 * A {@code JDialog} that displays an interactive console with possible readline
 * functionality like tab completion and command history.
 */
public class SwingConsoleDialog extends JDialog {
	private TextAreaReadline tar;

	public SwingConsoleDialog(Window owner, String title) {
		super(owner, title);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}

	public void run(String[] args, SwingConsoleModel model) {
		// NOTE: If you update this method, also update SwingConsoleFrame#run.

		List<String> list = Arrays.asList(args);

		JEditorPane text = new JTextPane();
		text.setBackground(new Color(0xf2, 0xf2, 0xf2));
		text.setCaretColor(new Color(0xa4, 0x00, 0x00));
		Font font = SwingConsoleUtil.findFont("Monospaced", Font.PLAIN, 14, new String[] { "Monaco", "Andale Mono" });
		text.setFont(font);
		text.setForeground(new Color(0xa4, 0x00, 0x00));
		text.setMargin(new Insets(8, 8, 8, 8));

		JScrollPane pane = new JScrollPane();
		pane.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		pane.setViewportView(text);

		setSize(700, 600);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(pane, BorderLayout.CENTER);

		tar = new TextAreaReadline(text, " Welcome to the " + getTitle() + " \n\n");

		model.setUp(list, tar);

		Thread swingConsoleThread = new Thread(() -> {
			setVisible(true);
			model.run(tar);
		}, getTitle());
		swingConsoleThread.setDaemon(true);
		swingConsoleThread.start();
		try {
			swingConsoleThread.join();
		} catch (InterruptedException ie) {
			// Ignore.
		}

		dispose();
	}

	@Override
	public void dispose() {
		// Since tar.shutdown could take a few seconds, let us hide the window if it is
		// not already hidden.
		setVisible(false);
		if (tar != null)
			tar.shutdown();
		super.dispose();
	}

	private static final long serialVersionUID = 3746242973444417387L;
}