pipeline {
    agent any

    environment {
        PROJECT_NAME = 'beat'
        REPOSITORY_URL = 'https://github.com/TEAM-BEAT/BEAT-SERVER.git'
        PROD_BRANCH = 'main'
        DEV_BRANCH = 'develop'
        DOCKER_HUB_URL = 'registry.hub.docker.com'
        DOCKER_HUB_FULL_URL = "https://${DOCKER_HUB_URL}"
        DOCKER_HUB_DEV_CREDENTIAL_ID = 'DOCKER_HUB_DEV_CREDENTIALS'
        DOCKER_HUB_PROD_CREDENTIAL_ID = 'DOCKER_HUB_PROD_CREDENTIALS'
    }

    stages {
        stage('Set Variables') {
            steps {
                script {
                    // Get the current branch name
                    BRANCH_NAME = env.GIT_BRANCH ? env.GIT_BRANCH : sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                    echo "Current branch: ${BRANCH_NAME}"

                    OPERATION_ENV = BRANCH_NAME == PROD_BRANCH ? 'prod' : 'dev'
                    DOCKER_IMAGE_NAME = BRANCH_NAME == PROD_BRANCH ? 'donghoon0203/beat-Prod' : 'hoonyworld/beat-dev'
                    SSH_CREDENTIAL_ID = OPERATION_ENV.toUpperCase() + '_SSH'
                    SSH_PORT_CREDENTIAL_ID = OPERATION_ENV.toUpperCase() + '_SSH_PORT'
                    SSH_HOST_CREDENTIAL_ID = OPERATION_ENV.toUpperCase() + '_SSH_HOST'
                    PORT_PROPERTIES_FILE = 'application-' + OPERATION_ENV + '.yml'

                    echo "Operation environment: ${OPERATION_ENV}"
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
                git branch: BRANCH_NAME,
                    url: REPOSITORY_URL
            }
        }

        stage('Deploy to Server') {
            steps {
                echo 'Deploy to Server'
                script {
                    def DOCKER_HUB_CREDENTIAL_ID = BRANCH_NAME == PROD_BRANCH ? DOCKER_HUB_PROD_CREDENTIAL_ID : DOCKER_HUB_DEV_CREDENTIAL_ID
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

                        // Docker 이미지 pull
                        sshCommand remote: remote, command:
                            'docker pull ' + DOCKER_IMAGE_NAME + ":latest"

                        // 기존 컨테이너 제거
                        sshCommand remote: remote, failOnError: false,
                            command: 'docker rm -f springboot'

                        // 새로운 컨테이너 실행
                        sshCommand remote: remote, command:
                            ('docker run -d --name springboot'
                            + ' -p 80:' + INTERNAL_PORT
                            + ' -e "SPRING_PROFILES_ACTIVE=' + OPERATION_ENV + '"'
                            + ' ' + DOCKER_IMAGE_NAME + ':latest')
                    }
                }
            }
        }
    }
}