/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.swt.ResourceManager;
import org.junit.jupiter.api.Test;

/**
 * Tests the decoration of images. The expected images are composed the way the helper did it originally.
 */
class ResourceManagerTest {
	@Test
	void decorateImage() throws Exception {
		UI.run(() -> {
			Display display = Display.getCurrent();
			Image base = new Image(display, createImageData(16, 16, new RGB(200, 0, 0)));
			Image decorator = new Image(display, createImageData(7, 5, new RGB(0, 0, 200)));
			for (int corner : new int[] { ResourceManager.TOP_LEFT, ResourceManager.TOP_RIGHT, ResourceManager.BOTTOM_LEFT,
					ResourceManager.BOTTOM_RIGHT }) {
				ImageData actual = ResourceManager.decorateImage(base, decorator, corner).getImageData();
				Image expectedImage = expected(base, decorator, corner).createImage();
				ImageData expected = expectedImage.getImageData();
				assertEquals(expected.width, actual.width);
				assertEquals(expected.height, actual.height);
				for (int x = 0; x < expected.width; x++) {
					for (int y = 0; y < expected.height; y++) {
						assertEquals(expected.palette.getRGB(expected.getPixel(x, y)),
								actual.palette.getRGB(actual.getPixel(x, y)), "corner " + corner + " at " + x + "/" + y);
						assertEquals(expected.getAlpha(x, y), actual.getAlpha(x, y), "corner " + corner + " at " + x + "/" + y);
					}
				}
				expectedImage.dispose();
			}
		});
	}

	private static ImageData createImageData(final int width, final int height, final RGB color) {
		ImageData data = new ImageData(width, height, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		int pixel = data.palette.getPixel(color);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				data.setPixel(x, y, pixel);
			}
		}
		return data;
	}

	/**
	 * Composes the image as the helper did it originally (with the deprecated drawImage(ImageData, ...)).
	 */
	@SuppressWarnings("deprecation")
	private static CompositeImageDescriptor expected(final Image base, final Image decorator, final int corner) {
		return new CompositeImageDescriptor() {
			@Override
			protected void drawCompositeImage(final int width, final int height) {
				int bw = base.getBounds().width;
				int bh = base.getBounds().height;
				int dw = decorator.getBounds().width;
				int dh = decorator.getBounds().height;
				drawImage(base.getImageData(), 0, 0);
				if (corner == ResourceManager.TOP_LEFT) {
					drawImage(decorator.getImageData(), 0, 0);
				}
				else if (corner == ResourceManager.TOP_RIGHT) {
					drawImage(decorator.getImageData(), bw - dw, 0);
				}
				else if (corner == ResourceManager.BOTTOM_LEFT) {
					drawImage(decorator.getImageData(), 0, bh - dh);
				}
				else {
					drawImage(decorator.getImageData(), bw - dw, bh - dh);
				}
			}

			@Override
			protected Point getSize() {
				return new Point(base.getBounds().width, base.getBounds().height);
			}
		};
	}
}
