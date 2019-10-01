# SearchEngine
### 概要（General）
* 日本郵政の郵便番号データをソースに地名から検索して一覧を表示する
* インデックスはbigramを利用
* SpringBoot 2.1.8で構築

### 実行時資料（How to Use）
* 必要なもの（requirement）
  * java1.8(JDK) or higher
  * maven3.5 or higher
  * linux or MacOS
* 実行手順（getting start）
  * git clone(or download)
 ```
 git clone https://github.com/yokimoto/SearchEngine.git
 ```
  * 設定値の変更（linux or Macでの例）  
 ```
 $ vi src/main/resources/application.yml
 settings:
  zipurl: https://www.post.japanpost.jp/zipcode/dl/kogaki/zip/ken_all.zip
  workdir: ここに上記クローンしたディレクトリを設定（/SearchEngine/のように/で終わるようにする）
 ```
 * ビルド
```
$ mvn clean package
```
 * 起動
```
$ cd target
$ java -jar SearchEngine-0.0.1-SNAPSHOT.jar
```
 * ブラウザで確認（http://localhost:9000/）
 * 初回は「インデックスの再作成」ボタンでファイルの作成を行ってください
 * 作成後は地名を入力することで、ヒットする住所の一覧が表示されます
 
### 技術説明資料（Internals）
#### 機能概要
* インデックスファイル作成
* 住所検索

#### ディレクトリ構成
```
SearchEngine/
　├ src/
　│　└ main/
　│　　　├ java/ javaソースコード
　│　　　└ resources/
　│　　　　　└ templates/ htmlファイル（thymeleafテンプレート）
　│　　　　　└ application.yml アプリケーション設定ファイル
　│　└ test/ javaテストコード
　│
　├ target/ classファイル
　├ mvnw maven実行ファイル
　├ mvnw.cmd maven実行ファイル（win）
　└ pom.xml mavenの依存関係ファイル
```
#### 内部仕様
 * インデックスファイル作成機能
   * 日本郵政のサイト（設定ファイルで指定）からzipファイルをダウンロード
   * zipファイルを解凍する
   * 出力用ファイル（KEN_ALL_OUTPUT.CSV）を作成
     * 解凍したcsvファイルを読み込み、必要なカラムの「郵便番号、都道府県、住所詳細１、住所詳細２」を取得する（元ファイルSJISなので明示的に文字コードを指定）
     * 住所詳細２はレコードを横断して出力されるため、そのレコードのマージを行う。具体的には順次ループで読み込んでいき、同じ郵便番号のレコードの住所詳細２をマージする。その際にさらに厳密に見るため、「（」で始まっているか、「）」で終わっているかも判断の一つとする。（データの規則性から判断）
	 * 上記のカラムに加えてインデックス用にマージしたレコードごとの独自のユニークIDを１カラム目に付与する
   * インデックスファイル（KEN_ALL_INDEX.CSV）を作成
     * 作成した出力用ファイルを読み込み、bigramのアルゴリズムに基づいてファイルを作成する
	 * 具体的には都道府県、住所詳細１、住所詳細２を連結し、２文字単位でインデックスを作成する、他のレコードでもそのインデックスにヒットした場合は、ファイルの２カラム目に出力用ファイルの独自のユニークIDを、カンマ区切りで設定する（実装はMapのキーがインデックス文字列、値が独自ユニークIDのカンマ区切り文字列、詳細は下記ファイル仕様を参照）
	 * 作成したインデックスをインデックスファイル（KEN_ALL_INDEX.CSV）として出力して終了
 * 住所検索機能
   * 検索画面で設定したキーワードを１文字単位に分割する、その際に空白はトリムしておく
   * インデックスファイルを読み込みMapに格納する（インデックスMapと呼称する）
   * キーワードを分割した文字を１文字ずつインデックスMapのキーと照らし合わせ、含まれていた場合は独自ユニークIDのカンマ区切り文字列を保持する
   * それをすべての文字で繰り返し、サマリーした独自ユニークIDのリストから、出力用ファイルで照会し、出力するレコードのリストを返却する
   * ソート順は郵便番号の昇順とする
 * 中間ファイル仕様(KEN_ALL_OUTPUT.CSV)
  ```
"1","0600000","北海道","札幌市中央区","以下に掲載がない場合"
"2","0640941","北海道","札幌市中央区","旭ケ丘"
"3","0600041","北海道","札幌市中央区","大通東"
"4","0600042","北海道","札幌市中央区","大通西（１〜１９丁目）"
  ```
  元のCSVに「独自のユニークID,郵便番号,都道府県,住所詳細１,住所詳細２」で抽出したファイル。独自のユニークIDをキーに、インデックスファイルとデータマージして出力時に利用する。
  
 * 中間ファイル仕様(KEN_ALL_INDEX.CSV)
  ```
"信友","36865,61425"
"中袖","35894"
"醐御","81666,81667,81668,81669,81694"
"野堤","16360"
  ```
  KEN_ALL_OUTPUT.CSVの都道府県、住所詳細１、住所詳細２からbigramでインデックスを作成し、その文字列にヒットする「独自のユニークID」をカンマ区切りで出力。検索時に利用する。

#### その他
 * 設定ファイル仕様(application.yml)
 
```
server:
  port: 9000 ←アクセスポート

spring:
  thymeleaf:
    enabled: true thymeleafを使用するかどうか
    cache: false ←キャッシュを使用するかどうか
    
settings:
  zipurl: https://www.post.japanpost.jp/zipcode/dl/kogaki/zip/ken_all.zip ←日本郵政の住所ファイルダウンロードURL
  workdir: /Users/yokimoto/sts/workspace-spring-tool-suite-4-4.0.2.RELEASE/SearchEngine/ ←workディレクトリ、デフォルトはgitから取得したrootディレクトリを指定する
```
