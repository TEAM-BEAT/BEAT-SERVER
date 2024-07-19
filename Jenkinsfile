pipeline {
    agent any

    stages {
        stage('Set Variables') {
            steps {
                echo 'Set Variables'
                script {
                    // BASIC
                    PROJECT_NAME = 'beat'
                    REPOSITORY_URL = 'https://github.com/TEAM-BEAT/BEAT-SERVER.git'
                    PROD_BRANCH = 'main'
                    DEV_BRANCH = 'develop'
                    BRANCH_NAME = env.BRANCH_NAME
                    OPERATION_ENV = BRANCH_NAME.equals(PROD_BRANCH) ? 'prod' : 'dev'

                    // DOCKER
                    DOCKER_HUB_URL = 'registry.hub.docker.com'
                    DOCKER_HUB_FULL_URL = 'https://' + DOCKER_HUB_URL
                    DOCKER_HUB_DEV_CREDENTIAL_ID = 'DOCKER_HUB_DEV_CREDENTIALS'
                    DOCKER_HUB_PROD_CREDENTIAL_ID = 'DOCKER_HUB_PROD_CREDENTIALS'
                    DOCKER_IMAGE_NAME = BRANCH_NAME.equals(PROD_BRANCH) ? 'donghoon0203/beat-prod' : 'hoonyworld/beat-dev'

                    // SSH
                    SSH_CREDENTIAL_ID = OPERATION_ENV.toUpperCase() + '_SSH'
                    SSH_PORT_CREDENTIAL_ID = OPERATION_ENV.toUpperCase() + '_SSH_PORT'
                    SSH_HOST_CREDENTIAL_ID = OPERATION_ENV.toUpperCase() + '_SSH_HOST'

                    // PORT
                    PORT_PROPERTIES_FILE = 'application-' + OPERATION_ENV + '.yml'
                }
            }
        }

        stage('Parse Internal Port') {
            steps {
                script {
                    INTERNAL_PORT = sh(script: "yq e '.server.port' ./src/main/resources/${PORT_PROPERTIES_FILE}", returnStdout: true).trim()
                    echo "Internal port: ${INTERNAL_PORT}"
                }
            }
        }

        stage('Git Checkout') {
            steps {
                echo 'Checkout Remote Repository'
                git branch: "${env.BRANCH_NAME}",
                    url: REPOSITORY_URL
            }
        }

        stage('Deploy to Server') {
            steps {
                echo 'Deploy to Server'
                script {
                    def DOCKER_HUB_CREDENTIAL_ID = BRANCH_NAME.equals(PROD_BRANCH) ? DOCKER_HUB_PROD_CREDENTIAL_ID : DOCKER_HUB_DEV_CREDENTIAL_ID
                    withCredentials([
                        usernamePassword(credentialsId: DOCKER_HUB_CREDENTIAL_ID,
                                         usernameVariable: 'DOCKER_HUB_ID',
                                         passwordVariable: 'DOCKER_HUB_PW'),
                        sshUserPrivateKey(credentialsId: SSH_CREDENTIAL_ID,
                                          keyFileVariable: 'KEY_FILE',
                                          usernameVariable: 'USERNAME'),
                        string(credentialsId: SSH_HOST_CREDENTIAL_ID, variable: 'HOST'),
                        string(credentialsId: SSH_PORT_CREDENTIAL_ID, variable: 'PORT')]) {

                        def remote = [:]
                        remote.name = OPERATION_ENV
                        remote.host = HOST
                        remote.user = USERNAME
                        remote.identityFile = KEY_FILE
                        remote.port = 22
                        remote.allowAnyHosts = true

                        // SSH 연결 테스트
                        sshCommand remote: remote, command: 'echo "SSH 연결 성공"'

                        // Docker 이미지 pull
                        sshCommand remote: remote, command: 'docker pull ' + DOCKER_IMAGE_NAME + ":latest"

                        // 기존 컨테이너 제거
                        sshCommand remote: remote, failOnError: false, command: 'docker rm -f springboot'

                        // 새로운 컨테이너 실행
                        sshCommand remote: remote, command: (
                            'docker run -d --name springboot' +
                            ' --network beat-network' +
                            ' -p 8080:' + INTERNAL_PORT +
                            ' -e "SPRING_PROFILES_ACTIVE=' + OPERATION_ENV + '"' +
                            ' ' + DOCKER_IMAGE_NAME + ':latest'
                        )
                    }
                }
            }
        }
    }

    post {
        success {
            sh 'echo "success"'
            slackSend(color: '#00FF00', message: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
        unsuccessful {
            sh 'echo "fail"'
            slackSend(color: '#FF0000', message: "FAIL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
        }
    }
}