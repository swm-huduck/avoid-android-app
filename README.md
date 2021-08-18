# 1. AVOID

<img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/1-readme/readme/src/img_logo.png" style="filter: bright(150%)" width="300px">

HUDuck팀의 애플리케이션, **AVOID**입니다.

**AVOID**는 HUD(Head up display) 디바이스와 블루투스 통신을 통해, 사용자 편의에 맞는 HUD 환경을 구축해줍니다.

# 2. 기능

**AVOID**는 총 4개의 기능으로 이뤄져 있습니다.

1. 내비게이션
2. 내 차 설정
3. 디바이스 연결
4. HUD 표시 항목 설정

## 2.1. 내비게이션 🧭

검색을 통해 원하는 장소를 찾을 수 있습니다.
그 후, 사용자의 화물차 정보를 기반으로 경로를 추천 및 안내합니다. 

### 2.1.1. 플로우 차트

![System_architecture-Final_of_Android-Navigation](https://git.swmgit.org/swm-12/12_swm35/application/-/raw/1-readme/readme/src/System%20architecture-Final%20of%20Android-Navigation.png)

### 2.1.2. GPS 테스트 방법

아래에 링크된 GPS 센서값 변경 애플리케이션을 통해, GPS 테스트를 쉽게 하실 수 있습니다.

[Fake GPS Location - GPS JoyStick](https://play.google.com/store/apps/details?id=com.theappninjas.fakegpsjoystick&hl=ko&gl=US)


### 2.1.3. 구동 화면

|메인|검색|검색 결과|경로 추천|
|:--:|:--:|:--:|:--:|
|![](https://git.swmgit.org/swm-12/12_swm35/application/-/raw/1-readme/readme/src/%EB%A9%94%EC%9D%B8.png)|![](https://git.swmgit.org/swm-12/12_swm35/application/-/raw/1-readme/readme/src/%EA%B2%80%EC%83%89.png)|![](https://git.swmgit.org/swm-12/12_swm35/application/-/raw/1-readme/readme/src/%EA%B2%80%EC%83%89%20%EA%B2%B0%EA%B3%BC.png)|![](https://git.swmgit.org/swm-12/12_swm35/application/-/raw/1-readme/readme/src/%EA%B2%BD%EB%A1%9C%20%EC%B6%94%EC%B2%9C.png)|

#### 2.1.3.1. 길 안내 구동 영상
![](https://git.swmgit.org/swm-12/12_swm35/application/-/raw/1-readme/readme/src/%E1%84%82%E1%85%A2%E1%84%87%E1%85%B5%E1%84%80%E1%85%A6%E1%84%8B%E1%85%B5%E1%84%89%E1%85%A7%E1%86%AB%20%E1%84%89%E1%85%B5%E1%84%8B%E1%85%A7%E1%86%AB.mov)

## 2.2. 내 차 설정 🚚

경로 검색에 필요한 사용자의 화물차 정보를 입력할 수 있습니다.

기본값이 있어, 사용자의 필요에 맞게 입력을 선택할 수 있습니다.

입력되는 정보는 다음과 같습니다.

1. 차량 너비
2. 차량 높이
3. 차량 길이
4. 차량 무게
5. 화물 무게
6. 총 무게 (자동 계산, 차량 무게 + 화물 무게)


## 2.3. 디바이스 연결 📱

블루투스를 통해 HUD 디바이스와 스마트폰의 연결을 돕습니다.

애플리케이션의 사용에서 HUD 디바이스와의 연결이 필수이기 때문에, 블루투스 미연결 상태일 경우 본 기능(디바이스 연결)으로 이동됩니다.

## 2.4. 옵션 설정 ⚙

사용자가 HUD에 표시하고자 하는 옵션을 지정할 수 있습니다.

지정 가능한 옵션은 다음과 같습니다.

1. 위험 경고
    1. 앞 차 급정거 (ON/OFF)
    2. 위험 민감도  (민감/보통/둔감)
    3. [기능 추가 예정]
2. 내비게이션
    1. 속도 (ON/OFF)
    2. 회전 정보 (ON/OFF)
3. 스마트폰 알림
    1. 전화 (ON/OFF)
    2. 문자 (ON/OFF)
    3. 카카오톡 (ON/OFF)

# 3. 프로젝트 시작하기

## 3.1. 개발 환경

PC OS `macOS Big Sur 11.4 (Apple M1)`<br/>
Android Studio `Arctic Fox | 2020.3.1 Beta 3 (aarch64)`<br/>
Android Emulator `Pixel 2 API 30 (Android 11.0, arm64)`<br/>
Android Gradle Plugin Version `7.0.0-beta03`<br/>
Gradle Version `7.0`

## 3.2. API (SDK)

애플리케이션의 구현을 위해 사용된 API 목록입니다.

[NAVER MAPS API]()<br/>
[T map API]()<br/>
[Mapbox Navigation SDK]()

## 3.3. API Key 설정

위에 열거된 API를 사용하기 위해, 반드시 API Key를 입력해야 합니다.

API Key 발급 방법은 API Document를 참고하시면 됩니다.

### 3.3.1. NAVER MAPS API

[NAVER MAPS API Document](https://navermaps.github.io/android-map-sdk/guide-ko/0.html)

`res/values/api_keys.xml` 파일 내부에 클라이언트 ID를 입력합니다.
```xml
<string name="naver_maps_client_id">클라이언트 ID</string>
```

### 3.3.2. T map API

[T map API Document](https://tmapapi.sktelecom.com/main.html#android/guide/androidGuide.sample1)

`res/values/api_keys.xml` 파일 내부에 API Key를 입력합니다.
```xml
<string name="skt_map_api_key">API Key</string>
```

### 3.3.3. Mapbox Navigation SDK

[Mapbox Navigation SDK Document](https://docs.mapbox.com/android/navigation/guides/)

`res/values/api_keys.xml` 파일 내부에 엑세스 토큰을 입력합니다.
```xml
<string name="mapbox_access_token">엑세스 토큰</string>
```

`gradle.properties` 파일 내부에 다운로드 토큰을 입력합니다.
```gradle
MAPBOX_DOWNLOADS_TOKEN=다운로드 토큰
```

