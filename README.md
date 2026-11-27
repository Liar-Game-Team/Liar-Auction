# 🎨 라이어 경매 (Liar Auction)

> **진품인가, 가짜인가. 서로를 속이고 파악하는 2~6인 심리전 네트워크 경매 게임**  
> 한성대학교 컴퓨터공학 네트워크 프로그래밍 텀프로젝트 (Java Socket & Swing) · 2조

---

## 👥 팀원 및 역할 분담

공통 뼈대(소켓 서버/클라이언트, GUI, 공용 클래스, 협상 채팅)는 함께 개발하고, 핵심 기능은 두 파트로 나눕니다.

| | **파트 A: 경매 진행** — 주성진 | **파트 B: 정보·거래** — 권이건 |
| :--- | :--- | :--- |
| **서버** | 작품 덱 생성 (명작 2, 보통 3, 가짜 3 섞기)<br>라운드 단계 자동 진행 (서버 타이머)<br>실시간 입찰 판정, 낙찰/유찰<br>현금보다 많이 부르는 입찰 거부 | 감정서 아이템(80%)·소문(60%) 생성<br>교란 아이템으로 다음 라운드 소문 조작<br>최종 정산 및 실제 작품 정보 공개 |
| **클라이언트** | 작품 카드, 현재가, 최고 입찰자, 남은 시간<br>+10 / +50 / +100 입찰 버튼 | 아이템 함 및 비공개 소문<br>최종 결과 창 (진위 공개, 자산 비교) |
| **발표 포인트** | 동시 입찰 순서 판정 (`synchronized`)<br>낙찰 타이머 리셋 | 진위는 서버만 보관<br>플레이어별로 다른 정보 개별 전송 |

### 파트 간 연결 지점
1. **라운드 시작 → 아이템 보유 수 전송** (A → B)
2. **소문 단계 → 플레이어별 소문 전송** (A → B)
3. **게임 종료 → 정산** (A → B)

---

## 🛠️ 기술 스택 및 환경

* **Language:** Java 17
* **Build:** Gradle (Wrapper 포함, 테스트는 JUnit 5)
* **Network:** Java Native Socket (TCP/IP)
* **GUI:** Java Swing
* **Architecture:** Multi-Threaded Client-Server (No External Libraries)

### 빌드 & 테스트
```bash
./gradlew build   # 컴파일 + 테스트
./gradlew test    # 테스트만
./gradlew runServer                 # 서버 실행 (기본 포트 5000, 변경: --args="6000")
./gradlew runClient                 # 클라이언트 실행 (창 2개 띄워서 1:1 테스트)
```

### 코드 구조
| 패키지 | 내용 | 담당 |
| :--- | :--- | :--- |
| `liarauction.common` | `Protocol`·`Message` (프로토콜), `Artwork`·`Grade`·`PlayerState`·`Phase`·`GameState` (공용 클래스), `GameConfig` (설정값) | 공통 |
| `liarauction.server` | `AuctionServer` (접속 받기), `ClientHandler` (클라이언트당 스레드), `Lobby` (로그인·방 목록·방 만들기/들어가기), `GameRoom` (방 하나: 준비·시작·채팅·퇴장, 메시지 분배) | 공통 |
| `liarauction.server.auction` | `AuctionController`: 덱 생성, 단계 타이머, 입찰 판정 | 파트 A |
| `liarauction.server.info` | `InfoController`: 아이템, 플레이어별 소문, 최종 정산 | 파트 B |
| `liarauction.client` | `ServerConnection` (받기 스레드 → Swing 스레드로 전달) | 공통 |
| `liarauction.client.ui` | `MainFrame` (로그인 → 방 목록 → 방 → 게임), `RoomListPanel`·`RoomPanel`·`GamePanel`·`StatusPanel`·`ChatPanel` | 공통 |
| | `AuctionPanel` (경매 화면) | 파트 A |
| | `InfoPanel` (아이템 함·소문·결과 창) | 파트 B |

* 파트별 할 일은 컨트롤러·패널에 `TODO(#이슈번호)`로 표시되어 있습니다.
* 게임 상태를 바꾸는 코드는 `GameRoom` lock 안에서 실행됩니다. 타이머 스레드에서 바꿀 때는 `synchronized (room) { ... }`로 감쌉니다.

---

## 📡 네트워크 통신 프로토콜 (Protocol Specification)

메시지는 한 줄에 하나, `COMMAND|ARG1|ARG2...` 형태의 텍스트 프로토콜로 전달됩니다.

* 메시지 이름과 인자 개수는 `liarauction.common.Protocol`, 만들기·읽기는 `liarauction.common.Message`를 사용합니다.
* 인자 안의 특수문자는 이스케이프합니다: `\` → `\\`, `|` → `\p`, 줄바꿈 → `\n` / `\r` (채팅에 `|`가 들어가도 안전)
* 인자 개수가 다르거나 모르는 메시지는 받는 쪽에서 무시하고 `ERROR`로 응답합니다.

### 1. Client ➡️ Server
| Command | Arguments | Description |
| :--- | :--- | :--- |
| `REQ_LOGIN` | `nickname` | 닉네임 등록, 방 목록 화면으로 |
| `REQ_CHAT` | `message` | 채팅 (방 목록 화면이면 방 목록 채팅, 방 안이면 그 방 채팅) |
| `REQ_BID` | `increment` | 입찰 요청, 현재가에 더할 금액 (`10` / `50` / `100`) |
| `REQ_USE_ITEM` | `item` | 공개 단계 아이템 사용 (`CERTIFICATE` / `JAMMER`) |
| `REQ_ROOM_LIST` | - | 방 목록 다시 받기 (바뀔 때마다 자동으로도 옴) |
| `REQ_CREATE_ROOM` | `name\|maxPlayers\|password` | 방 만들기 (최대 인원 2~6명, 비밀번호가 빈 문자열이면 공개방), 만든 사람이 방장 |
| `REQ_JOIN_ROOM` | `roomId\|password` | 방 들어가기 (공개방이면 빈 비밀번호) |
| `REQ_QUICK_JOIN` | - | 자리가 남은 대기 중 공개방에 바로 들어가기 |
| `REQ_LEAVE_ROOM` | - | 방 나가기, 방 목록으로 |
| `REQ_READY` | `true / false` | 방에서 준비 / 준비 취소 (방장은 필요 없음) |
| `REQ_START` | - | 게임 시작 (방장만, 2명 이상이고 나머지가 모두 준비했을 때) |

### 2. Server ➡️ Client
| Command | Arguments | Description |
| :--- | :--- | :--- |
| `LOGIN_OK` | `nickname` | 로그인 성공, 방 목록 화면으로 |
| `LOGIN_FAIL` | `reason` | 로그인 실패 (닉네임 중복, 쓸 수 없는 문자 등) |
| `ROOM_LIST` | `rooms` | 방 목록 (방 목록 화면에 있는 사람에게). 방마다 `id,name,count,maxPlayers,locked,playing`, 방과 방은 `;`로 구분 |
| `ROOM_JOINED` | `roomId\|name\|maxPlayers\|locked` | 방에 들어감 (만들기·들어가기·빠른 입장) |
| `ROOM_LEFT` | - | 방에서 나와 방 목록으로 |
| `ROOM_STATE` | `host\|players\|readyPlayers` | 방 대기 상태. 입장·퇴장·준비가 바뀔 때, 게임이 끝나 돌아올 때 전송. `players`·`readyPlayers`는 입장 순서대로 쉼표로 이은 닉네임 |
| `GAME_START` | `players\|startCash\|totalRounds` | 게임 시작 (`players`는 쉼표로 이은 닉네임, `totalRounds`는 인원별 8 / 10 / 12) |
| `BROADCAST_CHAT` | `sender\|message` | 전체 채팅 브로드캐스트 |
| `START_ROUND` | `roundNum\|title\|artist\|startPrice` | 라운드 시작 및 작품 정보 공개 |
| `PHASE` | `phaseName\|seconds` | 라운드 단계 전환 및 남은 시간 (서버 타이머 기준, `phaseName`은 `Phase` 이름) |
| `ITEMS` | `certificates\|jammers` | 아이템 보유 수 (플레이어별 개별 전송) |
| `ITEM_RESULT` | `item\|infoText` | 아이템 사용 결과 (사용자에게만 전송) |
| `RUMOR` | `infoText` | 소문 (플레이어별 개별 전송, 기본은 모두 같은 내용이고 교란당한 플레이어만 다른 거짓 소문) |
| `UPDATE_BID` | `highestBidder\|currentPrice` | 실시간 입찰 최신화 |
| `BID_REJECT` | `reason` | 입찰 거부 (현금 부족 등) |
| `ROUND_END` | `winner\|price` | 해당 라운드 낙찰 결과 (유찰 시 `winner`=`NONE`) |
| `UPDATE_CASH` | `nickname\|cash` | 낙찰 후 플레이어 현금 변경 |
| `PLAYER_LEFT` | `nickname` | 게임 중 플레이어 접속 끊김 (2명 이상 남으면 계속, 2명 미만이면 `ROOM_STATE`로 방 대기 상태 복귀) |
| `GAME_OVER` | `winner\|details` | 최종 진위 공개 및 승패 판정 (무승부 시 `winner`=`DRAW`) |
| `ERROR` | `reason` | 잘못된 요청 (형식 오류, 지금 단계에서 할 수 없는 요청 등) |

---

## 🎮 게임 규칙 요약

1. **방:** 로그인하면 방 목록. 방 이름·최대 인원(2~6명)·비밀번호(선택)를 정해 방을 만들거나, 목록에서 골라 들어가거나, 빠른 입장. 방을 만든 사람이 방장이고, 나머지가 모두 준비하면 방장이 시작. 여러 방이 동시에 게임 가능
2. **라운드 수:** 2~4명 8라운드, 5명 10라운드, 6명 12라운드. 절반이 끝나면 브레이크타임
3. **작품 구성:** 기본 8점 (명작 2점: 400~600만원 / 보통 3점: 150~250만원 / 가짜 3점: 0), 진위는 **서버만 보관**
4. **시작 자산:** 각 플레이어 현금 1,000만원 (돈은 모두 만원 단위)
5. **라운드 진행:** 공개(10초) → 소문(5초) → 협상(30초) → 경매(최대 20초) → 결과 공개(3초)
6. **아이템:** 각 플레이어는 감정서 1장과 교란 1개를 보유하며 공개 단계에 사용할 수 있음
7. **감정서:** 사용한 플레이어에게만 감정 결과 전송 (정확도 80%)
8. **소문:** 모든 플레이어에게 같은 소문 (정확도 60%). 교란을 사용하면 다음 라운드 상대에게 실제 등급 및 기본 소문과 다른 100% 거짓 소문이 전달되며, 마지막 라운드에는 교란할 수 없음
9. **경매:** 시작가 100만원부터 실시간 입찰, 5초간 추가 입찰이 없으면 낙찰 (입찰 시 타이머 리셋). 아무도 입찰하지 않으면 유찰. 보유 현금보다 많은 입찰은 거부
10. **승리 조건:** 마지막 라운드 종료 후 모든 작품의 실제 등급·가치를 공개하고, `남은 현금 + 보유 작품 실제 가치` 합산 최고자를 승자로 판정
11. **중간에 나감:** 남은 사람끼리 계속하고, 2명 미만이 되면 방 대기 상태로 돌아감. 게임이 끝나도 같은 방으로 돌아가 다시 시작 가능. 아무도 없는 방은 사라짐

---

## 📅 개발 일정

* **개발 완성:** 11월 말 · **테스트:** 11월 말 · **최종 완성:** 12월 2일

| 주차 | 공통 | 파트 A (주성진) | 파트 B (권이건) |
| :--- | :--- | :--- | :--- |
| **11주차** | 기본 소켓·GUI 뼈대 완성<br>경매 메시지 목록 확정<br>공용 클래스 확정 (작품, 플레이어 상태, 게임 진행 뼈대) | 작품 덱 생성<br>라운드 단계 타이머 | 감정서·소문 생성<br>아이템 보유 및 사용<br>플레이어별 개별 전송 |
| **12주차** | 1차 통합: 8라운드 한 판이 끝까지 돌아가는지 확인 | 실시간 입찰 판정, 낙찰/유찰<br>경매 화면 연결 | 교란 소문<br>아이템·소문 UI |
| **13주차** | 2차 통합, 버그 수정<br>발표 시나리오 작성<br>주말까지 기능 개발 마감 | 예외 처리 (접속 끊김, 현금 부족)<br>봇 대전으로 시작가 밸런스 조정 | 최종 정산 및 실제 작품 정보 공개<br>결과 창, 정확도 밸런스 조정 |
| **14주차** | 최종 리뷰<br>발표 영상 녹화 (와이어샤크 장면 포함)<br>보고서·README 정리, 제출 | 경매 부분 녹화 | 아이템·소문·정산 부분 녹화 |

---

## 🤝 협업 규칙

* **Notion:** 자료 공유, 주간 회의록
* **Discord:** 비대면 회의
* **GitHub:** 개발 (PR 시 CI 빌드·테스트 및 AI 코드 리뷰 자동 실행)

### 이슈
* 작업 시작 전에 **이슈 먼저 생성** → 이슈 1개 = 브랜치 1개 = PR 1개
* 템플릿: `기능` / `버그`, 제목은 `[A] 작품 덱 생성`처럼 파트 표시
* 라벨: 파트(`part:A`, `part:B`, `part:common`) + 종류(`enhancement`, `bug`, `documentation`), 공용 클래스·프로토콜 변경은 `protocol`
* 마일스톤: `11주차` ~ `14주차`, 담당자 지정 (파트 A: @jooseongjin, 파트 B: @yigun03)
* PR 본문에 `Closes #이슈번호`를 적어 merge 시 자동으로 닫기

### 브랜치
* `main`: 항상 실행 가능한 상태 유지, **직접 push 금지**
* 작업 브랜치: `타입/파트-기능` 형식으로 `main`에서 생성
  * 예: `feat/a-bid-timer`, `feat/b-inspection`, `fix/b-resale-cash`, `docs/readme`
  * 파트 구분 없는 공통 작업은 파트 생략 (예: `feat/socket-skeleton`)

### 커밋 메시지
`타입: 내용` 형식으로 작성합니다.

| 타입 | 용도 |
| :--- | :--- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변화 없는 코드 개선 |
| `docs` | 문서 수정 |
| `test` | 테스트 추가·수정 |
| `chore` | 빌드 설정, 기타 |

예: `feat: 감정서/소문 생성 로직 추가`

### Pull Request
* 기능 하나당 PR 하나, 작은 단위로 자주 올리기
* PR 템플릿(작업 내용, 관련 파트, 테스트 방법) 작성
* **CI(빌드·테스트) + AI 코드 리뷰 통과** 후 merge (팀원 승인 불필요)
* merge는 **Squash and merge**, merge 후 작업 브랜치 삭제

### 공용 코드 변경
* 공용 클래스(작품, 플레이어 상태 등)와 프로토콜 메시지 변경은 **Discord에 먼저 공유**하고 별도 PR로 올리기
* 파트 A·B는 패키지를 나눠 같은 파일을 동시에 수정하지 않도록 하기
