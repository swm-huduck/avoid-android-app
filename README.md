<img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/AVOiD-background.png" width="100%">

# 1. AVOiD

HUDuck팀의 AR HUD와 운전자 사이의 징검다리, **AVOiD**입니다.

**AVOiD**는 HUD(Head up display) 디바이스와 블루투스 통신을 통해, 운전자(사용자, 이하 '사용자') 편의에 맞는 HUD 환경을 구축해줍니다.

# 2. 기능

**AVOiD**는 총 4개의 기능으로 이뤄져 있습니다.

1. 내비게이션
2. 내 차 설정
3. 디바이스 연결
4. HUD 표시 항목 설정

## 2.1. 내비게이션 🧭

1. 검색을 통해 원하는 장소를 찾을 수 있습니다.
2. 사용자의 화물차 정보를 기반으로 경로를 추천 및 안내합니다.
3. AR 내비게이션을 위한 경로 정보를 실시간으로 HUD 디바이스에 전달합니다. 

### 2.1.1. 플로우 차트

<img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Android%20System%20Architecture-Navigation.png" width="1100px">

### 2.1.2. GPS 테스트 방법

아래에 링크된 GPS 센서값 변경 애플리케이션을 통해, GPS 테스트를 쉽게 하실 수 있습니다.

[Fake GPS Location - GPS JoyStick](https://play.google.com/store/apps/details?id=com.theappninjas.fakegpsjoystick&hl=ko&gl=US)


### 2.1.3. 구동 화면

<table>
<thead>
    <th width="33%">메인</th>
    <th width="33%">검색</th>
    <th width="33%">검색 결과</th>
</thead>
<tbody>
    <tr>
        <td width="33.3%">
            <img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20Navigation%2001.jpg">
        </td>
        <td width="33.3%">
            <img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20Navigation%2002.jpg">
        </td>
        <td width="33.3%">
            <img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20Navigation%2003.jpg">
        </td>
    </tr>
    <tr>
        <td>
            현재 위치를 표시한다.
        </td>
        <td>
            목적지를 검색할 수 있다.<br/>추천 검색어와 자음 검색을 지원한다.
        </td>
        <td>
            검색된 목적지의 이름, 업종, 주소, 지도상 위치를 표시한다.
        </td>
    </tr>
</tbody>
</table>

<table>
<thead>
    <th width="33%">거리뷰</th>
    <th width="33%">경로 안내</th>
</thead>
<tbody>
    <tr>
        <td width="33.3%">
            <img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20Navigation%2004.jpg">
        </td>
        <td width="33.3%">
            <img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20Navigation%2005.jpg">
        </td>
        <td width="33.3%">
        </td>
    </tr>
    <tr>
        <td>
            선택된 장소의 거리뷰를 보여준다.
        </td>
        <td>
            7가지의 경로 탐색 옵션(고속도로 우선, 무료 우선 등)을 통해 경로를 추천한다.
        </td>
    </tr>
</tbody>
</table>

|길 안내 시연 영상|
|:-:|
|[![시연 영상](http://img.youtube.com/vi/uEwq6NTwg50/0.jpg)](https://youtu.be/uEwq6NTwg50):|
|자체 구현 내비게이션으로 사용자의 위치를 기반으로 길을 안내한다.<br/>AR 내비게이션을 위한 회전 이벤트(좌회전, 우회전 등)의 거리 정보를 실시간으로 HUD에 전송한다.|

## 2.2. 내 차 설정 🚚

내비게이션 경로 검색에 필요한 사용의 화물차 정보를 입력할 수 있습니다.

기본값이 있어, 사용자의 필요에 맞게 입력을 선택할 수 있습니다.

입력되는 정보는 다음과 같습니다.

1. 차량 너비
2. 차량 높이
3. 차량 길이
4. 차량 무게
5. 화물 무게
6. 총 무게 (자동 계산, 차량 무게 + 화물 무게)

### 2.2.1. 플로우 차트

<img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Android%20System%20Architecture-My%20Car.png" width="600px">

### 2.2.2. 구동 화면

<table>
<thead>
    <th>내 차 설정 前</th>
    <th>내 차 설정 後</th>
</thead>
<tbody>
    <tr>
        <td width="33.3%">
            <img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20My%20Car%2001.jpg">
        </td>
        <td width="33.3%">
            <img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20My%20Car%2002.jpg">
        </td>
        <td width="33.3%">
        </td>
    </tr>
    <tr>
        <td colspan="2">
            사용자 화물차의 정보를 입력하여, 내비게이션에서 화물차가 원활히 지날 수 있는 경로를 추천한다.<br/>설정 내용은 화물이 변경될 경우, 화물 중량만 변경하면 된다.
        </td>
    </tr>
</tbody>
</table>


## 2.3. 디바이스 연결 📱

블루투스(BLE, Bluetooth low energy)를 통해 HUD 디바이스와 스마트폰의 연결을 돕습니다.

한 번 등록한 HUD 디바이스는 애플리케이션을 실행하면 자동으로 연결됩니다.

### 2.3.1. 플로우 차트

<img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Android%20System%20Architecture-HUD%20Device.png" width="1000px">

### 2.3.2. 구동 화면
<table>
    <thead>
        <th>HUD 디바이스 연결 확인</th>
    </thead>
    <tbody>
        <tr>
            <td width="33.3%">
                <img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/1-readme/readme/src/hud%20%EC%97%B0%EA%B2%B0.png">
            </td>
            <td width="33.3%">
            </td>
            <td width="33.3%">
            </td>
        </tr>
        <tr>
            <td>
                스마트폰과 HUD 디바이스의 블루투스 연결을 관리할 수 있다.
                한 번 등록된 HUD 디바이스의 정보는 저장되어 AVOiD 실행 시 자동으로 연결된다.
            </td>
        </tr>
    </tbody>
</table>

## 2.4. 옵션 설정 ⚙

사용자가 HUD에 표시하고자 하는 옵션을 지정할 수 있습니다.

지정 가능한 옵션은 다음과 같습니다.

1. 위험 경고
    1. 앞 차 급정거 (ON/OFF)
    2. 위험 민감도  (민감/보통/둔감)
2. 내비게이션
    1. 속도 (ON/OFF)
    2. 회전 정보 (ON/OFF)
3. 스마트폰 알림
    1. 전화 (ON/OFF)
    2. 문자 (ON/OFF)
    3. 카카오톡 (ON/OFF)

### 2.4.1. 플로우 차트

<img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Android%20System%20Architecture-Setting.png" width="300px">

### 2.4.2. 구동 화면

<table>
    <thead>
        <th>HUD 표시 옵션 설정</th>
    </thead>
    <tbody>
        <tr>
            <td>
                <img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/1-readme/readme/src/hud%20%ED%91%9C%EC%8B%9C%20%EC%98%B5%EC%85%98.png">
            </td>
        </tr>
        <tr>
            <td>
                HUD에 표시할 기능을 제어할 수 있다.<br/>
                변경 내용은 즉시 HUD 디바이스에 전송되어 적용된다.<br/>
                본 기능을 통해 운전자의 전방 주시를 최대화한다.
            </td>
        </tr>
    </tbody>
</table>

# 3. UI/UX

## 3.1. 와이어 프레임

<img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20Wire%20Frame%2001.png" width="100%">

## 3.2. 스토리 보드

<img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20Wire%20Frame%2002.png" width="100%">

## 3.3. 흐름도

<img src="https://git.swmgit.org/swm-12/12_swm35/application/-/raw/readme/readme_src/Application%20Wire%20Frame%2003.png" width="100%">

# 4. 프로젝트 시작하기

## 4.1. 개발 환경

PC OS `macOS Big Sur 11.6 (Apple M1)`<br/>
Android Studio `Arctic Fox | 2020.3.1 Beta 3 (aarch64)`<br/>
Android Emulator `Pixel 2 API 30 (Android 11.0, arm64)`<br/>
Android Gradle Plugin Version `7.0.0-beta03`<br/>
Gradle Version `7.0`

## 4.2. API (SDK)

애플리케이션의 구현을 위해 사용된 API 목록입니다.

[NAVER MAPS API]()<br/>
[T map API]()<br/>

## 4.3. API Key 설정

위에 열거된 API를 사용하기 위해, 반드시 API Key를 입력해야 합니다.

API Key 발급 방법은 API Document를 참고하시면 됩니다.

### 4.3.1. NAVER MAPS API

[NAVER MAPS API Document](https://navermaps.github.io/android-map-sdk/guide-ko/0.html)

`res/values/api_keys.xml` 파일 내부에 클라이언트 ID를 입력합니다.
```xml
<string name="naver_maps_client_id">클라이언트 ID</string>
```

### 4.3.2. T map API

[T map API Document](https://tmapapi.sktelecom.com/main.html#android/guide/androidGuide.sample1)

`res/values/api_keys.xml` 파일 내부에 API Key를 입력합니다.
```xml
<string name="skt_map_api_key">API Key</string>
```
