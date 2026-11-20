package poc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 技術検証用の最小アプリケーション。 */
@SpringBootApplication
@MapperScan("poc")
public class PocApplication {

  public static void main(String[] args) {
    SpringApplication.run(PocApplication.class, args);
  }
}
