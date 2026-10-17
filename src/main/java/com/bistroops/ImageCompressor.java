package com.bistroops;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/*
 * 上傳圖片時先「縮小＋壓縮」再存進資料庫
 */
public final class ImageCompressor {

	public static final int MEAL_WIDTH = 1400;
	public static final int PROMOTE_WIDTH = 1600;
	private static final float QUALITY = 0.8f;

	private ImageCompressor() {
	}   // 只有 static 方法的工具類別，不讓別人 new

	public static byte[] compress(byte[] original, int maxWidth) throws IOException {
		BufferedImage src;
		try {
			src = ImageIO.read(new ByteArrayInputStream(original));
		} catch (IOException e) {
			return original;                                     // 讀不懂（壞掉的檔案、特殊的 JPEG）：照舊存
		}
		if (src == null) return original;                        // 不支援的格式（例如 webp）：照舊存

		int orientation = exifOrientation(original);
		BufferedImage img = rotate(src, orientation);           // 照 EXIF 轉正

		int w = img.getWidth(), h = img.getHeight();
		boolean resized = w > maxWidth;
		if (resized) {
			h = Math.round(h * (float) maxWidth / w);            // 等比例：高度跟著寬度縮
			w = maxWidth;
		}
		byte[] jpg = toJpeg(scale(img, w, h));

		if (orientation == 1 && jpg.length >= original.length) return original;   // 壓完反而比較大（原本就很小）：存原檔
		return jpg;
	}

	// 縮小：一次縮太多會有鋸齒，所以先每次縮一半，接近目標再縮到剛好（畫質比較好）
	private static BufferedImage scale(BufferedImage img, int w, int h) {
		BufferedImage cur = img;
		while (cur.getWidth() / 2 >= w) {
			cur = draw(cur, cur.getWidth() / 2, cur.getHeight() / 2);
		}
		return draw(cur, w, h);
	}

	// 畫到一張新的 RGB 圖上（白底，透明的地方會變白）
	private static BufferedImage draw(BufferedImage src, int w, int h) {
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, w, h);
		g.drawImage(src, 0, 0, w, h, null);
		g.dispose();
		return out;
	}

	// 存成 JPEG，用 QUALITY 的品質（ImageIO.write 不能設定品質，所以用 ImageWriter）
	private static byte[] toJpeg(BufferedImage img) throws IOException {
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ImageWriteParam param = writer.getDefaultWriteParam();
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(QUALITY);
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
			writer.setOutput(out);
			writer.write(null, new IIOImage(img, null, null), param);
		} finally {
			writer.dispose();
		}
		return bytes.toByteArray();
	}

	// 照 EXIF 的方向轉正：3＝倒過來（轉 180 度）、6＝順時針轉 90 度、8＝逆時針轉 90 度；其他（1＝正的）不轉
	private static BufferedImage rotate(BufferedImage src, int orientation) {
		int degree = switch (orientation) {
			case 3 -> 180;
			case 6 -> 90;
			case 8 -> 270;
			default -> 0;
		};
		if (degree == 0) return src;
		int w = src.getWidth(), h = src.getHeight();
		boolean swap = degree != 180;                             // 轉 90 度：寬高對調
		BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.translate(out.getWidth() / 2.0, out.getHeight() / 2.0);  // 以中心點轉
		g.rotate(Math.toRadians(degree));
		g.translate(-w / 2.0, -h / 2.0);
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return out;
	}

	// 讀 JPEG 檔頭裡 EXIF 的「方向」（tag 0x0112）；不是 JPEG、沒有 EXIF 或讀不懂都當成 1（正的）
	//   JPEG 由一段一段組成，每段開頭是 FF 加一個代號：APP1（FF E1）放 EXIF，SOS（FF DA）之後就是影像本身
	private static int exifOrientation(byte[] b) {
		try {
			if ((b[0] & 0xFF) != 0xFF || (b[1] & 0xFF) != 0xD8) return 1;           // 不是 JPEG
			int i = 2;
			while ((b[i] & 0xFF) == 0xFF) {
				int marker = b[i + 1] & 0xFF;
				int len = ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
				if (marker == 0xDA) break;                                            // 影像開始了，後面沒有 EXIF
				if (marker == 0xE1 && b[i + 4] == 'E' && b[i + 5] == 'x' && b[i + 6] == 'i' && b[i + 7] == 'f') {
					int tiff = i + 10;                                                 // "Exif\0\0" 後面是 TIFF 格式
					boolean little = b[tiff] == 'I';                                   // II＝小端序、MM＝大端序
					int ifd = tiff + read(b, tiff + 4, 4, little);
					int count = read(b, ifd, 2, little);
					for (int k = 0; k < count; k++) {
						int entry = ifd + 2 + k * 12;                                  // 每一筆 12 bytes
						if (read(b, entry, 2, little) == 0x0112) return read(b, entry + 8, 2, little);
					}
					return 1;
				}
				i += 2 + len;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			// 檔案太短或格式怪：當成正的
		}
		return 1;
	}

	private static int read(byte[] b, int at, int size, boolean little) {
		int v = 0;
		for (int k = 0; k < size; k++) {
			int by = b[at + (little ? size - 1 - k : k)] & 0xFF;
			v = (v << 8) | by;
		}
		return v;
	}
}
