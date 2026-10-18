package com.bistroops;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ImageImporter {

	private static final List<String> FOLDERS = List.of("meal", "promote");

	private static final Map<String, Integer> MAX_WIDTH = Map.of(
			"meal", ImageCompressor.MEAL_WIDTH,
			"promote", ImageCompressor.PROMOTE_WIDTH);

	private static final Path IMAGES = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "static", "images");

	private static final Pattern NO_IN_NAME = Pattern.compile("\\((\\d+)\\)");

	public static void main(String[] args) throws Exception {
		System.out.println("圖片資料夾：" + IMAGES);

		Properties config = loadConfig();
		try (Connection conn = DriverManager.getConnection(
				config.getProperty("spring.datasource.url"),
				config.getProperty("spring.datasource.username"),
				config.getProperty("spring.datasource.password"))) {

			int total = 0;
			for (String table : FOLDERS) {
				total += importFolder(conn, table);
			}
			System.out.println("完成：總共存了 " + total + " 張圖片");
		}
	}

	private static Properties loadConfig() throws IOException {
		Path file = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "application.properties");
		Properties config = new Properties();
		try (Reader reader = new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
			config.load(reader);
		}
		return config;
	}

	private static int importFolder(Connection conn, String table) throws IOException, SQLException {
		Path folder = IMAGES.resolve(table);
		if (!Files.isDirectory(folder)) {
			System.out.println("[" + table + "] 找不到資料夾，跳過：" + folder);
			return 0;
		}

		String sql = "UPDATE " + table + " SET " + table + "_img = ? WHERE " + table + "_no = ?";

		List<Path> files;
		try (Stream<Path> list = Files.list(folder)) {
			files = list.filter(p -> p.getFileName().toString().toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|webp)"))
					.filter(p -> numberOf(p) != null)
					.sorted((a, b) -> Integer.compare(numberOf(a), numberOf(b)))
					.toList();
		}

		conn.setAutoCommit(false);
		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			for (Path file : files) {
				byte[] img = ImageCompressor.compress(readImage(file), MAX_WIDTH.getOrDefault(table, ImageCompressor.MEAL_WIDTH));
				ps.setBytes(1, img);
				ps.setInt(2, numberOf(file));
				ps.addBatch();
			}
			int[] results = ps.executeBatch();
			conn.commit();

			int saved = 0;
			for (int i = 0; i < files.size(); i++) {
				String name = files.get(i).getFileName().toString();
				if (results[i] > 0) {
					saved++;
					System.out.println("[" + table + "] " + name + " → " + table + "_no = " + numberOf(files.get(i)));
				} else {
					System.out.println("[" + table + "] " + name + " → 沒有 " + table + "_no = " + numberOf(files.get(i)) + " 這一筆，沒存");
				}
			}
			System.out.println("[" + table + "] 存了 " + saved + " / " + files.size() + " 張");
			return saved;
		} catch (SQLException e) {
			conn.rollback();
			throw e;
		} finally {
			conn.setAutoCommit(true);
		}
	}

	private static byte[] readImage(Path file) throws IOException {
		try (InputStream in = new BufferedInputStream(new FileInputStream(file.toFile()));
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int len;
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
			}
			return out.toByteArray();
		}
	}

	private static Integer numberOf(Path file) {
		Matcher m = NO_IN_NAME.matcher(file.getFileName().toString());
		return m.find() ? Integer.valueOf(m.group(1)) : null;
	}
}
