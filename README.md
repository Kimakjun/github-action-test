**Github Actions?** 
- github actions 란 github 에서 제공하는 CI/CD 를 편리하게 구축할 수 있도록 도와주는 서비스 
- 자세한 내용: https://www.daleseo.com/github-actions-basics/
- 문서: https://docs.github.com/en/actions
- backend github actions test 레포: https://github.com/Kimakjun/github-action-test
- frontend github actions test 레포: https://github.com/bsideproject/11-02-frontend/tree/test/github-action

**Github Actions 를 활용한 CI/CD 구조** 
- github actions 을 활용한 CI/CD 의 전체적인 flow
<img width="974" alt="image" src="https://user-images.githubusercontent.com/49400477/175823083-264b1ec5-8872-4ed2-853c-5f85a669cb3d.png">

**위 플로우를 실행시키기 위한 yml 파일 구조** 
```
- 코드 변화 감지(트리거 역할)
 
- 수행할 작업들(jobs)
  - build 작업
    -  build 를 진행할 환경
    -  build 를 위한 step 들
    
  - deploy 작업
    - deploy 를 진행할 환경 
    - deploy 를 위한 step 들 
```

**Actions?** 
  - Github Actions 반복 단계를 재사용하기 용이하도록 제공되는 일종의 작업 공유 메커니즘. 젠킨스로 치면 유용한 플러그인 같은 것.
해당 테스트에서도 여러 actions 를 사용. 
    - actions/checkout@v3: 깃헙 레포에 소스코드를 Github Actions 실행환경으로 가져오기 위한 actions
    - actions/setup-java@v2: Github Actions 실행환경에 자바를 설치해주는 actions
    - docker/login-action@v1: 도커 hub 에 로그인하기위한 actions
  
**Github Actions Runners?**
  - GitHub Actions workflow으로부터 job을 실행시켜주는 어플리케이션이다. 위 **테스트에서는 deploy 작업을 실행시켜주도록 NCP 서버를 Runner 로 등록**하였다. 배포작업를 NCP 서버(Runner)에서 실행하도록 하여 어플리케이션을 실행시킨다. 
  - Runner 로 등록된 NCP 서버는 GitHub Actions workflow 를 listen 하고 있다가 job 을 진행할 환경이 자신이라면(runs-on) 
  해당 job 의 step 들을 자신의 서버에서 실행시킨다. 
   
**전체 코드** 
```
name: CI

## 트리거 설정
  - 마스터 브랜치에 push, pr 발생하면 jobs 실행.
on:
  push:
    branches: [ "master" ]
  pull_request:
    branches: [ "master" ]

## build 과정
  - ubuntu-latest 을 실행환경으로 설정
  - 깃헙 레포의 소스코드를 ubuntu-latest 환경으로 체크아웃
  - 스프링 프로젝트 빌드를 위한 JDK 11 설치
  - 빌드, 도커 헙 로그인 및 빌드된 결과물로 이미지 생성 & 푸쉬
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run build
        run: ./gradlew clean build -x test

      - name: Login to DockerHub
        uses: docker/login-action@v1
        with:
          username: ${{secrets.DOCKERHUB_USERNAME}}
          password: ${{secrets.DOCKERHUB_TOKEN}}

      - name: Docker build & push to push
        run: |
          docker build -t ${{secrets.DOCKER_REPO}} .
          docker tag ${{secrets.DOCKER_REPO}}:latest ${{secrets.DOCKERHUB_USERNAME}}/${{secrets.DOCKER_REPO}}:latest
          docker push ${{secrets.DOCKERHUB_USERNAME}}/${{secrets.DOCKER_REPO}}
          
## deploy 과정
  - needs 를 build 로 두어 build 이후에 실행되도록 의존관계 설정
  - runs-on 를 self-hosted, label-go 설정. ncp 서버를 깃헙 러너로 label-go 라벨과 함께 등록했기 때문에 해당 작업(deploy)이 ncp 서버에서 실행  
  - 도커 로그인 및 이미 띄워진 컨테이너 삭제
  - 위 build 과정에서 push 된 이미지 pull 및 컨테이너 실행
  deploy:
    needs: build
    name: Deploy
    runs-on: [ self-hosted, label-go ]
    steps:
      - name: Login to DockerHub
        uses: docker/login-action@v1
        with:
          username: ${{secrets.DOCKERHUB_USERNAME}}
          password: ${{secrets.DOCKERHUB_TOKEN}}
      - name: Docker run
        run: |
          RESULT=$(docker ps -q -a)
          if [ -z "$RESULT" ];
          then
            echo $RESULT
          else
            docker rm -f $(docker ps -q -a)
          fi
          docker pull ${{secrets.DOCKERHUB_USERNAME}}/${{secrets.DOCKER_REPO}}
          docker run -itd -p 80:8080 --name hjspring ${{secrets.DOCKERHUB_USERNAME}}/${{secrets.DOCKER_REPO}}:latest

```


**테스트** 
  - **소스 코드 Push 이후 자동으로 빌드 배포 진행**
    <img width="899" alt="image" src="https://user-images.githubusercontent.com/49400477/175824471-5fdd3518-2e5b-4b45-9c83-772de8ae486d.png">
    <img width="1193" alt="image" src="https://user-images.githubusercontent.com/49400477/175825120-0f315f14-50ea-4730-a301-989fd7a31df3.png">

  - **배포 후 도커 컨테이너 및 어플리케이션 실행 확인** 
    <img width="1001" alt="image" src="https://user-images.githubusercontent.com/49400477/175825012-e983f6ef-0ad1-448b-8846-0455bc508d30.png">

  







