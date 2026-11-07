# 🎨 라이어 경매 (Liar Auction)

> **진품인가, 가짜인가. 상대를 속이고 파악하는 1:1 심리전 네트워크 경매 게임**  
> 한성대학교 컴퓨터공학 네트워크 프로그래밍 텀프로젝트 (Java Socket & Swing)

---

## 👥 팀원 및 역할 분담

* **주성진 (Core 1):** 라운드 진행 & 덱 관리, 비공개 감정 정보 시스템(감정서/소문), 실시간 경매 입찰 동기화
* **권이건 (Core 2):** 서버/대기실 멀티스레드 인프라, 협상 채팅 & 로그 저장, 되팔기 거래 시스템, 최종 자산 집계

---

## 🛠️ 기술 스택 및 환경

* **Language:** Java 11
* **Network:** Java Native Socket (TCP/IP)
* **GUI:** Java Swing
* **Architecture:** Multi-Threaded Client-Server (No External Libraries)

---

## 📡 네트워크 통신 프로토콜 (Protocol Specification)

메시지는 `COMMAND|ARG1|ARG2...` 형태의 텍스트 프로토콜로 전달됩니다.

### 1. Client ➡️ Server
| Command | Arguments | Description |
| :--- | :--- | :--- |
| `REQ_LOGIN` | `nickname` | 대기실 입장 및 닉네임 등록 |
| `REQ_CHAT` | `message` | 채팅 메시지 전송 |
| `REQ_BID` | `amount` | 입찰 요청 (+10, +50, +100 등) |
| `REQ_RESALE` | `price` | 낙찰 작품 되팔기 가격 제안 |
| `RESALE_ANS` | `ACCEPT / REJECT` | 되팔기 제안 수락 또는 거절 |

### 2. Server ➡️ Client
| Command | Arguments | Description |
| :--- | :--- | :--- |
| `BROADCAST_CHAT` | `sender\|message` | 전체 채팅 브로드캐스트 |
| `START_ROUND` | `roundNum\|title\|artist` | 라운드 시작 및 작품 정보 전송 |
| `INSPECTION` | `isOfficial\|infoText` | 비공개 감정 결과 (담당자 80% / 소문 60%) |
| `UPDATE_BID` | `highestBidder\|currentPrice` | 실시간 입찰 최신화 |
| `ROUND_END` | `winner\|price` | 해당 라운드 낙찰 결과 |
| `GAME_OVER` | `winner\|details` | 최종 진위 공개 및 승패 판정 |

---

## 🎮 게임 규칙 요약

1. **작품 구성:** 총 8점 (명작 2점: 400~600 / 보통 3점: 150~250 / 가짜 3점: 0)
2. **시작 자산:** 각 플레이어 현금 1,000 보유
3. **감정 시스템:** 매 라운드 한 명은 **감정서(정확도 80%)**, 한 명은 **소문(정확도 60%)**을 부여받음 (매회 교대)
4. **협상 & 경매:** 30초 협상 채팅 후 20초간 실시간 입찰 (5초 무응답 시 낙찰)
5. **승리 조건:** 8라운드 종료 후 `남은 현금 + 보유 작품 실제 가치` 합산 최고자 승리
