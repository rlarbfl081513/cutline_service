# Cutline Parsing

카카오톡 1:1 대화를 업로드하면, 원본 라인부터 LLM 입력 JSON까지 어떤 변환이 일어나는지를 엔지니어가 그대로 재현할 수 있을 정도로 상세하게 기록한 문서입니다. 클래스/메서드 이름과 함께 단계별로 다룹니다.

---

## 0. 입력 제약 및 전처리 가정
- **지원 포맷** (`MultiPlatformKakaoParser.Flavor`)
  - Android: `2025년 8월 3일 오후 4:00, 홍길동 : 안녕`
  - iOS: `2025. 8. 3. 16:00, 홍길동: 안녕`
  - PC: `[홍길동 [오후 4:00] 안녕` + 별도 날짜 헤더 `--- 2025년 8월 3일 토요일 ---`
- **인코딩**: UTF-8 텍스트(.txt). BOM이 있더라도 `String.strip()` 호출로 공백 제거됩니다.
- **대상**: 반드시 두 사람의 단일 채팅방 로그. Guard(`TwoPersonGuard`)가 세 번째 발화자를 감지하면 즉시 예외를 던집니다.
- **라인 단위 스트리밍**: 파일 전체를 메모리에 올리지 않습니다. `MultiPlatformKakaoParser.accept(String line)`에서 한 줄씩 처리합니다.

---

## 1. 라인 파서 세부 동작 (`MultiPlatformKakaoParser`)

### 1.1 스니핑 단계 (`handleSniffingPhase`)
1. `accept(line)` 호출 시 `sniffed == false`면 스니핑 모드로 진입.
2. 최대 50줄을 `buffer`에 저장하며 플랫폼별 정규식과 매칭시켜 점수를 계산.
   - Android: `msgStart` 매칭 시 +3, 날짜 헤더 매칭 시 +1.
   - iOS: 동일 로직.
   - PC: 메시지 시그니처 +3, 날짜 헤더 +2 (PC는 헤더 중요).
3. 50줄 이하라도 세 플랫폼 중 하나의 점수가 특정 임계치(4 이상)면 조기 확정.
4. 확정되면 `sniffed = true`, `flavor`와 `ps`(플랫폼별 패턴 묶음)를 설정.
5. 지금까지 버퍼링한 라인을 `handleReplayMode()`로 다시 `processLine`에 흘려보냄.

### 1.2 본 파싱 (`processLine`)
- **메시지 시그니처 매칭**
  - `matcher = ps.msgStart.matcher(line)` 성공 시 새로운 메시지가 시작된 것으로 간주.
  - 직전 `pendingMessage`를 `flushPendingMessage()`로 확정 → `MessageRaw(dt, speakerName, text)` 생성.
  - 새 메시지의 `PendingMessage`를 초기화하고 그룹에서 시간/발신자/본문을 읽어 첫 본문을 적재.
- **본문 계속 라인**
  - 기존 `pendingMessage != null` 상태에서 시그니처와 매칭되지 않는 라인이 오면 현재 본문 뒤에 `\n`을 붙이고 추가.
- **날짜 헤더 라인**
  - `ps.dateHeader`가 정의되거나(PC) Android/iOS에서도 날짜 패턴이 잡히면, `currentDateContext`에 헤더 정보를 저장.
  - PC에서는 다음 메시지를 파싱할 때 `buildDtPC()`에서 헤더에 포함된 연/월/일과 메시지의 시각을 합성합니다.
- **스킵 조건**
  - `ps.skipMeta`에 매칭되면(채팅방 이름, 시스템 공지, 파일명 등) 그대로 버립니다.
  - `SystemFilters.isDrop(text)`이 true이면(“삭제된 메시지입니다.” 등) 메시지를 생성하지 않습니다.

### 1.3 시간/발신자 보정 함수
- `buildDtAndroid(matcher)`
  - 그룹: (1)년 (2)월 (3)일 (4)오전/오후 (5)시 (6)분
  - 누락된 연/월/일은 직전 메시지나 헤더에서 보정, 없으면 현재 날짜 사용.
  - 오전/오후를 24시간제로 변환: 오후이고 12시 미만이면 +12, 오전 12시는 0시 처리.
- `buildDtIOS(matcher)`
  - 24시간제가 그대로 들어오므로 단순히 `LocalDateTime.of(year, month, day, hour, minute)`.
- `buildDtPC(matcher)`
  - `dateContext`에 저장된 `LocalDate`에 메시지 시각을 합쳐 `ZonedDateTime` 생성.
  - 날짜 헤더가 없는 상태에서 PC 메시지가 오면 `IllegalStateException` → 메시지 skip.
- `normalize(text)`
  - CR 제거, `strip()` 적용, 내부 다중 공백을 하나로 축약.

### 1.4 `MessageRaw` 방출 규칙
- `flushPendingMessage()`
  1. `pendingMessage`가 null이면 `Optional.empty()`.
  2. `normalize(body)` 후 비어 있으면 drop.
  3. Guard 적용 전 상태로 `MessageRaw` 반환 (`systemGenerated=false`).
- End-of-file 처리: `MultiPlatformKakaoParser.flush()`에서 마지막 `pendingMessage`를 반드시 방출.

---

## 2. 두 사람 검증 및 U/F 매핑 (`TwoPersonGuard`)
1. `GuardConfig(userDisplayName, friendDisplayName)` 초기화.
2. `acceptSpeaker(speakerName)` 호출 시
   - 사용자/친구 이름과 같으면 `seen` 집합에 추가.
   - 집합 크기가 2 초과면 `IllegalArgumentException` – 다자간 대화로 판단.
   - 이름이 둘 중 하나와 일치하지 않으면 즉시 예외.
3. `mapToUF(name)`으로 `Speaker.U` 또는 `Speaker.F`로 변환.

---

## 3. 턴 집계 (`TurnAccumulator`)
- **목표**: 동일 발화자가 같은 분(minute)에 이어서 보낸 메시지를 하나의 턴으로 묶어, DSL과 통계에 활용.
- **상태 필드**
  - `curSpk`, `curStart`, `curEnd`, `buf`(본문 버퍼), `msgCount`.
- **알고리즘** (`ingest(Speaker spk, ZonedDateTime dt, String text)`)
  1. `minute = dt.withSecond(0).withNano(0)`으로 분 단위 절삭.
  2. 현재 턴이 없으면 `startNewTurn()` 호출.
  3. 동일 발화자 + 동일 minute → `buf.append` (있으면 `\n`), `curEnd` 갱신, `msgCount++`.
  4. 발화자 변경 또는 minute 변경 → `closeTurn()`으로 이전 턴 확정 후 새 턴 시작.
- **flush()**
  - 마지막 턴까지 `out` 리스트에 담아 반환 후 내부 상태 초기화.

---

## 4. 월별 라우팅 및 통계 계산

### 4.1 월 분리 (`MonthRouter`)
- `Map<YearMonth, List<Turn>> route(List<Turn> turns)`
  - 각 턴의 `start()` 기준 월을 키로 사용.
  - 월이 비어 있으면 새 리스트 생성.

### 4.2 통계 누적 (`MonthlyStatsAccumulator`)
- `onMessage(Speaker spk, ZonedDateTime dt, String text)`
  - 친구(F) 메시지만 `fMsgCount`, `fMsgChars` 증가. 문자 수는 `codePointCount`로 계산 → 이모지 포함 정밀 길이.
  - `activeDays` 집합에 날짜 추가.
- `computeReplyStats(List<Turn> turns)`
  - `U → F` 전환만 추적해 응답 대기 시간을 분 단위로 누적.
  - 응답 시간이 0 이하이면 무시.
- `computeMaxNoChatStreak()`
  - 해당 월의 1일부터 말일까지 순회하며 `activeDays`에 없는 연속된 일수의 최댓값 계산.
- `finalizeMonth(replyStats)`
  - `MonthlyStatsResult(YearMonth ym, int fMsgCount, int fMsgChars, long fReplyMinutesSum, int fReplyPairsCount, int activeDaysCount, int maxNoChatStreakDays)` 반환.

---

## 5. DSL 생성 (`DslBuilder`)

### 5.1 DSL 레코드 포맷
```
<UF>|+<delta>|"<escaped_text>"
```
- `<UF>`: `Speaker.U` → `U`, `Speaker.F` → `F`.
- `<delta>`: 이전 턴이 끝난 시각과 현재 턴 시작 시각의 분 차이. 최초 턴은 0.
- `<escaped_text>`: `DslCompressor.compress(text)` → `escape()`.

### 5.2 `DslCompressor` 단계
1. `stripOuterBrackets`로 `[사진]`, `[이모티콘]` 등 외곽 괄호 제거.
2. 첨부물 토큰 치환: `사진`→`<PHOTO>`, `동영상`→`<VIDEO>`, `파일`→`<FILE>`, `보이스메시지`→`<AUDIO>`, `이모티콘`→`<STICKER>`.
3. URL 처리: 첫 번째 URL은 `<URL>`로 치환, 나머지는 제거.
4. 반복 감정어 축약: `ㅋㅋㅋ`, `ㅎㅎ`, `ㅠㅠ`, `ㅜㅜ` 등을 각각 `<LAUGH>`, `<CRY>` 토큰으로 1회만 남김.
5. 이모지 처리: 첫 이모지는 `<EMOJI>`, 나머지 삭제.
6. 공백 정규화: 다중 공백/탭을 단일 공백, 앞뒤 공백 제거.

### 5.3 `escape(String s)`
- 역슬래시 → `\\`
- 큰따옴표 → `\"`
- 줄바꿈 → `\n`
- 캐리지리턴 삭제.

### 5.4 예시 변환
원본 메시지(동일 분 2줄):
```
2025년 8월 3일 오후 4:00, 홍길동 : ㅋㅋㅋㅋ 오늘 영화 예매했어!
2025년 8월 3일 오후 4:00, 홍길동 : 링크 여기야 https://example.com/movie
```
→ `Turn(F, start=16:00, end=16:00, text="ㅋㅋㅋㅋ 오늘 영화 예매했어!\n링크 여기야 https://example.com/movie", msgCount=2)`
→ `DslCompressor` 결과: `<LAUGH> 오늘 영화 예매했어!\n링크 여기야 <URL>`
→ DSL 라인: `F|+5|"<LAUGH> 오늘 영화 예매했어!\n링크 여기야 <URL>"`

---

## 6. 월 컨텍스트 & 결측치 보정 (`MonthlyContextBuilder`)
1. 입력: `List<MonthOutput>` (DSL + `MonthlyStatsResult`), `Person`, 직전 `PersonValue`, 사용자/친구 표시명.
2. 각 월에 대해:
   - `autoAnalysisProvider.provide(index, output)` 호출 → 자동 분석 결과 (실시간 호출 or 미리 받아온 리스트).
   - `ManualStatsCalculator.calculate(stats)`로 수동 점수 계산 (메시지 길이/갯수/활동일 보너스, 응답지연/무응답 페널티).
   - `AutoStatsCalculator.calculate(autoStatsPayload)`로 LLM이 준 점수 표준화.
   - `RelationshipValueCalculator.calculate(person, previousValue, manualStats, autoStats)`로 관계값 산출.
   - `PersonValue` 생성 후 `MonthlyComputationContext`에 묶어 반환.
3. 월이 건너뛰어진 경우 (`lastProcessedMonth.plusMonths(1) < currentMonth`)
   - `appendMissingMonthContexts`가 중간 월을 채우는 더미 컨텍스트 생성.
   - `MISSING_MONTH_PENALTY = 10_000_000` 만큼 관계값을 감소시키고 피드백/토픽/이슈는 빈 값으로 유지.

---

## 7. LLM 호출 상세

### 7.1 AutoAnalysis (`AutoAnalysisServiceImpl.analyze`)
- **System Prompt**
  - “DSL을 기반으로 관계 통계를 도출하는 분석가” 역할, 출력 JSON 스키마, 필드별 제약(미응답 처리, 주제/이슈 규칙, 피드백 한 문장 등) 명시.
- **User Prompt**
  - `Month`, `User`, `Friend`, `friend message count/chars`, `active days`, `max silent streak`, `reply minutes sum/count`, DSL 전문.
- **LLM 호출**
  - `chatClient.prompt().system(...).user(...).call().content()`.
- **응답 파싱**
  - `extractJson(raw)`로 코드블록 제거 → `objectMapper.readValue` → `AutoAnalysisResponse`.
  - 누락 필드는 기본값: `AutoStatsPayload.empty()`, 빈 리스트, 빈 문자열.
- **실패 처리**
  - 통신/모델 오류: warn 로그 후 `AutoAnalysisResult.empty()`.
  - JSON 파싱 실패: warn 로그 후 빈 결과.

### 7.2 Relationship Strategy (`RelationshipStrategyServiceImpl.recommend`)
- **System Prompt**
  - JSON 3필드만 허용, 각 값은 정확히 4줄, 한국어 명령형, 이모지/구두점 제한.
- **User Prompt**
  - 관계 유형/상태, 누적 12개월 요약(JSON 문자열), 최신 월 수동/자동 통계, 최신 DSL 전문.
- **LLM 호출 & 파싱**
  - `objectMapper.readValue(extractJson(content), StrategyResponse.class)`.
  - 실패 시 `StrategyRecommendation.empty()`.

---

## 8. 저장/정리 흐름
1. `PersistenceCoordinator.persist(person, contexts)`
   - `PersonValueRepository.save(value)`로 월별 관계값 저장.
   - `applyTopics/Issues`: 동일 월 기존 데이터 삭제 후 LLM 결과 재생성.
   - 월별 요약(`MonthlySummarySnapshot`) 목록 작성 (topics/issue/autostats를 JSON 문자열로 직렬화).
2. `PersonUpdateCoordinator.persistAndUpdate`
   - 최신 `PersonValue` 조회, 통계 로그 기록.
   - `RelationshipStrategyService.recommend` 결과를 `Person.updateStrategies()`에 반영 후 `PersonRepository.save(person)`.
3. 컨트롤러 응답은 즉시 성공 반환 → 클라이언트는 별도 조회 API로 월별 결과 확인.

---

## 9. 전체 예시 (줄번호 기준)

### 9.1 원본 (Android 내보내기 발췌)
```
1: 카카오톡 대화
2: 홍길동, 010-0000-0000
3: 2025년 8월 3일 오후 4:00, 홍길동 : ㅋㅋㅋㅋ 오늘 영화 예매했어!
4: 2025년 8월 3일 오후 4:00, 홍길동 : 링크 여기야 https://example.com/movie
5: 2025년 8월 3일 오후 4:01, 나 : 오 좋아! 몇 시에 볼까?
6: 2025년 8월 3일 오후 4:05, 홍길동 : 7시 정도 어때?
```

### 9.2 스니핑 결과
- 1~2행은 `skipMeta` 매칭 → 점수 미반영.
- 3행이 Android `msgStart` → Android 점수 3, 날짜 헤더 없음 → 50줄 전에 확정.

### 9.3 MessageRaw 시퀀스
1. `MessageRaw(dt=2025-08-03T16:00+09:00, speaker="홍길동", text="ㅋㅋㅋㅋ 오늘 영화 예매했어!", system=false)`
2. 동일 minute & speaker → 두 번째 라인 본문을 같은 메시지에 `\n`으로 누적 후 flush 안 함.
3. 세 번째 라인(사용자) 도착 시 flush → MessageRaw#1 확정.
4. 사용자 메시지 → `MessageRaw(dt=2025-08-03T16:01+09:00, speaker="나", text="오 좋아! 몇 시에 볼까?", false)`
5. 친구 메시지 → `MessageRaw(dt=2025-08-03T16:05+09:00, speaker="홍길동", text="7시 정도 어때?", false)`

### 9.4 Guard & Turn 생성
- Guard는 `홍길동`, `나`만 허용.
- Turn#1: `Speaker.F`, start=end=16:00, text=`ㅋㅋㅋㅋ ... \n링크 ...`, msgCount=2
- Turn#2: `Speaker.U`, start=end=16:01, text=사용자 메시지, msgCount=1
- Turn#3: `Speaker.F`, start=end=16:05, text=친구 메시지, msgCount=1

### 9.5 DSL
```
F|+0|"<LAUGH> 오늘 영화 예매했어!\n링크 여기야 <URL>"
U|+1|"오 좋아! 몇 시에 볼까?"
F|+4|"7시 정도 어때?"
```

### 9.6 월별 통계 (2025-08)
- 친구 메시지 수: 3 (turn1 msgCount=2 + turn3 msgCount=1)
- 친구 문자 수: `<LAUGH>` 토큰 이전 문자 수 기준 (실제 원문 기준 codePointCount)
- 활동일수: 1
- 응답 지연: U→F (16:01→16:05) 4분 → `replyMinutesSum=4`, `replyPairsCount=1`
- 최장 무응답일: 0 (같은 날 대화)

### 9.7 AutoAnalysis LLM 입력(N=1월)
```text
You are given a month of KakaoTalk conversation exported as a DSL.
Month: 2025-08
User (U): 나
Friend (F): 홍길동
Friend message count: 3
Friend message chars: 34
Active days: 1
Max silent streak days: 0
Total reply minutes from U to F: 4
Reply pair count: 1
DSL:
F|+0|"<LAUGH> 오늘 영화 예매했어!\n링크 여기야 <URL>"
U|+1|"오 좋아! 몇 시에 볼까?"
F|+4|"7시 정도 어때?"
```

### 9.8 AutoAnalysis 예상(JSON)
```json
{
  "autoStats": {
    "startChat": 0,
    "question": 1,
    "privateStory": 0,
    "positiveReaction": 1,
    "getHelp": 0,
    "giveHelp": 0,
    "meetingSuccess": 1,
    "meetingRejection": 0,
    "attack": 0,
    "noResponse": 0
  },
  "topics": [
    { "topic": "영화", "count": 2 }
  ],
  "issues": [
    { "category": "MEETING_SUCCESS", "summary": "상영 시간 확정" }
  ],
  "feedback": "약속을 빠르게 확정한 안정적인 관계입니다"
}
```

### 9.9 Relationship Strategy 입력 요약
- 직전 12개월 요약이 없으면 `(no history)`.
- 최신 월 DSL/통계/자동 분석 JSON이 통째로 전달되어 4줄씩 전략을 생성.

---

## 10. API 흐름 요약 (`ParsingController`)
1. `POST /{personId}`
   - JWT 쿠키에서 인증 → `AuthenticationUtils.getCurrentUserId()`.
   - `UserRepository.findById()`로 사용자명 확보.
   - `PersonRepository.findById(personId)`로 대상자 확인.
   - `parsingService.processWithBatchAutoAnalysis(personId, file, userDisplayName)` 호출.
2. `ParsingService.processWithBatchAutoAnalysis`
   - `onePassCore.run(source, userDisplayName, person.getName())` → `List<MonthOutput>`.
   - `autoAnalyses = analyzeAutoAnalysisInBatches(outputs, userDisplayName, person.getName())`.
   - `contexts = monthlyContextBuilder.buildWithAutoAnalyses(outputs, autoAnalyses, person, latestPersonValue)`.
   - `personUpdateCoordinator.persistAndUpdate(person, contexts)`.
3. 응답: `ApiResponse.success(true)`.

---

## 11. 업로드 체크리스트 & 트러블슈팅
- **파일 인코딩 오류**: ANSI 파일은 `String.strip()`에서 깨짐 → 업로드 전 UTF-8로 재저장.
- **참가자 이름 불일치**: Guard가 “등록되지 않은 화자” 예외를 던집니다. Person 정보와 실제 로그 이름이 정확히 일치해야 합니다.
- **PC 로그 날짜 헤더 누락**: “PC 날짜 컨텍스트 없음” warn 후 해당 구간은 스킵되어 DSL이 끊어질 수 있습니다.
- **대용량 로그 (수십만 행)**: 스트리밍 파싱 + Virtual Thread LLM 호출로 처리하지만, OpenAI API rate-limit 고려 필요. 재시도 로직은 현재 없으므로 실패시 수동 재업로드 필요.
- **LLM 응답 JSON 깨짐**: `AutoAnalysisResult.empty()`가 반환되면 해당 월 토픽/이슈/피드백이 비어 있습니다. 로그에서 원인 확인 후 재시도.

---

이 문서를 따라가면 원본 라인이 DSL과 LLM 요청으로 어떻게 변환되는지, 그리고 최종적으로 어떤 엔티티에 저장되는지 전체 과정을 추적할 수 있습니다.

## 가상 스레드 실행 모델
- `ParsingService.analyzeAutoAnalysisInBatches()`는 월별 DSL을 12건씩 묶은 뒤, 매 배치마다 `Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())`로 **가상 스레드 전용 실행기**를 생성합니다. try-with-resources를 사용해 배치가 끝나면 실행기를 즉시 닫아 스케줄러 자원을 반납합니다.
- 각 배치에서 생성한 `Callable<AutoAnalysisResult>`는 `autoAnalysisService.analyze()` 한 번만 감싸고 있으며, `invokeAll()`이 반환하는 `Future` 리스트는 가상 스레드가 OpenAI API 호출을 기다리는 동안에도 컨테이너 스레드를 점유하지 않습니다. 덕분에 네트워크 I/O 지연이 길어도 수백 건의 요청을 동시에 처리할 수 있습니다.
- `invokeAll()`은 전달된 `Callable`을 모두 제출한 뒤 완료될 때까지 블로킹하지만, 가상 스레드 풀은 필요 시 수천 개까지 확장되므로 별도의 큐 튜닝이 필요 없습니다. 대신 배치 크기를 12로 제한해 OpenAI rate-limit과 DB 후처리 부담을 완화합니다.
- 결과 수집 루프에서는 각 `Future#get()`을 호출해 개별 월의 AI 분석 결과를 읽어오며, `ExecutionException`이 발생하면 해당 인덱스에 `AutoAnalysisResult.empty()`를 채워 후속 저장 파이프라인이 깨지지 않도록 합니다. 오류는 구조화된 로그로 남겨 운영자가 특정 월만 재시도하도록 유도합니다.
- `InterruptedException`이 발생하면 현재 스레드를 다시 인터럽트 한 뒤 배치를 중단합니다. 이 처리는 스프링 트랜잭션 롤백 흐름과 어긋나지 않도록 설계되었습니다.
- 가상 스레드를 사용하면 기존 플랫폼 스레드 대비 메모리 압박이 적기 때문에, `autoAnalysisService`가 OpenAI SDK 내부에서 **동기/블로킹 HTTP 클라이언트**를 사용해도 전체 서비스 스레드 풀이 고갈되지 않습니다. 운영 환경에서는 한 사용자 요청이 여러 월 데이터를 올려도 동일한 JVM에서 다른 API 요청과 안전하게 공존합니다.

## 관계 값 계산식 요약

### 1) 원시 점수 수집
- **수동 지표**(`ChatManualStats`)
  - `manualPositive = bonusChars + bonusMessages + bonusChatDays`
  - `manualPenalty = penaltyResponse + penaltySilent`
- **자동 지표**(`ChatAutoStats`)
  - `autoPositive = scoreStartChat + scoreQuestion + scorePrivateStory + scorePositiveReaction + scoreMeetingSuccess + scoreGiveHelp`
  - `autoPenalty = |scoreNoResponse| + |scoreMeetingRejection| + |scoreAttack| + |scoreGetHelp|`

### 2) 정규화 (0~1)
- 상한값
  - `MANUAL_POSITIVE_MAX = 10,000,000` (문자/메시지/대화일 보너스 최대치 합)
  - `MANUAL_PENALTY_MAX = 10,000,000` (응답/무응답 패널티 최대치 합)
  - `AUTO_POSITIVE_MAX = 20,000,000` (LLM 긍정 점수 상한)
  - `AUTO_PENALTY_MAX = 20,000,000` (LLM 패널티 절댓값 상한)
- 각 항목을 `min(value, max)/max`로 나누어 0~1 범위로 정규화 후
  `blended = (manualNorm + autoNorm) / 2` 로 수동/자동 비중을 동일하게 반영합니다.

### 3) 스케일 변환 & 승수 적용
- `positiveTotal = blendedPositive × 20,000,000`
- `negativeTotal = blendedPenalty × 20,000,000`
- 관계 프로필 및 상태 가중치
  - `positiveMultiplier = max(0, 1 + relationPositiveWeight + statusWeight)`
  - `negativeMultiplier = max(0, 1 + relationNegativeWeight − statusWeight)`
  - 상태 관심도가 높을수록(+0.25) 긍정은 강화, 부정은 완화됩니다.
- `weightedPositive = positiveTotal × positiveMultiplier`
- `weightedNegative = negativeTotal × negativeMultiplier`
- 최종 증감: `delta = weightedPositive − weightedNegative`

### 4) 기본값·근속 보너스·캡
- 관계 유형별(`RelationProfile`)
  - `baseValue`: 초기 점수 (연인 50M, 친구 50M, 직장동료 30M)
  - `tenureUnit`: 연차별 가산 단위 (연인 5M, 친구 2.5M, 직장동료 1.5M)
  - `tenureCap`: 근속 보너스 상한 (연인 50M, 친구 50M, 직장동료 30M)
  - `totalCap`: 총 점수 상한 (연인/친구 100M, 직장동료 60M)
- 근속 보너스: `tenureBonus = min( round((durationMonths/12) × tenureUnit), tenureCap)`
- 시작 점수: `startingValue = previousValue.value (없으면 baseValue + tenureBonus)`
- 시작점과 결과 모두 `0 ≤ value ≤ totalCap` 범위로 클램프합니다.

### 5) 결과 산출
- `currentRaw = startingValue + delta`
- `currentCapped = clamp(currentRaw, 0, totalCap)`
- `finalValue = round(currentCapped / 10,000) × 10,000`
- 변화율
  - 이전 값이 있으면 `changeRate = (finalValue − previousValue) / previousValue`
  - 첫 달이라도 baseValue가 있으면 `(finalValue − startingValue)/startingValue` 로 계산
- 반환(`RelationshipValueResult`)
  - `currentValue = finalValue`
  - `weightedDelta = round(delta)`
  - `positiveContribution = round(weightedPositive)`
  - `negativeContribution = round(weightedNegative)`
  - 승수(`positiveMultiplier`, `negativeMultiplier`)와 변화율도 함께 포함합니다.
