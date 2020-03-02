package com.creemama.swingreadline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

/**
 * A {@code JFrame} that displays an interactive console with possible readline
 * functionality like tab completion and command history.
 */
public class JReadlineFrame extends JFrame {
	private TextAreaReadline tar;

	public JReadlineFrame(String title) {
		super(title);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	public void run(String[] args, JReadlineFrameModel model) {
		List<String> list = Arrays.asList(args);

		getContentPane().setLayout(new BorderLayout());
		setSize(700, 600);

		JEditorPane text = new JTextPane();

		text.setMargin(new Insets(8, 8, 8, 8));
		text.setCaretColor(new Color(0xa4, 0x00, 0x00));
		text.setBackground(new Color(0xf2, 0xf2, 0xf2));
		text.setForeground(new Color(0xa4, 0x00, 0x00));
		Font font = findFont("Monospaced", Font.PLAIN, 14, new String[] { "Monaco", "Andale Mono" });

		text.setFont(font);
		JScrollPane pane = new JScrollPane();
		pane.setViewportView(text);
		pane.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		getContentPane().add(pane);
		validate();

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

	private Font findFont(String otherwise, int style, int size, String[] families) {
		String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		Arrays.sort(fonts);
		Font font = null;
		for (int i = 0; i < families.length; i++) {
			if (Arrays.binarySearch(fonts, families[i]) >= 0) {
				font = new Font(families[i], style, size);
				break;
			}
		}
		if (font == null) {
			font = new Font(otherwise, style, size);
		}
		return font;
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
