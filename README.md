# Daily-Dev-Insight
Daily Dev Insight – 개발 지식·기술 뉴스 자동 큐레이션 서비스

## 실행 방법

- **IDE (권장)**  
  `backend` 프로젝트를 연 뒤 `com.dailydevinsight.DailyDevInsightApplication` 의 **main** 메서드를 실행합니다.
- **Gradle**  
  Gradle이 설치되어 있다면:
  ```bash
  cd backend
  gradle bootRun
  ```
  Gradle Wrapper가 있다면:
  ```bash
  cd backend
  ./gradlew bootRun   # Windows: gradlew.bat bootRun
  ```

실행 후 브라우저에서 [http://localhost:8080](http://localhost:8080) 로 접속하면 됩니다.

### API (JSON)
- `GET /api/insights` — 오늘의 큐레이션 목록 (데모 데이터)
