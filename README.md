# Taskboard — Spring Boot / MySQL

課題の状態と親子関係を管理するToDoアプリ。

## 機能

- 4列のボード（未対応・処理中・処理済み・完了）
- ドラッグ＆ドロップによる状態変更・並び替えと保存
- 課題の追加・編集・削除、タイトル・ID検索
- 親子課題の関連付け・解除、一覧での子課題展開
- 未完了の子がある親の完了拒否
- 子の再開・追加・移動に伴う、完了済みの親の再開
- 別タブからの更新競合の検知

## 環境

Java 17以上、Maven 3.6.3以上、MySQL 8、デスクトップブラウザー。
アプリはローカルホストで動作します。

## ビルド

```powershell
mvn clean verify
```

アプリが起動中の場合は停止してからビルドしてください。

## Windowsで起動

1. MySQLに `tododb` を用意します。
2. 初回のみ、以下のコマンドで接続設定を保存します。
3. `start-mysql.bat` をダブルクリックします。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-mysql.ps1
```

設定時にJDBC URL、DBユーザー名、パスワードを入力します。
設定はGit対象外の `.local` に保存され、パスワードはWindowsのユーザーに紐づく暗号化形式で保持されます。別PC・別ユーザーでは再設定してください。

画面は [http://127.0.0.1:8081](http://127.0.0.1:8081) で開きます。
停止は起動ウィンドウでEnterを押すか、`stop-mysql.bat` を実行します。
停止用batはこのアプリだけを停止し、MySQLサービスは停止しません。
ログは `.local/mysql-output.log` と `.local/mysql-error.log` に保存されます。

## 環境変数で起動

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/tododb?serverTimezone=Asia/Tokyo'
$env:DB_USER = 'DBユーザー名'
$env:DB_PASS = 'DBパスワード'
java -jar target/todo-list-java-1.0.0.jar --server.port=8081
```

接続URLのTLS等のオプションはMySQL環境に合わせて指定してください。

## 旧版からの移行

起動前にDBをバックアップし、旧アプリを停止してください。
初回起動時にFlywayが旧 `todos` テーブルを移行します。

想定する旧カラムは `id, title, description, due_date, is_completed` です。
履歴のない既存DBをV1として登録し、V2を適用します。新規DBにはV1から適用します。
ID・タイトル・詳細・期限は保持し、完了は `DONE`、未完了は `OPEN` に変換します。
旧 `is_completed` 列は削除されるため、旧アプリとの同時使用はできません。
旧版へ戻す場合はバックアップから復元してください。

## 主な構成

- `BoardController`：画面とJSON API
- `BoardService`：親子制約、完了判定、順序保存、トランザクション
- `Task`：課題データと状態
- `templates/board.html`：ボード・一覧・詳細
- `static/app.js`：画面操作とAPI通信
- `db/migration`：テーブル作成・移行SQL

## 検証

`mvn verify` でH2を使用した12件のテストを実行します。
親子制約、完了・再開、削除、並び替え、競合、API、旧形式データの移行を検証します。

MySQLでも検証する場合は、使い捨ての専用DBを作成し、
`TEST_DB_URL`・`TEST_DB_USER`・`TEST_DB_PASS` を設定して
`mvn -Dtest=BoardIntegrationTest test` を実行してください。
**このテストは課題テーブルを空にするため、通常利用のDBを指定しないでください。**
