pipeline {
    agent any

    environment {
            // PORT
            EXTERNAL_PORT_BLUE = credentials('EXTERNAL_PORT_BLUE')
            EXTERNAL_PORT_GREEN = credentials('EXTERNAL_PORT_GREEN')
    }

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
                    DOCKER_HUB_DEV_CREDENTIAL_ID = 'DOCKER_HUB_DEV_CREDENTIALS'
                    DOCKER_HUB_PROD_CREDENTIAL_ID = 'DOCKER_HUB_PROD_CREDENTIALS'

                    // SSH
                    SSH_CREDENTIAL_ID = OPERATION_ENV.toUpperCase() + '_SSH'
                    SSH_PORT_CREDENTIAL_ID = OPERATION_ENV.toUpperCase() + '_SSH_PORT'
                    SSH_HOST_CREDENTIAL_ID = OPERATION_ENV.toUpperCase() + '_SSH_HOST'

                    // ENVIRONMENT CONFIG FILE
                    ENVIRONMENT_CONFIG_FILE = 'application-' + OPERATION_ENV + '.yml'
                }
            }
        }

        stage('Parse Internal Port') {
            steps {
                script {
                    INTERNAL_PORT = sh(script: "yq e '.server.port' ./src/main/resources/${ENVIRONMENT_CONFIG_FILE}", returnStdout: true).trim()
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
                        remote.port = PORT as Integer
                        remote.allowAnyHosts = true

                        // SSH 연결 테스트
                        sshCommand remote: remote, command: 'echo "SSH 연결 성공"'

                        // Docker 이미지 pull
                        sshCommand remote: remote, command: "docker pull ${DOCKER_HUB_ID}/${PROJECT_NAME}-${OPERATION_ENV}:latest"

                        // 환경변수를 넘기고 deploy-${OPERATION_ENV}.sh 실행
                        sshCommand remote: remote, command: """
                            export OPERATION_ENV=${OPERATION_ENV} && \
                            export INTERNAL_PORT=${INTERNAL_PORT} && \
                            export EXTERNAL_PORT_GREEN=${EXTERNAL_PORT_GREEN} && \
                            export EXTERNAL_PORT_BLUE=${EXTERNAL_PORT_BLUE} && \
                            export DOCKER_IMAGE_NAME=${DOCKER_HUB_ID}/${PROJECT_NAME}-${OPERATION_ENV}:latest && \
                            cd /home/ubuntu/deployment && \
                            chmod +x deploy-${OPERATION_ENV}.sh && \
                            ./deploy-${OPERATION_ENV}.sh
                        """
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
