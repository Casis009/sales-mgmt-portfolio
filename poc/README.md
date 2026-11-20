# poc: 기술 검증(PoC)

> ⚠ **이 폴더의 코드는 아직 한 번도 실행하지 않은 초안이에요.**
> 작성 환경에서 Maven Central에 접속할 수 없어서 빌드·실행을 못 했어요. 라이브러리 좌표나 API 이름이 Spring Boot 4.1 / JasperReports 7에서 달라져 있을 수 있으니, 실행해서 에러가 나면 메시지를 그대로 Claude에게 붙여 주세요. 고친 내용은 `docs/ja/03_詳細設計/01_技術検証結果報告書.md`에 기록해요.

## 검증 항목

| No | 검증 내용 | 파일 | 실행 방법 |
|---|---|---|---|
| P-01 | MyBatis가 Spring Boot 4.1에서 동작 | `MyBatisPocTest` | `mvn -Dtest=MyBatisPocTest test` |
| P-02 | JasperReports로 일본어 PDF 출력(IPAex 폰트) | `JasperPdfPocTest` | `mvn -Dtest=JasperPdfPocTest test` → `target/poc.pdf`를 열어 글자 깨짐 확인 |
| P-03 | OCI 인스턴스에서 Docker 이미지 동작 | `Dockerfile` | 아래 「P-03」 |
| P-04 | Testcontainers가 GitHub Actions에서 동작 | `.github-sample/poc.yml` | 아래 「P-04」 |
| P-05 | Spring Batch 비동기 기동·실행 상황 조회 | (코드 없음) | P04 1단계(개발 환경 구축)에서 PC로 실행하면서 작성 |

## 준비
1. JDK 21, Maven 3.9, Docker Desktop 설치
2. IPAex 고딕 폰트(`ipaexg.ttf`)를 IPA 공식 사이트에서 내려받아 `src/main/resources/fonts/`에 둔다(라이선스상 저장소에 넣어도 되지만 용량 때문에 PoC에서는 직접 받는 방식).
3. `pom.xml`의 버전(`spring-boot`, `mybatis-starter`, `jasperreports`)을 그 시점 최신 패치로 맞춘다.

## P-03: OCI에서 이미지 동작
```bash
# 서버에서 CPU 아키텍처 확인
uname -m          # aarch64 → ARM, x86_64 → 일반

# PC에서 멀티 아키텍처 이미지 빌드(ARM 서버인 경우)
docker buildx build --platform linux/amd64,linux/arm64 -t ghcr.io/<계정>/hanbai-poc:0.1 --push .

# 서버에서 실행
docker run --rm ghcr.io/<계정>/hanbai-poc:0.1
```

## P-04: GitHub Actions
`.github-sample/poc.yml`을 저장소의 `.github/workflows/poc.yml`로 복사해 push하고, Actions 탭에서 테스트가 통과하는지 확인한다.

## 결과 기록
실행 결과(성공/실패, 고친 점, 채택한 버전)를 알려 주면 `01_技術検証結果報告書.md`의 「結果」 칸을 채워요.
