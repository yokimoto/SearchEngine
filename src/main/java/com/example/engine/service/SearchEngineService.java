package com.example.engine.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.engine.service.util.SearchEngineFileUtils;

/**
 * 郵便番号検索機能ビジネスロジック
 */
@Service
public class SearchEngineService {

	// 日本郵政の郵便番号ファイルダウンロードURL
	@Value("${settings.zipurl}")
	private String zipUrl;

	// 作業用ディレクトリ
	@Value("${settings.workdir}")
	private String workDir;

	// 日本郵政の郵便番号ファイル（圧縮）
	private static final String ZIP_FILE_NAME = "KEN_ALL.ZIP";
	// 日本郵政の郵便番号ファイル（解凍）
	private static final String CSV_FILE_NAME = "KEN_ALL.CSV";
	// 出力用ファイル
	private static final String OUTPUT_FILE_NAME = "KEN_ALL_OUTPUT.CSV";
	// インデックスファイル
	private static final String INDEX_FILE_NAME = "KEN_ALL_INDEX.CSV";

	// 出力用ファイルヘッダカラム名
	private static final String OUTPUT_CSV_UNIQUE_ID = "uniqueId";
	private static final String OUTPUT_CSV_ZIP_CODE = "zipCode";
	private static final String OUTPUT_CSV_PREFECTUERS = "prefectures";
	private static final String OUTPUT_CSV_DETAIL_ADDRESS_1 = "detailAddress1";
	private static final String OUTPUT_CSV_DETAIL_ADDRESS_2 = "detailAddress2";

	// インデックスファイルヘッダカラム名
	private static final String INPUT_CSV_INDEX_KEY = "indexKey";
	private static final String INPUT_CSV_UNIQUE_IDS = "uniqueIds";

	// 住所検索機能

	/**
	 * 指定された検索ワードからインデックスファイルを元に、 ヒットする住所の結果を返す
	 * 
	 * @param keyword 検索ワード
	 * @return 住所リスト
	 * @throws IOException ファイルアクセスエラー
	 */
	public List<String> searchAddress(String keyword) throws IOException {
		// インデックスファイルのロードとparse→Map
		Map<String, String> indexMap = loadIndex();
		// キーワードのトリムと分割
		String[] splitKeywordArray = splitKeyword(keyword);

		// 分割したキーワードはインデックスにヒットするか、ヒットした場合は住所ごとのユニークIDをリストに保持
		List<String> addressUniqueIdList = getIndexMatchedAddreses(splitKeywordArray, indexMap);

		// 一旦Setに入れることで重複IDの排除
		Set<String> addressUniqueIdSet = new HashSet<>(addressUniqueIdList);
		// 住所ごとのユニークIDのリストから住所リストを作成し、取得する
		List<String> resultAddressList = getResultAddresses(addressUniqueIdSet);

		// 住所ごとのユニークIDで昇順ソート
		Collections.sort(resultAddressList);
		return resultAddressList;
	}

	/**
	 * インデックスファイルを読み込みパースする
	 * 
	 * @return インデックスファイルをparseしたMap
	 * @throws IOException
	 */
	Map<String, String> loadIndex() throws IOException {
		// インデックスファイルのロードとparse→Map
		File indesxCsvData = new File(workDir + INDEX_FILE_NAME);
		CSVParser parser = CSVParser.parse(indesxCsvData, Charset.forName("UTF-8"), CSVFormat.DEFAULT.withFirstRecordAsHeader());
		Map<String, String> indexMap = new HashMap<>();
		for (CSVRecord record : parser) {
			indexMap.put(record.get(INPUT_CSV_INDEX_KEY), record.get(INPUT_CSV_UNIQUE_IDS));
		}
		return indexMap;
	}

	/**
	 * キーワードの空白を除去し、１文字単位の配列に変換
	 * 
	 * @param keyword
	 * @return 変換したString配列
	 */
	String[] splitKeyword(String keyword) {
		return keyword.replaceAll("\\h", "").split("");
	}

	/**
	 * インデックスデータからキーにヒットした住所レコードIDのリストを返す
	 * 
	 * @param splitKeywordArray キーワードを１文字単位にした配列
	 * @param indexMap          parseしたインデックスファイルのMap
	 * @return インデックスにヒットした住所レコードIDのリスト
	 */
	List<String> getIndexMatchedAddreses(String[] splitKeywordArray, Map<String, String> indexMap) {
		// 分割したキーワードはインデックスにヒットするか、ヒットした場合は住所ごとのユニークIDをリストに保持
		List<String> addressUniqueIdList = new ArrayList<>();
		for (String splitKeyword : splitKeywordArray) {
			Set<String> tmpSet = new HashSet<>();
			for (Map.Entry<String, String> entry : indexMap.entrySet()) {
				// キーワードがインデックスに含まれているか
				if (entry.getKey().contains(splitKeyword)) {
					tmpSet.addAll(Arrays.asList(entry.getValue().split(",")));
				}
			}
			if (addressUniqueIdList.size() > 0) {
				// 既に保持されている住所ごとのユニークIDと一致するものを保持（キーワード１文字ごとのAND処理）
				List<String> list = tmpSet.stream().filter(addressUniqueIdList::contains).collect(Collectors.toList());
				addressUniqueIdList.retainAll(list);
			} else {
				// 空の場合はそのまま
				addressUniqueIdList.addAll(tmpSet);
			}
		}
		return addressUniqueIdList;
	}

	/**
	 * インデックスにヒットした住所レコードIDのリストと 出力用ファイルを照会し、キーワードにヒットしたデータを返す
	 * 
	 * @param addressUniqueIdSet インデックスにヒットした住所レコードIDのリスト
	 * @return キーワードにヒットしたデータリスト
	 * @throws IOException ファイルアクセスエラー
	 */
	List<String> getResultAddresses(Set<String> addressUniqueIdSet) throws IOException {
		List<String> resultAddressList = new ArrayList<>();
		if (addressUniqueIdSet.size() > 0) {
			// 出力用ファイルをロードして検索用Mapに格納
			File outputCsvData = new File(workDir + OUTPUT_FILE_NAME);
			CSVParser outputParser = CSVParser.parse(outputCsvData, Charset.forName("UTF-8"), CSVFormat.DEFAULT.withFirstRecordAsHeader());
			Map<String, String> outputMap = new HashMap<>();
			for (CSVRecord record : outputParser) {
				outputMap.put(record.get(OUTPUT_CSV_UNIQUE_ID), "\"" + record.get(OUTPUT_CSV_ZIP_CODE) + "\",\"" + record.get(OUTPUT_CSV_PREFECTUERS)
						+ "\",\"" + record.get(OUTPUT_CSV_DETAIL_ADDRESS_1) + "\",\"" + record.get(OUTPUT_CSV_DETAIL_ADDRESS_2) + "\"");
			}
			// 住所ごとのユニークIDと出力用データとバインドして表示用データを作成
			for (String addressUniqueId : addressUniqueIdSet) {
				if (outputMap.containsKey(addressUniqueId)) {
					resultAddressList.add(outputMap.get(addressUniqueId));
				}
			}
		}
		return resultAddressList;
	}

	// インデックスファイル作成機能

	/**
	 * 住所検索に使うインデックスファイルを作成する インデックスのアルゴリズムはbigramを利用する
	 * 
	 * @throws Exception ファイルアクセスエラー ダウンロードエラー
	 */
	public void createIndexFile() throws Exception {
		// 日本郵政から郵便番号ファイルをダウンロード
		SearchEngineFileUtils.fileDownload(zipUrl, workDir + ZIP_FILE_NAME);
		// ダウンロードした郵便番号ファイルを解凍
		SearchEngineFileUtils.unzip(workDir + ZIP_FILE_NAME, workDir);
		// 出力用ファイルの作成
		makeOutputFile();
		// インデックスファイルの作成
		makeIndexFile();
	}

	/**
	 * 日本郵政の郵便番号ファイルから、出力用のファイルを作成する なお住所詳細が複数レコードにまたがる場合は、マージして１レコードにする
	 * 
	 * @throws IOException ファイルアクセスエラー
	 */
	void makeOutputFile() throws IOException {
		// 郵便番号ファイルのロード、元ファイルはSJIS注意（便宜上必要なものだけヘッダをつける）
		File csvData = new File(workDir + CSV_FILE_NAME);
		CSVParser parser = CSVParser.parse(csvData, Charset.forName("SJIS"), CSVFormat.DEFAULT.withHeader("0", "1", OUTPUT_CSV_ZIP_CODE, "3", "4",
				"5", OUTPUT_CSV_PREFECTUERS, OUTPUT_CSV_DETAIL_ADDRESS_1, OUTPUT_CSV_DETAIL_ADDRESS_2));

		// 出力用ファイルの作成処理
		// 必要なカラムの郵便番号（カラム2）、都道府県（カラム6）、住所詳細１（カラム7）、住所詳細２（カラム8）を抽出
		// その際に住所詳細２は複数レコードにまたがるケースがあるので、マージ処理を行う
		try (FileWriter fw = new FileWriter(workDir + OUTPUT_FILE_NAME)) {
			CSVPrinter printer = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.ALL).withHeader(OUTPUT_CSV_UNIQUE_ID, OUTPUT_CSV_ZIP_CODE,
					OUTPUT_CSV_PREFECTUERS, OUTPUT_CSV_DETAIL_ADDRESS_1, OUTPUT_CSV_DETAIL_ADDRESS_2).print(fw);
			String mergeAddress = "";
			List<CSVRecord> csvDataList = parser.getRecords();
			//
			for (int i = 0; i < csvDataList.size(); i++) {
				CSVRecord currentRecord = csvDataList.get(i);
				if (i == csvDataList.size() - 1) {
					// 最後のレコードはそのままプリント
					printer.printRecord(i + 1, currentRecord.get(OUTPUT_CSV_ZIP_CODE), currentRecord.get(OUTPUT_CSV_PREFECTUERS),
							currentRecord.get(OUTPUT_CSV_DETAIL_ADDRESS_1), mergeAddress + currentRecord.get(OUTPUT_CSV_DETAIL_ADDRESS_2));
				} else {
					// 最後より前のレコードはマージ処理、ここの処理は複雑なのでコメント多め
					// 一つ先のレコードを取得
					CSVRecord nextRecord = csvDataList.get(i + 1);
					// マージするかもしれない住所詳細２の現在のデータを取得
					String currentDetailAddress2 = currentRecord.get(OUTPUT_CSV_DETAIL_ADDRESS_2);
					// マージ対象か解析
					if (analyseMerge(currentRecord, nextRecord, currentDetailAddress2, mergeAddress)) {
						mergeAddress += currentDetailAddress2;
					} else {
						printer.printRecord(i + 1, currentRecord.get(OUTPUT_CSV_ZIP_CODE), currentRecord.get(OUTPUT_CSV_PREFECTUERS),
								currentRecord.get(OUTPUT_CSV_DETAIL_ADDRESS_1), mergeAddress + currentRecord.get(OUTPUT_CSV_DETAIL_ADDRESS_2));
						mergeAddress = "";
					}
				}
			}
		}
	}

	/**
	 * 住所詳細が複数レコードにまたがっているか分析する
	 * 
	 * @param currentRecord         現在のレコード
	 * @param nextRecord            次のレコード
	 * @param currentDetailAddress2 住所詳細
	 * @param mergeAddress          マージ用に保持している住所詳細
	 * @return true 住所詳細が複数レコードにまたがっている false 住所詳細が複数レコードにまたがっていない
	 */
	boolean analyseMerge(CSVRecord currentRecord, CSVRecord nextRecord, String currentDetailAddress2, String mergeAddress) {
		// 基本は郵便番号が同じレコードはマージ対象候補
		// それに加えてデータの傾向から住所に「（」が含んで「）」が含まないものがマージ対象、ただし１レコードで「（」「）」完結しているものはマージ対象外
		// 具体的なデータは郵便番号 6050874 6020847 9380162
		if (currentRecord.get(OUTPUT_CSV_ZIP_CODE).equals(nextRecord.get(OUTPUT_CSV_ZIP_CODE))
				&& (currentDetailAddress2.contains("（") || mergeAddress.contains("（")) && !currentDetailAddress2.contains("）")) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * インデックスファイルを作成する 出力用ファイルからbigramのアルゴリズムで文字列を抽出し、該当する住所レコードのリストを作成 それをファイルに出力する
	 * 
	 * @throws IOException ファイルアクセスエラー
	 */
	void makeIndexFile() throws IOException {
		// 出力用ファイルのロード
		File outputCsvData = new File(workDir + OUTPUT_FILE_NAME);
		CSVParser parser = CSVParser.parse(outputCsvData, Charset.forName("UTF-8"), CSVFormat.DEFAULT.withFirstRecordAsHeader());
		Map<String, String> indexMap = new HashMap<>();
		for (CSVRecord record : parser) {
			// 結合した住所文字列を２文字ごとに切り出し、インデックスとしてMapにキー、値は住所のユニークID格納する
			// 既にキーが存在している場合はカンマ区切りで結合していく
			String address = record.get(OUTPUT_CSV_PREFECTUERS) + record.get(OUTPUT_CSV_DETAIL_ADDRESS_1) + record.get(OUTPUT_CSV_DETAIL_ADDRESS_2);
			for (int i = 0; i <= address.length() - 2; i++) {
				String s = address.substring(i, i + 2);
				if (!indexMap.containsKey(s)) {
					indexMap.put(s, record.get(OUTPUT_CSV_UNIQUE_ID));
				} else {
					indexMap.replace(s, indexMap.get(s) + "," + record.get(OUTPUT_CSV_UNIQUE_ID));
				}
			}
		}

		// インデックスガKey、住所レコードのIDをValueのMapをそれぞれ１カラム目、２カラム目でファイル出力
		try (FileWriter fw = new FileWriter(workDir + INDEX_FILE_NAME)) {
			CSVPrinter printer = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.ALL).withHeader(INPUT_CSV_INDEX_KEY, INPUT_CSV_UNIQUE_IDS).print(fw);
			for (Map.Entry<String, String> entry : indexMap.entrySet()) {
				printer.printRecord(entry.getKey(), entry.getValue());
			}
		}
	}
}
