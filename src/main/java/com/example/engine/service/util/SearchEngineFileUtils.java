package com.example.engine.service.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

/**
 * 郵便番号検索ファイル共通機能
 */
public class SearchEngineFileUtils {

	/**
	 * ファイルをダウンロードし、ローカルに保存する
	 * 
	 * @param downloadUrl ダウンロードURL
	 * @param destPath    保存先パス
	 * @throws IOException ダウンロードエラー
	 */
	public static void fileDownload(String downloadUrl, String destPath) throws IOException {
		URL url = new URL(downloadUrl);
		File destFile = new File(destPath);
		if (destFile.exists()) {
			destFile.delete();
		}
		FileUtils.copyURLToFile(url, destFile);
	}

	/**
	 * 入力されたzip圧縮ファイルを解凍する
	 * 
	 * @param inputFile 入力ファイル
	 * @param outputDir 出力デジレクトリ
	 * @throws IOException 入出力エラー
	 */
	public static void unzip(String inputFile, String outputDir) throws IOException {
		try (FileInputStream fis = new FileInputStream(inputFile); ZipInputStream archive = new ZipInputStream(fis)) {
			ZipEntry entry = null;
			while ((entry = archive.getNextEntry()) != null) {
				File file = new File(outputDir + entry.getName());

				if (entry.isDirectory()) {
					file.mkdirs();
					continue;
				}

				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}

				try (FileOutputStream fos = new FileOutputStream(file); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
					int size = 0;
					byte[] buf = new byte[1024];
					while ((size = archive.read(buf)) > 0) {
						bos.write(buf, 0, size);
					}
				}
			}
		}
	}

}
