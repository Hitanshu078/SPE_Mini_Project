pipeline {
    agent any

    triggers {
        // GitHub webhook (needs the GitHub plugin and a webhook to <jenkins-url>/github-webhook/)
        githubPush()
        // Fallback: poll every ~2 minutes, also catches commits pushed while the machine was off
        pollSCM('H/2 * * * *')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // A second build waits instead of racing the running deployment
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
    }

    environment {
        // Jenkins credential of type "Username with password" (Docker Hub username + access token).
        // Exposes DOCKERHUB_USR and DOCKERHUB_PSW; both are masked in the console log.
        DOCKERHUB    = credentials('dockerhub-creds')
        APP_NAME     = 'spe-calculator'
        PROXY_PORT   = '8081'
        NOTIFY_EMAIL = 'hitanshu078@gmail.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                sh './mvnw -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh './mvnw -B test'
            }
            post {
                // Publish results even when tests fail; a failure here fails the build
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    docker build \
                      -t "$DOCKERHUB_USR/$APP_NAME:$BUILD_NUMBER" \
                      -t "$DOCKERHUB_USR/$APP_NAME:latest" .
                '''
            }
        }

        stage('Push') {
            steps {
                // Single quotes: the shell expands the secrets, not Groovy, so they never appear in the script
                sh '''
                    echo "$DOCKERHUB_PSW" | docker login -u "$DOCKERHUB_USR" --password-stdin
                    docker push "$DOCKERHUB_USR/$APP_NAME:$BUILD_NUMBER"
                    docker push "$DOCKERHUB_USR/$APP_NAME:latest"
                '''
            }
            post {
                always {
                    sh 'docker logout'
                }
            }
        }

        stage('Deploy') {
            steps {
                // up -d only recreates containers whose image/config changed, so re-runs are safe
                sh '''
                    export IMAGE_NAME="$DOCKERHUB_USR/$APP_NAME"
                    export IMAGE_TAG="$BUILD_NUMBER"
                    docker compose up -d --remove-orphans --wait
                    docker compose ps
                '''
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    for i in $(seq 1 30); do
                      if curl -fsS "http://localhost:$PROXY_PORT/actuator/health" | grep -q UP; then
                        echo "Health check passed"
                        break
                      fi
                      echo "Waiting for service... ($i/30)"
                      sleep 2
                    done
                    curl -fsS "http://localhost:$PROXY_PORT/actuator/health" | grep -q UP
                    echo
                    curl -fsS "http://localhost:$PROXY_PORT/api/v1/add?a=2&b=3"
                    echo
                '''
            }
        }
    }

    post {
        success {
            emailext(
                to: "${env.NOTIFY_EMAIL}",
                subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Build #${env.BUILD_NUMBER} of ${env.JOB_NAME} succeeded.

The calculator stack has been deployed and is healthy.

Build details: ${env.BUILD_URL}"""
            )
        }

        failure {
            emailext(
                to: "${env.NOTIFY_EMAIL}",
                subject: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """Build #${env.BUILD_NUMBER} of ${env.JOB_NAME} failed.

Console output: ${env.BUILD_URL}console""",
                attachLog: true
            )
        }
    }
}
