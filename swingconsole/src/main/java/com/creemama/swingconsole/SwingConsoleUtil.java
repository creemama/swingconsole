/**
 * 
 */
package com.creemama.swingconsole;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

/**
 * Helper methods common between {@link SwingConsoleDialog} and
 * {@link SwingConsoleFrame}.
 */
public class SwingConsoleUtil {
	public static Font findFont(String otherwise, int style, int size, String[] families) {
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
}
