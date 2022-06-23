# Github-Action-Test

### github action 를 이해하기 좋은 글
- https://www.daleseo.com/github-actions-basics/

### github action, ncp(naver cloud platfrom) 활용하여 ci/cd 구축 시도
1. 기존 github action, aws의 ci/cd 과정을 참고하여 
깃헙 액션에서 소스코드를 빌드하여 빌드된 파일을 ncp 오브젝트 저장소에 올리고 ncp sourceDeploy 에서 빌드된 파일을 ncp 서버인스턴스에 배포하는 식으로 하면 될거라고 생각했다.
![image](https://user-images.githubusercontent.com/49400477/175364751-cfc8c7d8-1e47-4352-b573-7b9d1b30d5af.png)

2. 또한 ncp sourceDeploy 문서를 확인하면 ncp 오브젝트 스토리지에 업로드된 소스코드 압축파일을 자동으로 다운받아 배포해준다는 내용이 있어 가능할 것이라고 생각했다. 
![image](https://user-images.githubusercontent.com/49400477/175365811-c3364d56-8f17-4497-88b8-ebb1a6670e0a.png)

3. 하지만 깃헙 액션에서 sourceDeploy 에게 2번과정을 실행시키는 요청에 대한 내용은 문서에서 찾을 수 없었다. 아마 ncp 에서는 깃헙 액션을 활용하여 1번과 같은 배포플로우를 지원하는 구조는 아닌 것같다.
조사를 좀더 해보니 이미 나와 같은 문제를 겪은 사람도 있었다. 내용을 읽어보니 k8s 로는 깃헙 액션을 활용해서 ci/cd 구축이 가능한 것으로 보인다. 다른방법으로 시도를 해봐야겠다.
    - https://jgrammer.tistory.com/entry/%EB%AC%B4%EC%8B%A0%EC%82%AC-%EC%8A%A4%ED%86%A0%EC%96%B4-watcher-CICD-%EB%8F%84%EC%9E%85%EA%B8%B0


### 시도는 해봤으니 기록은 남긴다.
1. workflow trigger
  - master 브랜치에 push, pr 발생시 jobs 의 step 실행된다.(필요하다면 병렬 실행, 의존관계 설정해서 실행도 가능)
```
on:
  push:
    branches: [ "master" ]
  pull_request:
    branches: [ "master" ]
```

2. project build
```
# 해당 액션 job 은 ubuntu-18.04 에서 실행된다. 해당 CI 가 실행되는 서버를 설정하는 것
jobs:
  build:
    runs-on: ubuntu-18.04

    steps:
      # Checkout 액션을 사용(actions/checkout@v3)하면 간편하게 코드 저장소로 부터 CI 서버로 코드를 내려받을 수 있다. 즉 필수과정이다.
      - uses: actions/checkout@v3

      # 자바 JDK 11 설치
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          distribution: 'adopt'
      
      # chmod +x 로 gradlew 에 실행권한 추가
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      # 빌드 실행
      - name: Run build
        run: ./gradlew clean build -x test
```
3. ncp object storage 에 build 파일 zip 으로 압축하여 업로드 
```
      # $GITHUB_SHA 라는 변수는 Github 자체에서 커밋마다 생성하는 랜덤한 변수값이다. 업로드시 빌드파일이 같은이름으로 충돌되지않기 위해 사용한다. 
      - name: Make zip file
        run: zip -r ./$GITHUB_SHA.zip .
        shell: bash

      # 신기하게도 NCP 에서 cli 명령어는 aws 를 사용한다. 빌드 파일을 NCP 오브젝트 스토리지에 업로드한다. 
      - name: Upload build file to NCP
        env:
          AWS_ACCESS_KEY_ID:
          AWS_SECRET_ACCESS_KEY:
          AWS_REGION: kr
        run: aws --endpoint-url=https://kr.object.ncloudstorage.com s3 cp ./$GITHUB_SHA.zip s3://hakjun-bucket/

```

4. 업로드 후 ncp sourceDeploy 에 요청을 보내 ncp object storage zip 파일로 지정된 서버에서 배포 실행. 
```
방법 NOT FOUND
```
