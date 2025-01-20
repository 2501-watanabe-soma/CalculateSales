package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_SERIALNUMBER = "売上ファイル名が連番になっていません";
	private static final String TOTAL_AMOUNT_EXCEEDS = "合計金額が10桁を超えました";
	private static final String FILE_INVALID_BRANCH_CODE = "の支店コードが不正です";
	private static final String FILE_INVALID_COMMODITY_CODE = "の商品コードが不正です";
	private static final String SALESFILE_INVALID_FORMAT = "のフォーマットが不正です";

	// 支店定義ファイルと商品定義ファイルの正規表現
	private static final String BRANCH_EXPRESSION = "^[0-9]{3}$";
	private static final String COMMODITY_EXPRESSION = "^[a-zA-Z0-9]{8}$";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// コマンドライン引数チェック(エラー処理3)
		if (args.length != 1 ) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		BufferedReader br = null;
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, "支店", BRANCH_EXPRESSION, branchNames, branchSales)) {
			return;
		}

		// 商品定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_COMMODITY_LST, "商品", COMMODITY_EXPRESSION, commodityNames, commoditySales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		// ファイルパスを指定、格納
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();

		//ファルダ内の売上ファイルの判定、格納
		for (int i = 0; i < files.length; i++) {
			String fileName = files[i].getName();
			// ファイル判定、売上ファイル名一致チェック(エラー処理3)
			if (files[i].isFile() && fileName.matches("^[0-9]{8}.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		// 売上ファイル名連番チェック(エラー処理2-1)
		Collections.sort(rcdFiles);
		for (int i = 0; i < rcdFiles.size() -1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			if((latter - former) != 1) {
				System.out.println(FILE_NOT_SERIALNUMBER);
				return;
			}
		}

		// ファイル数分データを読み込む
		try {
			for (int i = 0; i < rcdFiles.size(); i++) {
				List<String> sale = new ArrayList<>();

				File file = rcdFiles.get(i);
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				// 売上額の加算、格納
				String line;
				while ((line = br.readLine()) != null) {
					sale.add(line);
				}

				// 売上ファイルフォーマットチェック(エラー処理2-4)
				if (sale.size() != 3) {
					System.out.println(file.getName() + SALESFILE_INVALID_FORMAT);
					return;
				}

				// 支店コードの存在チェック(エラー処理2-3)
				if (!branchNames.containsKey(sale.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + FILE_INVALID_BRANCH_CODE);
					return;
				}

				// 商品コードの存在チェック
				if (!commodityNames.containsKey(sale.get(1))) {
					System.out.println(rcdFiles.get(i).getName() + FILE_INVALID_COMMODITY_CODE);
					return;
				}

				// 売上金額が数字かチェック(エラー処理3)
				if (!sale.get(2).matches("^[0-9]*$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				long fileSale = Long.parseLong(sale.get(2));
				Long branchSaleAmount = branchSales.get(sale.get(0)) + fileSale;
				Long commoditySaleAmount = commoditySales.get(sale.get(1)) + fileSale;
				// 売上金額合計の上限チェック(エラー処理2-2)
				if(branchSaleAmount >= 1000000000L || commoditySaleAmount >= 1000000000L) {
					System.out.println(TOTAL_AMOUNT_EXCEEDS);
					return;
				}
				branchSales.put(sale.get(0), branchSaleAmount);
				commoditySales.put(sale.get(1), commoditySaleAmount);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					//ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, "支店別集計", branchNames, branchSales)) {
			return;
		}

		// 商品別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_COMMODITY_OUT, "商品別集計", commodityNames, commoditySales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, String kinds, String expression, Map<String, String> Names,
			Map<String, Long> Sales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			// 支店定義ファイルの存在チェック(エラー処理1-1)
			if(!file.exists()) {
				System.out.println(kinds + FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				// カンマで区切り、itemsに格納
				String[] items = line.split(",");
				// ファイルのフォーマットチェック(エラー処理1-2)
				if((items.length != 2) || (!items[0].matches(expression)) ) {
					System.out.println(kinds + FILE_INVALID_FORMAT);
					return false;
				}
				// ファイルに値を格納
				Names.put(items[0], items[1]);
				Sales.put(items[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, String kinds, Map<String, String> Names,
			Map<String, Long> Sales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			// ファイル作成、書き込み
			for (String key : Names.keySet()) {
				bw.write(key + "," + Names.get(key) + "," + Sales.get(key));
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
