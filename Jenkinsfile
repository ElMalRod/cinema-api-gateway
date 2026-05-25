pipeline {
    agent any

    environment {
        SERVICE_NAME      = 'cinema-api-gateway'
        SERVICE_PORT      = '8085'
        KAFKA_SERVERS     = '18.188.55.33:9092'
        GATEWAY_SERVICES_AUTH  = 'http://cinema-auth-service:8081'
        GATEWAY_SERVICES_USERS = 'http://cinema-users-service:8082'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Coverage') {
            steps {
                sh 'mvn verify'

                script {
                    def coverage = sh(
                        script: '''
                            awk -F"," '
                            NR>1 {
                                missed += $4;
                                covered += $5
                            }
                            END {
                                if (missed + covered > 0)
                                    printf "%.0f", covered * 100 / (missed + covered)
                                else
                                    print "0"
                            }' target/site/jacoco/jacoco.csv
                        ''',
                        returnStdout: true
                    ).trim()

                    echo "Code coverage: ${coverage}%"

                    if (coverage.toInteger() < 85) {
                        error "Coverage ${coverage}% es menor al 85% requerido"
                    }
                }
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${env.SERVICE_NAME}:latest ."
            }
        }

        stage('Transfer Image') {
            steps {
                withCredentials([
                    string(credentialsId: 'EC2_SERVICES_HOST', variable: 'HOST')
                ]) {
                    sshagent(['SSH_DEPLOY_KEY']) {
                        sh '''
                            docker save cinema-api-gateway:latest | gzip > cinema-api-gateway.tar.gz

                            scp -o StrictHostKeyChecking=no \
                                cinema-api-gateway.tar.gz \
                                ubuntu@"$HOST":~/
                        '''
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'EC2_SERVICES_HOST', variable: 'HOST')
                ]) {
                    sshagent(['SSH_DEPLOY_KEY']) {
                        sh '''
                            ssh -o StrictHostKeyChecking=no ubuntu@"$HOST" "
                                docker load < cinema-api-gateway.tar.gz

                                docker stop cinema-api-gateway || true
                                docker rm cinema-api-gateway || true

                                docker run -d \
                                    --name cinema-api-gateway \
                                    --network cinema-network \
                                    -p 8085:8080 \
                                    -e GATEWAY_SERVICES_AUTH="$GATEWAY_SERVICES_AUTH" \
                                    -e GATEWAY_SERVICES_USERS="$GATEWAY_SERVICES_USERS" \
                                    -e SPRING_KAFKA_BOOTSTRAP_SERVERS=18.188.55.33:9092 \
                                    cinema-api-gateway:latest
                            "
                        '''
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                withCredentials([
                    string(credentialsId: 'EC2_SERVICES_HOST', variable: 'HOST')
                ]) {
                    sshagent(['SSH_DEPLOY_KEY']) {
                        sh '''
                            ssh -o StrictHostKeyChecking=no ubuntu@"$HOST" "
                                sleep 20
                                curl -f http://localhost:8085/actuator/health
                            "
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo "cinema-api-gateway desplegado correctamente"
        }

        failure {
            echo "Pipeline falló en cinema-api-gateway"
        }

        always {
            sh 'rm -f cinema-api-gateway.tar.gz || true'
        }
    }
}
