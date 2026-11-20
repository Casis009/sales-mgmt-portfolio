package jp.co.hikari.hanbai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 販売管理システムの起動クラス。 */
@SpringBootApplication
@EnableScheduling
public class HanbaiApplication {

  /**
   * アプリケーションを起動する。
   *
   * @param args 起動引数
   */
  public static void main(String[] args) {
    SpringApplication.run(HanbaiApplication.class, args);
  }
}
