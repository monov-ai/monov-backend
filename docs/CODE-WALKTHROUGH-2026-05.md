# 코드 상세 가이드 — 2026년 5월 작업

오늘 구현한 코드를 **개념 → 코드 → 왜 이렇게 → 다른 부분과의 연결** 순서로 풀어 설명합니다. STUDY 문서가 *무엇을 했는가* 라면, 이 문서는 *어떻게/왜 그렇게 짰는가* 입니다.

---

## 목차

1. [패키지 재배치 — `external/` 와 `infrastructure/`](#1-패키지-재배치)
2. [JWT 버그 수정 — Bearer 토큰 파싱](#2-jwt-버그-수정)
3. [`business` 도메인 sub-aggregate 분리](#3-business-도메인-sub-aggregate-분리)
4. [Recommendation aggregate — JSON 컬럼 + value object](#4-recommendation-aggregate)
5. [ImageJob aggregate — 스냅샷 + 자식 엔티티](#5-imagejob-aggregate)
6. [Prompt 합성 서비스](#6-prompt-합성-서비스)
7. [OpenAI 통합 — Spring AI structured output](#7-openai-통합)
8. [Nanobanana 통합 — Feign + base64 + S3](#8-nanobanana-통합)
9. [비동기 인프라 — AsyncConfig + 이벤트 + 워커](#9-비동기-인프라)
10. [보조 인프라 — SlugGenerator, S3 업로드, ErrorCode](#10-보조-인프라)

---

## 1. 패키지 재배치

### 1.1 `UserOnboarding` 삭제

#### 의도
프로젝트에 `User.onboardingCompletedAt`/`User.job`/`User.source` 같은 onboarding 관련 컬럼이 이미 존재했고, 별도 `UserOnboarding` 엔티티가 또 있어서 중복이었습니다. 컨버터(`UserOnboardingConverter`)도 이미 본문이 주석 처리되어 dead code였습니다.

#### 작업
```bash
git rm src/main/java/com/monovai/domain/user/entity/UserOnboarding.java
git rm src/main/java/com/monovai/migration/dto/converter/UserOnboardingConverter.java
```

#### 핵심 포인트
- **단일 진실의 원천 (Single Source of Truth)**: 같은 도메인 정보가 두 군데 있으면 어느 게 맞는지 헷갈리고 동기화 비용이 듦. 한쪽으로 통일.
- **Dead code 제거**: 주석 처리된 코드는 의도(곧 살릴 거? 영원히 안 쓸 거?)가 불명. 의도가 분명할 때만 살리고, 아니면 삭제. git history에 남으니까 안전.

### 1.2 `external/` 위치 통일 (#15)

#### 의도
외부 HTTP API 클라이언트가 `global/client/{kakao,google}` 에 있었는데, S3/Redis 같은 외부 시스템 어댑터는 `infrastructure/` 에 있었습니다. 비슷한 역할이 두 군데로 갈라져 있어서, OpenAI/Nanobanana 추가할 때 어디 둘지 혼란.

#### 작업
```
global/client/google/      →  external/google/
global/client/kakao/       →  external/kakao/
```

`git mv` 로 이동하고 패키지 선언 + 모든 import를 갱신.

#### 핵심 포인트
- **컨벤션 통일**: 새 코드 추가할 때 "어디 두지?" 고민이 사라짐.
- **`external/` vs `infrastructure/` 의도**:
  - `external/`: 다른 회사가 운영하는 HTTP API (Kakao OAuth, Google API, OpenAI, Nanobanana)
  - `infrastructure/`: 우리가 운영하는 인프라 (S3, Redis)
- **`git mv` 활용**: rename이라고 git이 인지해서 history 보존됨. `cp` + `rm` 보다 훨씬 깔끔.

### 1.3 JWT 도메인 레이어 정리 (#16)

#### 의도
`global/jwt/domain/{entity,repository,service}` 라는 *전역 인프라 안의 미니 도메인* 구조가 있었는데, 두 단계 도메인이 어색했습니다. 게다가 `Token`은 `@RedisHash`로 Redis에 저장되는 엔티티라 `infrastructure/redis/` 가 더 정확한 위치.

#### 작업
```
global/jwt/domain/entity/Token.java        →  infrastructure/redis/entity/Token.java
global/jwt/domain/repository/TokenRepo     →  infrastructure/redis/repository/TokenRepo
global/jwt/domain/service/JwtService.java  →  global/jwt/service/JwtService.java
global/jwt/domain/service/TokenService.java →  global/jwt/service/TokenService.java
```

#### 핵심 포인트
- **위치는 책임을 따라간다**: `Token` 은 Redis 저장소 엔티티이지 "JWT 도메인 객체"가 아님. 위치 = 책임의 시각적 표현.
- **불필요한 중첩 제거**: `global/jwt/service/` 한 단계가 `global/jwt/domain/service/` 보다 짧고 의미 동일.
- **import 자동 갱신**: 이동하면 `package` 선언 + 그 클래스를 import한 모든 파일을 갱신해야 함. 하나라도 빠뜨리면 컴파일 깨짐. 전체 `grep -r "global.jwt.domain"` 으로 검증.

---

## 2. JWT 버그 수정

### 2.1 문제

```java
// JwtService.reissueToken — 수정 전
String refreshToken = jwtExtractor.extractToken(authorizationHeader);
// ...
String role = jwtExtractor.extractRole(authorizationHeader);  // ❌
```

#### 무슨 일이 일어났나

각 변수가 담고 있던 값:
- `authorizationHeader`: `"Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2..."` (HTTP 헤더 원문)
- `refreshToken`: `"eyJhbGciOiJIUzI1NiJ9.eyJ1c2..."` (`Bearer ` 떼어낸 순수 JWT)

`extractRole(authorizationHeader)` 가 호출하는 내부 메서드:
```java
public String extractRole(String token) {
    Claims claims = extractClaims(token);   // ← 여기서 JWT 파싱
    return claims.get("role", String.class);
}
```

`extractClaims` 는 받은 문자열을 **JWT로 파싱**합니다 (`header.payload.signature` 형식). `"Bearer eyJ..."` 통째로 파싱 시도 → 점 두 개 구분 안 맞음 → `JwtException` → `UnauthorizedException`.

결과: **/auth/reissue 엔드포인트가 항상 401**. 리프레시 자체가 동작 안 함.

### 2.2 수정

```java
String role = jwtExtractor.extractRole(refreshToken);   // ✅
```

순수 JWT를 넘기면 정상 파싱.

### 2.3 핵심 포인트

- **함수 인자의 *형태*를 정확히 추적**: `String` 타입이라고 다 같은 게 아님. `Bearer prefix 포함` vs `순수 JWT` 는 의미가 완전히 다름.
- **타입 시스템의 한계**: 자바는 `String`이 둘 다 같은 타입. 컴파일러가 못 잡음. 코드 리뷰 / 테스트가 책임.
- **방어 패턴**: 미래에 비슷한 실수를 막으려면, `record AuthHeader(String value)`, `record JwtToken(String value)` 같은 wrapper 타입을 만들어 컴파일러에게 구분시킬 수 있음 (이번엔 안 함, 오버킬).

---

## 3. business 도메인 sub-aggregate 분리

### 3.1 의도

`/business` 라는 큰 우산 안에 성격이 다른 3개 작업이 사슬로 연결됨:

| Phase | 외부 의존 | 동기성 | 데이터 모델 |
|---|---|---|---|
| 1. Recommendation | OpenAI GPT | 동기 | request + 3 추천 |
| 2. ImageJob | Nanobanana | 비동기 | job + variants[] |
| 3. Edit | Nanobanana | 비동기 + 체이닝 | edit (baseId polymorphic) |

평면으로 두면 controller 3개, service 3개, entity 5+개가 한 폴더에 섞여서 "여러 도메인이 섞여있는 느낌". 

### 3.2 구조

```
domain/business/
├── recommendation/    Phase 1
├── imagejob/          Phase 2
├── edit/              Phase 3
└── prompt/            공유 (Phase 1·2·3 모두 사용)
```

각 sub-aggregate 안은 표준 4-레이어:
```
recommendation/
├── controller/
├── service/
├── repository/
├── entity/
│   ├── enums/
│   └── value/
└── dto/
    ├── request/
    └── response/
```

### 3.3 핵심 포인트

- **DDD aggregate 경계**: aggregate는 함께 변경되는 엔티티의 묶음. 3 phase는 데이터 모델/lifecycle이 다르므로 별개 aggregate.
- **공유 책임은 별도 폴더**: `prompt/` 는 Phase 1·2·3 모두 사용. 한 곳에 두면 모든 phase가 import → 결합도 OK (단방향).
- **결합도 방향**: phase 간 참조는 같은 우산 안이라 import 부담 없음. `business/imagejob/...` 가 `business/recommendation/value/...` 를 참조하는 건 자연스러움.

---

## 4. Recommendation aggregate

### 4.1 큰 그림

```
RecommendationRequest (1 row)
  ├── Style style
  ├── description
  ├── productImageUrl, productImagePath
  ├── referenceImageUrl, referenceImagePath
  ├── headline, summary (TEXT)
  ├── corePoints (JSON 컬럼)         ← record
  └── recommendations (JSON 컬럼)    ← List<record>
```

별도 `Recommendation` 엔티티 없이, **3개 추천을 JSON 컬럼**에 통째로 저장. 정규화하면 `recommendation_tags`, `global_locks` 테이블이 추가로 생기지만 검색/개별 수정 패턴이 0건이라 정규화 이득이 없음.

### 4.2 Value object — `GlobalLock`

```java
package com.monovai.domain.business.recommendation.entity.value;

public record GlobalLock(
    String background,
    String surface,
    String lighting,
    String mood
) {
}
```

#### 왜 record?
- 불변 (final 필드)
- equals/hashCode/toString/accessor 자동 생성
- Jackson native 지원 (Spring AI + Hibernate JSON 컬럼 모두 동작)
- value object = "정체성 없고 값만 같으면 같은 것" → record 의미와 일치

#### 왜 별도 클래스?
인라인으로 String 필드 4개를 RecommendationRequest에 박을 수도 있음. 별도 클래스로 둔 이유:
- **재사용**: ImageJobVariant도 같은 GlobalLock 사용 (스냅샷 복사)
- **응집**: "background는 surface와 함께 의미를 가짐" — 4개 필드가 묶여 다님
- **JSON 직렬화 단위**: GPT 응답에서도 nested 객체로 옴

### 4.3 Value object — `RecommendationItem`

```java
public record RecommendationItem(
    String id,                  // GPT slug ("warm_wood_studio")
    String title,
    String description,
    List<String> tags,
    boolean recommended,
    GlobalLock globalLock
) {
}
```

#### 핵심 포인트
- `List<String> tags` — record 안에 `List` 두는 게 정상. Jackson이 직렬화 처리.
- `GlobalLock globalLock` — record 안에 record 중첩 가능.
- `String id` — DB PK 가 아니라 **GPT가 만든 slug** (`warm_wood_studio`). 사용자가 `recommendationIds` 로 보낼 때 이걸 씀.

### 4.4 RecommendationRequest 엔티티 — JSON 컬럼

```java
@Entity
@Table(name = "recommendation_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationRequest extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String requestSlug;          // "bizrec_…"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private Style style;

    @Column(columnDefinition = "TEXT") private String description;
    @Column(columnDefinition = "TEXT") private String productImageUrl;
    // ... (productImagePath, reference*)

    @Enumerated(EnumType.STRING)
    private RecommendationStatus status;

    @Column(columnDefinition = "TEXT") private String headline;
    @Column(columnDefinition = "TEXT") private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private CorePoints corePoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<RecommendationItem> recommendations;

    // ...
}
```

#### `@JdbcTypeCode(SqlTypes.JSON)` 의 마법

이 한 어노테이션으로:
- 저장 시: record/List → JSON 문자열로 자동 직렬화 → MySQL JSON 컬럼에 INSERT
- 조회 시: MySQL JSON 컬럼 → 자동 역직렬화 → record/List 인스턴스

내부에서 Jackson을 사용함. 우리 record들이 이미 Jackson 호환이라 추가 설정 0.

#### 표준 JPA 엔티티 패턴

| 어노테이션 | 의도 |
|---|---|
| `@Entity` | JPA 엔티티 표시 |
| `@Table(name = "...")` | 테이블 이름 명시 (안 하면 클래스명 변환) |
| `@Getter` (Lombok) | getter 자동 생성 |
| `@NoArgsConstructor(access = PROTECTED)` | JPA 가 프록시 만들 때 필요. PROTECTED로 외부 직접 호출 차단 |
| `@Builder(access = PRIVATE)` | 정적 팩토리 메서드 안에서만 builder 사용 |
| 정적 팩토리 (`create`, `of`) | 외부에서 인스턴스 생성하는 진입점. 검증/기본값 책임 |
| 상태 메서드 (`markRunning`, `completeWith`) | 직접 setter 노출 안 함. 의미 있는 전이만 허용 |

#### 핵심 메서드

```java
public static RecommendationRequest create(
    String requestSlug, User user, Style style, String description,
    String productImageUrl, String productImagePath,
    String referenceImageUrl, String referenceImagePath
) {
    return RecommendationRequest.builder()
        .requestSlug(requestSlug)
        .user(user)
        // ...
        .status(RecommendationStatus.PENDING)
        .build();
}

public void completeWith(String headline, String summary, CorePoints corePoints,
    List<RecommendationItem> recommendations) {
    this.headline = headline;
    this.summary = summary;
    this.corePoints = corePoints;
    this.recommendations = recommendations;
    this.status = RecommendationStatus.COMPLETED;
}
```

`completeWith` 가 단일 메서드로 5개 필드를 동시에 갱신. 외부에서 setter 4개를 따로 호출하면 중간에 inconsistent 상태 가능 → 한 번에 처리.

### 4.5 Style enum — String → 변환 패턴

```java
public enum Style {
    STUDIO,
    BANNER_EVENT,
    SOURCE_IMAGE,
    FREEFORM;

    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public String getLabel() {
        return switch (this) {
            case STUDIO -> "스튜디오";
            // ...
        };
    }

    public static Style from(String value) {
        if (value == null) {
            throw new BadRequestException(ErrorCode.INVALID_STYLE);
        }
        try {
            return Style.valueOf(value.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.INVALID_STYLE);
        }
    }
}
```

#### 왜 `@JsonCreator/@JsonValue` 안 썼는가?

처음엔 썼다가 제거. 이유:

DTO에서 `Style style` 직접 받으면:
```
HTTP 요청 → Jackson 파싱 → Style.from() 호출 → 잘못된 값 시 IllegalArgumentException
                                              → 그러나 이건 record 생성 단계
                                              → record 못 만듦
                                              → Bean Validation 실행 안 됨
                                              → @NotBlank 메시지 못 씀
                                              → GlobalExceptionHandler가 500으로 잡음
```

해결: DTO는 `String style`로 받고, **서비스에서 명시적으로 `Style.from()`** 호출:
```java
// CreateRecommendationRequest
@NotBlank String style;

// RecommendationService
Style style = Style.from(request.style());   // 잘못되면 BadRequestException(INVALID_STYLE)
```

이러면:
- Jackson 파싱: String → String (절대 실패 안 함)
- Bean Validation: `@NotBlank` 정상 동작
- Service의 `Style.from()` 명시적 검증 → 의미 있는 400

### 4.6 RecommendationService — 동기 흐름

```java
@Transactional
public RecommendationCreatedResponse create(Long userId, CreateRecommendationRequest request) {
    // 1. style 변환 + 입력 검증
    Style style = Style.from(request.style());
    validateStudioRequiresProductImage(style, request.productImageUrl());

    // 2. User 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    // 3. RecommendationRequest 저장 (PENDING)
    RecommendationRequest entity = RecommendationRequest.create(...);
    entity = requestRepository.save(entity);

    // 4. GPT 호출
    String systemPrompt = promptCompileService.compileRecommendationSystemPrompt(style);
    String userPrompt = promptCompileService.compileRecommendationUserPrompt(...);

    GptRecommendationResponse gpt;
    try {
        gpt = openAiChatService.generateRecommendations(systemPrompt, userPrompt);
    } catch (Exception e) {
        entity.markFailed();
        throw new BadRequestException(ErrorCode.GPT_RESPONSE_INVALID);
    }

    // 5. 응답 검증 — 정확히 3건이어야 함
    if (gpt.recommendations() == null
        || gpt.recommendations().size() != EXPECTED_RECOMMENDATION_COUNT) {
        entity.markFailed();
        throw new BadRequestException(ErrorCode.GPT_RESPONSE_INVALID);
    }

    // 6. 본문을 entity 의 JSON 컬럼에 박제 + COMPLETED 로 전이
    entity.completeWith(gpt.headline(), gpt.summary(), gpt.corePoints(), gpt.recommendations());

    return RecommendationCreatedResponse.of(entity.getRequestSlug());
}
```

#### 핵심 포인트

- **트랜잭션 1개**: GPT 호출까지 한 트랜잭션 안. 실패 시 rollback (PENDING row도 사라짐). 운영자 시연용엔 단순함이 미덕.
- **검증 → 저장 → 외부 호출 → 검증 → 갱신**: 각 단계가 명확. 외부 호출 실패 시 markFailed 하지만 throw 도 하므로 결국 rollback. 다음 시도 때 새 row 생성.
- **`save()` 후 같은 entity에 setter 호출**: JPA의 dirty checking이 트랜잭션 종료 시 자동 UPDATE. 한 번 더 save() 안 불러도 됨.

### 4.7 Repository

```java
public interface RecommendationRequestRepository extends JpaRepository<RecommendationRequest, Long> {
    Optional<RecommendationRequest> findByRequestSlug(String requestSlug);
}
```

Spring Data JPA의 메서드 이름 규칙:
- `findBy{필드명}` → `WHERE {field_name} = ?` 자동 생성
- `Optional<T>` 반환 → null 가능성을 타입으로 표현

---

## 5. ImageJob aggregate

### 5.1 데이터 모델

```
ImageJob (1 row, status enum, snapshot fields)
  ├── jobSlug "bizimg_…"
  ├── user, request (FK)
  ├── style, description, productImageUrl, ... (스냅샷)
  ├── corePoints (JSON, 스냅샷)
  ├── angle, lighting, ratio (사용자 옵션, enum)
  ├── status, errorMessage
  └── variants (OneToMany)
        │
        └── ImageJobVariant (N rows, 자식 엔티티)
              ├── variantSeq (1, 2, 3 — V1/V2/V3 으로 외부 노출)
              ├── recommendationId, recommendationTitle, recommendationDescription (스냅샷)
              ├── globalLock (JSON, 스냅샷)
              ├── imagePrompt, nanobananaTaskId, resultImageUrl (워커가 채움)
              ├── errorMessage
              └── status enum
```

### 5.2 Angle / Lighting / Ratio enum

#### Angle — wire format에 숫자가 있는 케이스

```java
public enum Angle {
    FRONT,
    ANGLE_45,         // ← Java enum 이름은 숫자로 시작 못 함
    SIDE,
    TOP;

    public String getValue() {
        return switch (this) {
            case FRONT -> "front";
            case ANGLE_45 -> "45";   // ← wire format은 "45"
            case SIDE -> "side";
            case TOP -> "top";
        };
    }

    public static Angle from(String value) {
        if (value == null) throw new BadRequestException(ErrorCode.INVALID_ANGLE);
        return switch (value) {
            case "front" -> FRONT;
            case "45" -> ANGLE_45;
            case "side" -> SIDE;
            case "top" -> TOP;
            default -> throw new BadRequestException(ErrorCode.INVALID_ANGLE);
        };
    }
}
```

#### 핵심 포인트
- Java enum 이름 제약 (숫자 시작 X) → `getValue()` 와 `from()` 의 명시적 매핑이 더 안전.
- `Lighting` (`natural/warm/soft/strong`) 은 enum 이름과 wire가 일대일이라 `name().toLowerCase()` 로 충분.
- `Ratio` (`1:1/9:16/4:3`) 은 colon 때문에 enum 이름 불가 → 명시적 매핑.

### 5.3 ImageJobVariant — 스냅샷의 실체

```java
@Entity
@Table(name = "image_job_variants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageJobVariant extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ImageJob job;

    @Column(nullable = false)
    private Integer variantSeq;     // 1, 2, 3

    // 추천 스냅샷 (변경 안 됨)
    @Column(nullable = false, length = 100)
    private String recommendationId;     // GPT slug

    @Column(nullable = false, length = 200)
    private String recommendationTitle;  ← 그 시점 추천 데이터를 박제

    @Column(columnDefinition = "TEXT")
    private String recommendationDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private GlobalLock globalLock;       ← 그 시점 GlobalLock 박제

    // 워커가 채우는 필드
    @Column(columnDefinition = "TEXT")
    private String imagePrompt;          ← Nanobanana 호출 prompt

    @Column(length = 100)
    private String nanobananaTaskId;     ← 결과 추적용 (Gemini는 sync라 자체 발급)

    @Column(columnDefinition = "TEXT")
    private String resultImageUrl;       ← S3 URL

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    private VariantStatus status;
    
    // ...
}
```

#### 스냅샷 필드의 의미

`recommendationTitle`, `recommendationDescription`, `globalLock` 은 **`RecommendationRequest.recommendations[i]` 의 그 시점 값을 복사**한 것. 추천 데이터가 나중에 바뀌어도 (실제론 immutable이지만) variant는 영향 없음.

#### `getVariantId()` 메서드

```java
public String getVariantId() {
    return "V" + variantSeq;
}
```

DB에는 `variantSeq: Integer (1, 2, 3)` 으로 저장. 외부 노출 시 `"V1", "V2", "V3"` 로 파생.

이렇게 한 이유:
- DB는 정수가 정렬/인덱스에 효율적
- 외부 spec은 `"V1"` 형식 요구
- 양쪽 다 만족하면서 표현/저장 분리

### 5.4 ImageJob — 부모 엔티티

#### 스냅샷 필드 (RecommendationRequest에서 복사)

```java
@Enumerated(EnumType.STRING) private Style style;
@Column(columnDefinition = "TEXT") private String description;
@Column(columnDefinition = "TEXT") private String productImageUrl;
@Column(columnDefinition = "TEXT") private String productImagePath;
@Column(columnDefinition = "TEXT") private String referenceImageUrl;

@JdbcTypeCode(SqlTypes.JSON)
@Column(columnDefinition = "JSON")
private CorePoints corePoints;
```

이걸 굳이 ImageJob에 복사하는 이유는 *spec이 그렇게 하라고 함*. 워커가 이 값을 사용해서 Nanobanana prompt 합성. RecommendationRequest 를 매번 lazy load 하면 N+1 + 결합도 + 트랜잭션 경계 문제 → 스냅샷이 더 단순.

#### `@OneToMany` + 양방향 매핑

```java
@OneToMany(mappedBy = "job", cascade = CascadeType.ALL,
    orphanRemoval = true, fetch = FetchType.LAZY)
@OrderBy("variantSeq ASC")
private List<ImageJobVariant> variants = new ArrayList<>();
```

| 옵션 | 의미 |
|---|---|
| `mappedBy = "job"` | 양방향 관계의 owner 가 child(`ImageJobVariant.job`) 라는 의미. 부모는 mirror. |
| `cascade = CascadeType.ALL` | job 저장하면 variants 도 함께 저장/삭제. JPA 의 cascade. |
| `orphanRemoval = true` | variants 리스트에서 빼면 DB 에서도 삭제. |
| `fetch = FetchType.LAZY` | 명시적으로 안 해도 OneToMany 기본은 LAZY. |
| `@OrderBy("variantSeq ASC")` | 조회 시 variantSeq 오름차순 정렬 (V1, V2, V3 순) |

#### `addVariant()` 메서드

```java
public void addVariant(ImageJobVariant variant) {
    this.variants.add(variant);
}
```

부모-자식 관계 설정의 표준 패턴. 양방향 일관성을 위해 child의 `job` 도 같이 세팅하는 게 정석이지만, 우리는 `ImageJobVariant.create(job, ...)` 가 이미 `this.job = job` 박아놨으니 생략.

#### 상태 머신 메서드

```java
public void markRunning() { this.status = JobStatus.RUNNING; }
public void markCompleted() { this.status = JobStatus.COMPLETED; }
public void markPartial() { this.status = JobStatus.PARTIAL; }
public void markFailed(String errorMessage) {
    this.status = JobStatus.FAILED;
    this.errorMessage = errorMessage;
}

public void finalizeStatus() {
    long total = variants.size();
    long succeeded = variants.stream().filter(v -> v.getStatus().name().equals("COMPLETED")).count();
    long failed = variants.stream().filter(v -> v.getStatus().name().equals("FAILED")).count();

    if (succeeded == total)      this.status = JobStatus.COMPLETED;
    else if (failed == total)    this.status = JobStatus.FAILED;
    else if (succeeded > 0)      this.status = JobStatus.PARTIAL;
    else                          this.status = JobStatus.FAILED;
}
```

`finalizeStatus()` 가 핵심. 워커가 모든 variant 처리 후 호출하면 잡 전체 상태를 자동 결정.

### 5.5 ImageJobService.create — 6단계

```java
@Transactional
public ImageJobCreatedResponse create(Long userId, GenerateImageRequest request) {
    // 1. enum 변환 + 유효성 검증
    Angle angle = Angle.from(request.angle());
    Lighting lighting = Lighting.from(request.lighting());
    Ratio ratio = Ratio.from(request.ratio());

    // 2. RecommendationRequest 조회 + 권한 + 상태 검증
    RecommendationRequest req = requestRepository.findByRequestSlug(request.requestId())
        .orElseThrow(() -> new NotFoundException(ErrorCode.RECOMMENDATION_NOT_FOUND));

    if (!req.getUser().getId().equals(userId)) {
        throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
    }
    if (req.getStatus() != RecommendationStatus.COMPLETED) {
        throw new BadRequestException(ErrorCode.RECOMMENDATION_NOT_READY);
    }

    // 3. recommendationIds 검증 — request.recommendations 안에 실제로 존재해야 함
    List<RecommendationItem> selected = filterSelectedRecommendations(req, request.recommendationIds());

    // 4. User 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

    // 5. ImageJob 생성 (PENDING) + variants 스냅샷
    ImageJob job = ImageJob.create(
        slugGenerator.generate(SLUG_PREFIX), user, req, angle, lighting, ratio
    );
    int seq = 1;
    for (RecommendationItem item : selected) {
        ImageJobVariant variant = ImageJobVariant.create(
            job, seq++,
            item.id(), item.title(), item.description(), item.globalLock()
        );
        job.addVariant(variant);
    }
    job = jobRepository.save(job);

    // 6. 비동기 워커 트리거 (트랜잭션 커밋 후에 처리됨)
    eventPublisher.publishEvent(new ImageJobCreatedEvent(job.getId()));

    return ImageJobCreatedResponse.of(job.getJobSlug());
}
```

#### 핵심 포인트

- **검증 단계 (1~3)** 모두 통과 후에야 DB 작업 시작. fail-fast.
- **스냅샷 복사 (5)**: `ImageJobVariant.create` 가 추천 데이터(`item.id()`, `item.title()`, `item.description()`, `item.globalLock()`) 를 인자로 받아 자기 안에 박제.
- **`save(job)` 1번**: cascade 덕분에 variants 도 함께 INSERT.
- **`publishEvent` (6)**: 트랜잭션 커밋 후 워커가 동작하도록 이벤트 발행. 자세한 건 9장.

### 5.6 ImageJobService.get — 폴링 응답

```java
public ImageJobResponse get(Long userId, String jobId) {
    ImageJob job = jobRepository.findWithVariantsByJobSlug(jobId)
        .orElseThrow(() -> new NotFoundException(ErrorCode.IMAGE_JOB_NOT_FOUND));

    if (!job.getUser().getId().equals(userId)) {
        throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
    }

    List<ImageEdit> edits = editRepository.findAllByRootJob_IdOrderByCreatedAtAsc(job.getId());
    return ImageJobResponse.of(job, edits);
}
```

#### `@EntityGraph` 의 역할

```java
@EntityGraph(attributePaths = {"variants", "user", "request"})
Optional<ImageJob> findWithVariantsByJobSlug(String jobSlug);
```

기본 LAZY fetch면 다음과 같이 됨:
- `findByJobSlug` → ImageJob 1 query
- `job.getVariants()` 호출 시 → 추가 1 query (variants)
- `job.getUser().getId()` 호출 시 → 추가 1 query (user)
- ... N+1 문제

`@EntityGraph` 는 한 번에 JOIN FETCH로 가져오게 명시. 폴링이라 자주 호출되므로 성능 차이 큼.

#### `findAllByRootJob_IdOrderByCreatedAtAsc`

JPA 메서드 이름 규칙의 property path 문법:
- `RootJob_Id` → `rootJob.id` 필드 접근
- `OrderBy{Field}Asc` → `ORDER BY {field} ASC`

→ `WHERE root_job_id = ? ORDER BY created_at ASC` 자동 생성.

### 5.7 ImageJobResponse — DTO 매핑

```java
public record ImageJobResponse(
    boolean ok,
    JobView job,
    List<EditView> edits
) {
    public record JobView(
        String jobId,
        Long userId,
        String requestId,
        String style,
        String styleLabel,
        // ...
        List<VariantView> variants,
        String status,
        String error,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static JobView from(ImageJob j) { ... }
    }

    public record VariantView(...) {
        public static VariantView from(ImageJobVariant v) { ... }
    }
    
    // ...

    public static ImageJobResponse of(ImageJob job, List<ImageEdit> edits) {
        return new ImageJobResponse(true, JobView.from(job), edits.stream().map(EditView::from).toList());
    }
}
```

#### 핵심 포인트
- **record 안에 record 중첩**: 응답이 nested 구조라 자연스러움.
- **`from(entity)` 정적 팩토리**: 엔티티 → DTO 변환 위치를 DTO 안에 둠. 양쪽 모두 응집도 ↑.
- **`Instant createdAt`**: `Timestamp.toInstant()` 로 변환. ISO 8601 직렬화 자연스러움.
- **`getValue()` 호출**: enum 직렬화 시 `Style.STUDIO.getValue() = "studio"` 로 wire format 만듦. enum 직접 노출하면 Jackson이 `"STUDIO"` 로 직렬화해서 spec 안 맞음.

---

## 6. Prompt 합성 서비스

### 6.1 의도

Phase 1 (GPT 추천), Phase 2 (Nanobanana 이미지 생성), Phase 3 (Nanobanana 편집) 모두 prompt가 필요. 각 phase의 prompt 합성 로직을 한 클래스에 모음.

### 6.2 Phase 1 — 시스템 프롬프트

```java
private static final String RECOMMENDATION_SYSTEM_PROMPT_TEMPLATE = """
    너는 제품 사진 컨셉을 추천하는 AI 디렉터다.
    사용자가 입력한 정보를 바탕으로 정확히 3개의 컨셉 추천을 만든다.

    [현재 스타일]
    %s — %s
    %s

    [작성 규칙]
    - title, description, tags 는 한국어로 작성
    - id 는 영문 snake_case 로 짧고 의미있게 (예: warm_wood_studio)
    - globalLock 의 4개 필드(background, surface, lighting, mood) 는 영문으로
    - recommended 는 셋 중 가장 추천하는 1건만 true
    - 3개의 추천이 서로 다른 분위기/방향을 가지도록
    """;

public String compileRecommendationSystemPrompt(Style style) {
    return RECOMMENDATION_SYSTEM_PROMPT_TEMPLATE.formatted(
        style.getValue(), style.getLabel(), recommendationContextHint(style)
    );
}
```

#### Java 15+ Text Block (`"""`)
- 멀티라인 문자열
- 들여쓰기 자동 정리
- 이스케이프 줄어듦

#### `style` 별 분기 — `recommendationContextHint`

```java
private String recommendationContextHint(Style style) {
    return switch (style) {
        case STUDIO -> """
            - 깔끔한 스튜디오 환경에서 제품을 단독 부각
            - 배경/표면/조명을 정교하게 통제
            """;
        case BANNER_EVENT -> """
            - 캠페인/배너용 풍부한 디렉션
            - 텍스트 / 카피 / 인물 묘사 금지
            """;
        // ...
    };
}
```

`switch expression` (Java 14+) 의 활용. 모든 case를 다루면 컴파일러가 exhaustive 체크.

### 6.3 Phase 2 — 이미지 생성 프롬프트

```java
public String compileImageGenerationPrompt(ImageJob job, ImageJobVariant variant) {
    GlobalLock lock = variant.getGlobalLock();

    StringBuilder sb = new StringBuilder();
    sb.append("Premium product photography.\n");
    sb.append("Concept: ").append(variant.getRecommendationTitle()).append(".\n");

    if (lock != null) {
        if (lock.background() != null) sb.append("Background: ").append(lock.background()).append(".\n");
        if (lock.surface() != null) sb.append("Surface: ").append(lock.surface()).append(".\n");
        if (lock.lighting() != null) sb.append("Lighting: ").append(lock.lighting()).append(".\n");
        if (lock.mood() != null) sb.append("Mood: ").append(lock.mood()).append(".\n");
    }

    sb.append("Camera angle: ").append(job.getAngle().getValue()).append(".\n");
    sb.append("Lighting style preference: ").append(job.getLighting().getValue()).append(".\n");
    sb.append("Aspect ratio: ").append(job.getRatio().getValue()).append(".\n");

    if (job.getDescription() != null && !job.getDescription().isBlank()) {
        sb.append("User description: ").append(job.getDescription()).append("\n");
    }
    sb.append("Constraints: no text overlay, no people, no logos.\n");
    return sb.toString();
}
```

#### 핵심 포인트

- **GlobalLock 필드를 영문 그대로 prompt 에 박음**: GPT가 영문으로 만들어놨으니 번역 없이 사용 → 일관성 유지.
- **사용자 옵션과 globalLock 분리**: globalLock 은 추천의 정체성, angle/lighting/ratio 는 사용자가 매번 다르게 선택. 별도 줄로 명시.
- **금지 제약 (`Constraints`)**: 모델이 종종 텍스트/사람을 그려넣는 걸 명시적으로 막음.
- **null 체크**: globalLock 필드 일부가 null 일 수 있으므로 방어.

---

## 7. OpenAI 통합

### 7.1 ChatClient 빈

```java
@Configuration
public class OpenAiConfig {

    @Bean
    public ChatClient openAiChatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
```

#### Spring AI starter 의 자동 설정

`spring-ai-starter-model-openai` 가 의존성에 들어가면, 자동으로:
- `application.yml` 의 `spring.ai.openai.*` 키 읽음
- `OpenAiChatModel` 빈 등록
- `ChatClient.Builder` 빈 등록

우리는 그 builder 를 받아 `.build()` 만 호출하면 ChatClient 인스턴스 완성. 빈으로 등록해서 모든 서비스에서 주입 가능.

#### 향후 확장
```java
return builder
    .defaultSystem("기본 시스템 프롬프트")          // 모든 호출 공통
    .defaultOptions(OpenAiChatOptions.builder()
        .withTemperature(0.7)
        .build())
    .build();
```

이렇게 default 를 박아두면 호출처마다 반복 안 해도 됨.

### 7.2 OpenAiChatService — Spring AI 한 줄

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiChatService {

    private final ChatClient chatClient;

    public GptRecommendationResponse generateRecommendations(String systemPrompt, String userPrompt) {
        log.info("[OpenAI] generateRecommendations user prompt length={}", userPrompt.length());

        GptRecommendationResponse response = chatClient.prompt()
            .system(systemPrompt)
            .user(userPrompt)
            .call()
            .entity(GptRecommendationResponse.class);

        log.info("[OpenAI] response received: {} recommendations",
            response.recommendations() != null ? response.recommendations().size() : 0);
        return response;
    }
}
```

#### `.entity(GptRecommendationResponse.class)` 가 하는 일

내부 동작 단계:
1. record 구조 분석 → JSON schema 생성
2. 시스템 프롬프트 끝에 자동 부착: "이 schema 모양으로 JSON 응답하라" (model 별로 OpenAI는 `response_format=json_schema` 자동 사용)
3. 모델 호출 → JSON 응답 받음
4. Jackson으로 record 인스턴스로 역직렬화
5. 결과 반환

이 한 줄이 직접 짜면 30~50줄 (prompt 추가, 응답 검증, JSON 파싱, 에러 매핑).

### 7.3 GptRecommendationResponse — 도메인 record 재사용

```java
public record GptRecommendationResponse(
    String headline,
    String summary,
    CorePoints corePoints,                    // ← 도메인 value object
    List<RecommendationItem> recommendations  // ← 도메인 value object
) {
}
```

#### 도메인 → external 의존성

`external/openai/dto/GptRecommendationResponse` 가 `domain/business/recommendation/entity/value/CorePoints` 를 import.

일반적으로 `external` 이 `domain` 에 의존하는 건 의심할 만한 방향이지만, 여기서는 OK인 이유:
- value object 들은 **행위 없는 안정 계약** (record + 불변 필드만)
- 도메인 모양과 GPT 응답 모양이 1:1 → 별도 mapper 클래스 만들면 코드만 늘어남
- 단방향 의존만 있고 순환 없음

대안: `external/openai/dto/` 에 별도 record 정의 + 서비스에서 변환. 더 보수적이지만 verbose.

---

## 8. Nanobanana 통합

### 8.1 Properties

```java
@Getter
@Setter
@ConfigurationProperties(prefix = "nanobanana")
public class NanobananaProperties {

    private String apiKey;
    private String baseUrl = "https://generativelanguage.googleapis.com";
    private String model = "gemini-2.5-flash-image-preview";
}
```

#### `@ConfigurationProperties`
- `application.yml` 의 `nanobanana.*` 키를 자동 바인딩
- 메인 클래스의 `@ConfigurationPropertiesScan` 덕분에 자동 등록

#### `@Setter` 필요한 이유
`@ConfigurationProperties` 는 setter로 주입. Lombok `@Setter` 가 그걸 만들어줌. 또는 record 로도 가능 (`@ConstructorBinding`).

### 8.2 Feign 인터페이스

```java
@FeignClient(
    name = "nanobananaFeignClient",
    url = "${nanobanana.base-url:https://generativelanguage.googleapis.com}",
    configuration = NanobananaFeignConfig.class
)
public interface NanobananaFeignClient {

    @PostMapping("/v1beta/models/{model}:generateContent")
    GenerateContentResponse generate(
        @PathVariable("model") String model,
        @RequestBody GenerateContentRequest request
    );
}
```

#### 핵심 포인트

- **`name`**: Feign 빈 식별자. unique 해야 함.
- **`url`**: 베이스 URL. SpEL (`${...:default}`) 로 application.yml 참조 + default.
- **`configuration`**: 이 client에만 적용할 인증/디코더. 전역 Feign 설정 안 건드림.
- **path variable with colon**: `{model}:generateContent` — `:` 가 path 안에 있어도 OK. Feign이 처리.

### 8.3 Auth 인터셉터

```java
@RequiredArgsConstructor
public class NanobananaFeignConfig {

    private final NanobananaProperties properties;

    @Bean
    public RequestInterceptor nanobananaAuthInterceptor() {
        return template -> template.header("x-goog-api-key", properties.getApiKey());
    }
}
```

#### 왜 클래스에 `@Configuration` 안 붙임?

붙이면 Spring 이 application context 에 자동 등록 → 모든 Feign client에 인터셉터 적용됨 (Kakao/Google 까지) → 의도치 않은 헤더 추가.

`@FeignClient(configuration = ...)` 로 명시 지정해야만 활성화되는 게 OpenFeign의 권장 패턴.

### 8.4 Request DTO — record 중첩 + factory

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerateContentRequest(
    List<Content> contents,
    GenerationConfig generationConfig
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(String role, List<Part> parts) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Part(String text, InlineData inlineData) {
        public static Part text(String value) {
            return new Part(value, null);
        }
        public static Part inlineData(byte[] bytes, String mimeType) {
            return new Part(null, new InlineData(mimeType, Base64.getEncoder().encodeToString(bytes)));
        }
    }

    public record InlineData(String mimeType, String data) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerationConfig(List<String> responseModalities) {}

    // ---------- factory methods ----------

    public static GenerateContentRequest textOnly(String prompt) {
        return new GenerateContentRequest(
            List.of(new Content("user", List.of(Part.text(prompt)))),
            new GenerationConfig(List.of("TEXT", "IMAGE"))
        );
    }

    public static GenerateContentRequest textWithImage(String prompt, byte[] imageBytes, String mimeType) {
        return new GenerateContentRequest(
            List.of(new Content("user", List.of(
                Part.text(prompt),
                Part.inlineData(imageBytes, mimeType)
            ))),
            new GenerationConfig(List.of("TEXT", "IMAGE"))
        );
    }
}
```

#### 핵심 포인트

- **`@JsonInclude(NON_NULL)`**: null 필드는 JSON 에 포함 안 함. Gemini는 `text` 와 `inlineData` 중 하나만 들어가야 하는데, 다른 하나가 null로 직렬화되면 모델이 혼란 → null 제외.
- **record 안에 record 중첩**: 응답 구조가 자연스럽게 표현됨. 파일 1개로 끝.
- **factory methods**: `Part.text(...)`, `Part.inlineData(...)` — 사용자가 둘 중 하나만 채우게 강제.
- **Base64 인코딩**: Gemini 는 inlineData 에 base64 문자열을 받음. 이미지 바이트를 보낼 때 인코딩 필요.

### 8.5 Response DTO — `@JsonIgnoreProperties(ignoreUnknown = true)`

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerateContentResponse(List<Candidate> candidates) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content, String finishReason, Integer index) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(String role, List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text, InlineData inlineData) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InlineData(String mimeType, String data) {}

    public Optional<InlineData> firstInlineImage() {
        if (candidates == null || candidates.isEmpty()) return Optional.empty();
        Candidate first = candidates.get(0);
        if (first == null || first.content() == null || first.content().parts() == null) return Optional.empty();
        return first.content().parts().stream()
            .map(Part::inlineData)
            .filter(d -> d != null && d.data() != null)
            .findFirst();
    }
}
```

#### `@JsonIgnoreProperties(ignoreUnknown = true)`

Gemini 응답에는 `usageMetadata`, `safetyRatings` 등 우리가 안 쓰는 필드가 많음. 이 어노테이션 없으면 Jackson이 "필드 매핑 안 됨" 오류 던짐. `ignoreUnknown = true` 로 흡수.

#### Helper method `firstInlineImage()`

응답 파싱 로직을 DTO 안에 둠. 호출처(`NanobananaService`)는 `response.firstInlineImage().orElseThrow(...)` 한 줄.

방어 코드 (`null` 체크) 가 많은 이유: Gemini 응답이 unstable할 수 있음 (모델 거부, safety filter 등). null safety 로 NPE 방지.

### 8.6 NanobananaService — Feign + base64 + S3

```java
public NanobananaResult generateImage(String prompt, String sourceImageUrl) {
    log.info("[Nanobanana] generateImage prompt length={}, sourceUrl={}", prompt.length(), sourceImageUrl);

    GenerateContentRequest request = GenerateContentRequest.textOnly(prompt);
    GenerateContentResponse response;
    try {
        response = feignClient.generate(properties.getModel(), request);
    } catch (Exception e) {
        log.error("[Nanobanana] API 호출 실패", e);
        throw new RuntimeException("Nanobanana API 호출 실패: " + e.getMessage(), e);
    }

    GenerateContentResponse.InlineData image = response.firstInlineImage()
        .orElseThrow(() -> new RuntimeException("Nanobanana 응답에 이미지가 없습니다"));

    // base64 → 바이트 → S3 업로드
    byte[] imageBytes = Base64.getDecoder().decode(image.data());
    String mimeType = image.mimeType() != null ? image.mimeType() : "image/png";
    String extension = mimeTypeToExtension(mimeType);
    String taskId = "nb_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    String key = S3_KEY_PREFIX + "/" + taskId + "." + extension;

    String publicUrl = s3Service.uploadBytes(key, imageBytes, mimeType);
    return new NanobananaResult(taskId, publicUrl);
}
```

#### 데이터 흐름

```
String prompt
   │
   ▼
GenerateContentRequest.textOnly(prompt)        ← Gemini 형식 변환
   │
   ▼
feignClient.generate(model, request)            ← HTTP POST
   │
   ▼
GenerateContentResponse (base64 이미지 포함)
   │
   ▼
firstInlineImage() → InlineData(mimeType, data)
   │
   ▼ Base64.decode
byte[] imageBytes
   │
   ▼
s3Service.uploadBytes(key, bytes, mimeType)    ← S3 PUT
   │
   ▼
publicUrl: https://my-bucket.s3.ap-northeast-2.amazonaws.com/business_images/nb_xxx.png
   │
   ▼
NanobananaResult(taskId, publicUrl)
```

#### 핵심 포인트

- **`nanobananaTaskId` 자체 발급**: Gemini는 sync API라 별도 taskId가 없음. `nb_` prefix 로 추적용 ID 만듦.
- **MIME 타입 → 확장자**: S3 key에 확장자 붙이면 브라우저/뷰어가 알아서 표시.
- **에러 흡수**: 모든 실패를 RuntimeException으로 wrap → 워커가 catch → variant.markFailed.

---

## 9. 비동기 인프라

### 9.1 AsyncConfig — TaskExecutor 빈

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "businessWorkerExecutor")
    public TaskExecutor businessWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("biz-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

#### 각 옵션의 의미

| 옵션 | 값 | 의미 |
|---|---|---|
| `corePoolSize` | 4 | 평소 idle 상태에서도 유지하는 스레드 수 |
| `maxPoolSize` | 8 | 큐가 꽉 찼을 때 임시로 늘릴 수 있는 최대치 |
| `queueCapacity` | 100 | 풀이 꽉 찼을 때 작업이 대기할 수 있는 자리 수 |
| `threadNamePrefix` | `biz-worker-` | 스레드 이름 prefix. 로그에서 `biz-worker-1` 처럼 보임 |
| `rejectedExecutionHandler` | CallerRunsPolicy | 풀+큐 모두 꽉 찼을 때 거부 정책. CallerRuns: 호출 스레드(=request thread)가 직접 실행 → 데이터 손실 X |

#### 작업 도착 시 동작 순서

1. idle 스레드 있음 → 즉시 실행
2. 없음 + corePoolSize 미달 → 새 스레드 생성
3. corePoolSize 도달 → 큐에 대기
4. 큐도 꽉 참 → maxPoolSize까지 임시 스레드 생성
5. max도 꽉 참 → 거부 정책 발동 (우리는 CallerRuns)

#### `@EnableAsync` 위치

- 메인 클래스 OR 어떤 `@Configuration` 클래스 한 곳
- 우리는 `AsyncConfig` 에 넣어서 메인 클래스 깔끔하게 유지.

### 9.2 이벤트 record

```java
package com.monovai.worker.business.event;

public record ImageJobCreatedEvent(Long jobId) {
}
```

#### 한 줄 record의 가치
- 타입으로 의도 명시 (`Long jobId` 가 단순 Long이 아니라 "ImageJob 생성 이벤트의 jobId")
- 추후 필드 추가 쉬움 (`ImageJobCreatedEvent(Long jobId, Long userId, ...)`)
- 컴파일러가 listener 시그니처 검증

### 9.3 ImageGenerationWorker — AFTER_COMMIT + @Async

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationWorker {

    private final ImageJobRepository jobRepository;
    private final NanobananaService nanobananaService;
    private final PromptCompileService promptCompileService;

    @Async("businessWorkerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobCreated(ImageJobCreatedEvent event) {
        log.info("[ImageGenWorker] event received: jobId={}", event.jobId());
        try {
            processJob(event.jobId());
        } catch (Exception e) {
            log.error("[ImageGenWorker] processJob 전체 실패: jobId={}", event.jobId(), e);
            safeMarkJobFailed(event.jobId(), e.getMessage());
        }
    }

    @Transactional
    protected void processJob(Long jobId) {
        ImageJob job = jobRepository.findById(jobId).orElseThrow();
        if (job.getStatus() != JobStatus.PENDING) {
            log.warn("[ImageGenWorker] 이미 처리됨/처리 중, 무시: jobId={}, status={}", jobId, job.getStatus());
            return;
        }

        job.markRunning();

        for (ImageJobVariant v : job.getVariants()) {
            processVariant(job, v);
        }

        job.finalizeStatus();
    }

    private void processVariant(ImageJob job, ImageJobVariant variant) {
        try {
            String prompt = promptCompileService.compileImageGenerationPrompt(job, variant);
            variant.markRunning(prompt);

            NanobananaResult result = nanobananaService.generateImage(prompt, job.getProductImageUrl());
            variant.markSucceeded(result.taskId(), result.resultImageUrl());
        } catch (Exception e) {
            log.error("[ImageGenWorker] variant 실패: jobId={}, variantSeq={}", job.getId(), variant.getVariantSeq(), e);
            variant.markFailed(e.getMessage());
            // throw 안 함 — 다음 variant 영향 안 주려고
        }
    }

    @Transactional
    protected void safeMarkJobFailed(Long jobId, String message) {
        try {
            jobRepository.findById(jobId).ifPresent(j -> j.markFailed(message));
        } catch (Exception e) {
            log.error("[ImageGenWorker] markFailed 까지 실패: jobId={}", jobId, e);
        }
    }
}
```

#### 어노테이션 조합의 의미

```
@Async("businessWorkerExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onJobCreated(...)
```

이 두 줄이 동시에 적용되어:
1. `ImageJobService.create` 가 `publishEvent` 함
2. Spring 이 트랜잭션 커밋 후 (`AFTER_COMMIT`) 까지 이벤트 dispatch 보류
3. 커밋되면 dispatch
4. `@Async` 가 가로채서 `businessWorkerExecutor` 풀에 작업 제출
5. 별 스레드에서 `onJobCreated` 실행

#### 왜 `processJob` 을 별도 메서드로?

```java
public void onJobCreated(ImageJobCreatedEvent event) {  // 트랜잭션 X
    processJob(event.jobId());                          // 새 트랜잭션 시작
}

@Transactional
protected void processJob(Long jobId) { ... }
```

`@TransactionalEventListener` 진입은 트랜잭션 외부 (이미 AFTER_COMMIT). 처리 중 새 트랜잭션 필요해서 `@Transactional` 메서드를 별도로 분리.

다만 같은 클래스 내 호출은 self-invocation 함정! `this.processJob(...)` 이라 프록시를 안 거침. 그래도 OK인 이유는 **`@Async` 가 이미 프록시를 거치게 만들었기 때문**. `@Async` 메서드 안에서의 호출은 별 스레드에서 일어나고, 프록시로 진입한 거라 내부 호출도 transactional 적용됨.

(이건 미묘한 부분 — 더 안전하게 가려면 별 빈으로 분리. 우리는 단순화 위해 같은 클래스.)

#### Idempotency

```java
if (job.getStatus() != JobStatus.PENDING) {
    log.warn("이미 처리됨/처리 중, 무시");
    return;
}
```

같은 잡이 두 번 트리거되더라도 두 번째는 무시 → 멱등성 보장.

#### Variant 단위 격리

```java
for (ImageJobVariant v : job.getVariants()) {
    processVariant(job, v);   // 내부에서 try/catch
}
```

variant 1개 실패가 다른 variant에 영향 안 가도록 try/catch 로 흡수. 잡 전체 상태는 `finalizeStatus()` 가 다시 결정 (모두 성공 → COMPLETED, 일부 → PARTIAL, 전체 실패 → FAILED).

### 9.4 ImageJobService → Worker 통신

```java
// ImageJobService.create
job = jobRepository.save(job);
eventPublisher.publishEvent(new ImageJobCreatedEvent(job.getId()));
return ImageJobCreatedResponse.of(job.getJobSlug());
```

**`ApplicationEventPublisher` 주입** 으로 이벤트 발행. Service는 worker 클래스를 모름. 단방향 의존성.

향후 SQS로 마이그레이션 시 변화:
- listener 안의 구현만 SQS publish 로 변경
- Service 코드는 그대로

---

## 10. 보조 인프라

### 10.1 SlugGenerator

```java
@Component
public class SlugGenerator {

    public String generate(String prefix) {
        long epochSec = System.currentTimeMillis() / 1000;
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return prefix + "_" + epochSec + "_" + random;
    }
}
```

#### 결과 예시
```
bizrec_1714541234_abc123
bizimg_1714541987_def456
bizedit_1714542100_ghi789
```

#### 왜 이 형식?

- `prefix`: 리소스 종류를 즉시 인지 (`bizrec/bizimg/bizedit`)
- `epochSec`: 정렬 가능 (생성 시각순)
- `random6`: 추측 불가 (4 × 10⁹ 조합)

#### 주의

- **PK 아님**: DB는 별도 `Long id` 가짐. slug 는 외부 노출용.
- **충돌 가능성**: 같은 초에 random 6자가 겹칠 확률. 1 QPS에서는 무시할 수 있지만 100 QPS+ 라면 random 길이 늘리거나 ULID 도입 검토.

### 10.2 S3Service.uploadBytes

```java
public String uploadBytes(String key, byte[] data, String contentType) {
    ObjectMetadata metadata = ObjectMetadata.builder()
        .contentType(contentType)
        .build();
    try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
        s3Template.upload(bucketName, key, input, metadata);
    } catch (Exception e) {
        throw new RuntimeException("S3 업로드 실패: " + key, e);
    }
    return getPublicKey(key);
}
```

#### 흐름

1. `byte[]` → `ByteArrayInputStream` (S3Template은 InputStream 받음)
2. ObjectMetadata 에 contentType 넣어서 S3 가 적절한 헤더 반환하게
3. `try-with-resources` 로 stream 자동 close
4. 업로드 후 public URL 생성 (`getPublicKey` — 기존 메서드)

#### Spring Cloud AWS의 한계

`ObjectMetadata.Builder` 에 `contentLength` 메서드 없음 (지원하지 않음). 우리는 우회: contentType만 명시. S3가 byte 길이 자동 계산.

### 10.3 ErrorCode 추가

```java
INVALID_STYLE                E400020
STUDIO_PRODUCT_IMAGE_REQUIRED E400021
INVALID_ANGLE                E400022
INVALID_LIGHTING             E400023
INVALID_RATIO                E400024
RECOMMENDATION_NOT_READY     E400025
RECOMMENDATION_NOT_FOUND     E404011
RECOMMENDATION_ID_NOT_FOUND  E404012
IMAGE_JOB_NOT_FOUND          E404013
GPT_RESPONSE_INVALID         E502001
```

#### 새 코드 부여 규칙

- 4xx 카테고리별로 prefix (E400xxx, E404xxx)
- 502 (Bad Gateway) 는 외부 서비스 실패용
- 번호는 마지막 사용 번호 + 1 (충돌 회피)

#### `BadRequestException(ErrorCode.X)` 패턴

```java
throw new BadRequestException(ErrorCode.INVALID_STYLE);
```

- `BusinessException` 의 자식 클래스가 4xx 별로 있음 (`BadRequestException`, `NotFoundException`, `ForbiddenException`, `UnauthorizedException`)
- `GlobalExceptionHandler` 가 모두 잡아서 `ErrorCode.httpStatus` 로 응답

---

## 부록: 흐름도

### A. POST /business/recommendations 전체 흐름

```
[HTTP 요청]
    │ { style: "studio", description: "...", productImageUrl: "..." }
    ▼
RecommendationController.create
    │ @Valid 검증 (@NotBlank style)
    ▼
RecommendationService.create  @Transactional
    │ 1. Style.from("studio") → Style.STUDIO
    │ 2. validateStudioRequiresProductImage
    │ 3. UserRepository.findById
    │ 4. RecommendationRequest.create + save (PENDING)
    │ 5. PromptCompileService.compileRecommendationSystemPrompt(STUDIO)
    │ 6. PromptCompileService.compileRecommendationUserPrompt(...)
    ▼
OpenAiChatService.generateRecommendations
    │ chatClient.prompt().system(...).user(...).call().entity(...)
    ▼
[OpenAI API]  (1~3초)
    │ JSON 응답: { headline, summary, corePoints, recommendations[3] }
    ▼
GptRecommendationResponse 인스턴스 (record 자동 매핑)
    │
    ▼ RecommendationService 로 복귀
    │ 7. 검증 (recommendations.size() == 3)
    │ 8. entity.completeWith(...) → status=COMPLETED, JSON 컬럼에 박제
    │ 9. requestSlug 반환
    ▼
[HTTP 응답]
    { status: 201, message: "생성이 완료되었습니다",
      data: { requestId: "bizrec_..." } }
```

### B. POST /business/generate-image + 비동기 처리

```
[HTTP 요청]
    │ { requestId: "bizrec_...", recommendationIds: [...], angle, lighting, ratio }
    ▼
ImageJobController.generate
    │ @Valid 검증
    ▼
ImageJobService.create  @Transactional
    │ 1. enum 변환 (Angle/Lighting/Ratio.from)
    │ 2. RecommendationRequest 조회 + 권한/상태 검증
    │ 3. recommendationIds 검증 (실제 존재하는지)
    │ 4. User 조회
    │ 5. ImageJob.create + variants 스냅샷 + save (PENDING)
    │ 6. eventPublisher.publishEvent(new ImageJobCreatedEvent(jobId))
    │ 7. return { jobId: "bizimg_..." }
    ▼
[트랜잭션 커밋]
    │
    ▼ (이벤트 dispatch — AFTER_COMMIT)
    │
[biz-worker-1 스레드]
ImageGenerationWorker.onJobCreated
    │
    ▼ processJob @Transactional
    │ Job.markRunning
    │
    │ for each variant:
    │     ▼ processVariant
    │     │ promptCompileService.compileImageGenerationPrompt
    │     │ variant.markRunning(prompt)
    │     │
    │     ▼ NanobananaService.generateImage
    │     │ feignClient.generate → POST Gemini API
    │     │ response.firstInlineImage() → base64
    │     │ Base64.decode → byte[]
    │     │ s3Service.uploadBytes → S3 URL
    │     │ return NanobananaResult(taskId, url)
    │     │
    │     ▼ variant.markSucceeded(taskId, url)
    │
    ▼ Job.finalizeStatus (COMPLETED/PARTIAL/FAILED)

[GET /business/image-job?jobId=]  (프론트가 2초마다 폴링)
    ▼
ImageJobService.get
    │ jobRepository.findWithVariantsByJobSlug (@EntityGraph)
    │ 권한 검증
    │ editRepository.findAllByRootJob_IdOrderByCreatedAtAsc
    ▼
ImageJobResponse.of(job, edits)
    ▼
[HTTP 응답]
    { ok, job: { ... variants: [...] }, edits: [] }
```

---

## 부록: 코드 템플릿 모음

### 1. JPA 엔티티 (snapshot 패턴)
```java
@Entity
@Table(name = "...")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Foo extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ... 스냅샷 필드들

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private SomeRecord nestedData;

    @Enumerated(EnumType.STRING)
    private FooStatus status;

    @Builder(access = AccessLevel.PRIVATE)
    private Foo(...) { ... }

    public static Foo create(...) {
        return Foo.builder().status(PENDING).build();
    }

    public void markRunning() { this.status = RUNNING; }
    public void markCompleted() { this.status = COMPLETED; }
}
```

### 2. enum + String 변환
```java
public enum FooEnum {
    VALUE_A,
    VALUE_B;

    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static FooEnum from(String value) {
        if (value == null) throw new BadRequestException(ErrorCode.INVALID_X);
        try {
            return FooEnum.valueOf(value.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.INVALID_X);
        }
    }
}
```

### 3. 비동기 트리거 + 워커
```java
// Service
@Transactional
public Response create(...) {
    Entity e = repo.save(...);
    publisher.publishEvent(new EntityCreatedEvent(e.getId()));
    return Response.of(e.getId());
}

// Worker
@Component
public class FooWorker {
    @Async("workerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EntityCreatedEvent event) {
        try { processJob(event.id()); }
        catch (Exception e) { safeMarkFailed(event.id(), e.getMessage()); }
    }

    @Transactional
    protected void processJob(Long id) {
        Entity e = repo.findById(id).orElseThrow();
        if (e.getStatus() != PENDING) return;   // 멱등성
        e.markRunning();
        // ...
        e.markCompleted();
    }
}
```

### 4. Feign 클라이언트 + 인증
```java
@FeignClient(name = "x", url = "${x.base-url}", configuration = XFeignConfig.class)
public interface XFeignClient {
    @PostMapping("/api/{var}")
    Response call(@PathVariable("var") String var, @RequestBody Request body);
}

@RequiredArgsConstructor
public class XFeignConfig {
    private final XProperties properties;

    @Bean
    public RequestInterceptor xAuthInterceptor() {
        return template -> template.header("Authorization", "Bearer " + properties.getApiKey());
    }
}
```

### 5. Spring AI structured output
```java
public Foo callLlm(String system, String user) {
    return chatClient.prompt()
        .system(system)
        .user(user)
        .call()
        .entity(Foo.class);
}

public record Foo(String headline, List<Item> items) {
    public record Item(String name, String description) {}
}
```

### 6. JSON 컬럼 + value record
```java
public record ValueObject(String field1, int field2) {}

@Entity
class ParentEntity {
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private ValueObject value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private List<ValueObject> values;
}
```

---

*이 문서는 코드와 함께 읽을 때 의미가 살아납니다. 실제 파일과 함께 보세요.*