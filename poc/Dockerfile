# P-03：OCI のインスタンス（ARM の場合を含む）で Java 21 のコンテナが起動することを確認する（未実行の草案）
# 事前に `mvn -DskipTests package` で target/hanbai-poc-0.1.0.jar を作成しておく
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/hanbai-poc-0.1.0.jar app.jar
# DB に接続しないため、起動して Java のバージョンとアーキテクチャを表示するだけとする
ENTRYPOINT ["sh", "-c", "java -version && uname -m && echo 'container OK'"]
