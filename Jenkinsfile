pipeline {
    agent any

    environment {
        SERVICE_NAME     = 'cinema-api-gateway'
        SERVICE_PORT     = '8085'
        EC2_HOST         = credentials('EC2_SERVICES_HOST')
        SSH_KEY          = credentials('SSH_DEPLOY_KEY')
        KAFKA_SERVERS    = '18.188.55.33:9092'
        AUTH_SERVICE_URL = 'http://cinema-auth-service:8081'
        USERS_SERVICE_URL= 'http://cinema-users-service:8082'
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
                                missed += $4; covered += $5
                            }
                            END {
                                if (missed+covered > 0)
                                    printf "%.0f", covered*100/(missed+covered)
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
                sh "docker build -t ${SERVICE_NAME}:latest ."
            }
        }

        stage('Transfer Image') {
            steps {
                sh """
                    docker save ${SERVICE_NAME}:latest | gzip > ${SERVICE_NAME}.tar.gz
                    scp -i ${SSH_KEY} \
                        -o StrictHostKeyChecking=no \
                        ${SERVICE_NAME}.tar.gz \
                        ubuntu@${EC2_HOST}:~/
                """
            }
        }

        stage('Deploy') {
            steps {
                sh """
                    ssh -i ${SSH_KEY} \
                        -o StrictHostKeyChecking=no \
                        ubuntu@${EC2_HOST} '
                            docker load < ${SERVICE_NAME}.tar.gz
                            docker stop ${SERVICE_NAME} || true
                            docker rm ${SERVICE_NAME} || true
                            docker run -d \
                                --name ${SERVICE_NAME} \
                                --network cinema-network \
                                -p ${SERVICE_PORT}:8085 \
                                -e AUTH_SERVICE_URL=${AUTH_SERVICE_URL} \
                                -e USERS_SERVICE_URL=${USERS_SERVICE_URL} \
                                -e SPRING_KAFKA_BOOTSTRAP_SERVERS=${KAFKA_SERVERS} \
                                ${SERVICE_NAME}:latest
                        '
                """
            }
        }

        stage('Health Check') {
            steps {
                sh 'sleep 20'
                sh """
                    curl -f http://${EC2_HOST}:${SERVICE_PORT}/actuator/health \
                        || error "Health check falló"
                """
            }
        }
    }

    post {
        success {
            echo "${SERVICE_NAME} desplegado correctamente"
        }
        failure {
            echo "Pipeline falló en ${SERVICE_NAME}"
        }
        always {
            sh "rm -f ${SERVICE_NAME}.tar.gz || true"
        }
    }
}