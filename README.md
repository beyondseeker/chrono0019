# chrono0018

## プロジェクト概要
実験用プロジェクトを開始する際のベースとして利用されることを目的としたプロジェクトです。

## 利用法

### ◆ 1. 新規レポジトリの作成
GitHub の Web page にて、空のレポジトリを作成します。

### ◆ 2. 環境変数の定義
必要な情報を git-bash の環境変数に保存します。

```bash
# 新規レポジトリ名の指定
NEW_REPO_NAME=<chronoXXXXとか>

# ローカルでリポジトリ群を配置するディレクトリのパス指定
REPO_ROOT_DIR=</c/tmp/とか>
```

### ◆ 3. 新規レポジトリの内容初期化
新規レポジトリを、chrono0018 の内容を新規リポジトリ名に合わせた内容で初期化します。
環境変数が定義された状態で、下記をコピペで実行してください。

```bash
# ローカルリポジトリを配置するディレクトリに移動
cd $REPO_ROOT_DIR

# chrono0018 を新規リポジトリとして mirror
cd $REPO_ROOT_DIR
git clone --bare git@github.com:beyondseeker/chrono0018.git
cd chrono0018.git
git push --mirror git@github.com:beyondseeker/$NEW_REPO_NAME.git
cd ..
rm -rf chrono0018.git

# 新規リポジトリを clone
git clone git@github.com:beyondseeker/$NEW_REPO_NAME.git
cd $NEW_REPO_NAME

# .git フォルダ以下を除く全文字列について、chrono0018 を $NEW_REPO_NAME に変更。
grep -rl chrono0018 . --exclude-dir=.git | xargs sed -i "s/chrono0018/$NEW_REPO_NAME/g"

# chrono0018 を含むフォルダ名をその部分だけ $NEW_REPO_NAME に変更
find . -type d | grep chrono0018 | while read -r line ; do
    NEW_FOLDER_PATH=`echo "$line" | sed -e "s/chrono0018/$NEW_REPO_NAME/g"`
    mv $line $NEW_FOLDER_PATH
done

# readmeの変更
echo "$NEW_REPO_NAME" > README.md

# 変更内容の git への反映
git add .
git commit -m "rename chrono0018 to $NEW_REPO_NAME"
git push

```

## コード概要

### 基本方針
- 実験用プロジェクトの資料価値を高めるため、Android Studio による自動生成コードを可能な限り保持し、可能な限り差分を作らないようにする。
- 今後のプロジェクトでは build.gradle.kts を用いる予定のため、build.gradle.kts を用いる。

### 変更履歴
- Git repository を init
- Android Studio により Empty Activity project を生成
- *build.gradle* を *build.gradle.kts* に変換

### Android Studio による Android project 作成時の設定
- Project Template
  - Empty Activity
- Configuration
  - Name
    - chrono0018
  - Package name
    - com.objectfanatics.chrono0018
  - Language
    - Kotlin
  - Minimum SDK(*1)
    - API 23

*1: https://twitter.com/minsdkversion に準じる

