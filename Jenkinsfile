def testExecutor

pipeline {
    agent { label 'built-in' }

    environment {
        APP_NETWORK     = "app_default"
        REST_TEST_IMAGE = "app-rest-tests"

        AWS_ACCESS_KEY_ID     = credentials('jenkins_aws_access_key_id')
        AWS_SECRET_ACCESS_KEY = credentials('jenkins_aws_secret_access_key')
        AWS_DEFAULT_REGION    = "eu-central-1"

        DOCKER_REGISTRY = "docker.io"
    }

    stages {

        stage('Initialise test executor') {
            steps {
                script {
                    testExecutor = load 'scripts/testRunner.groovy'
                }
            }
        }

        stage('Preflight check') {
            steps {
                sh '''#!/bin/bash
                  set -e

                  echo "=== Preflight ==="
                  echo "User: $(whoami)"
                  echo "Node: $(hostname)"
                  echo "Workspace: $(pwd)"

                  command -v docker >/dev/null || {
                    echo "Docker CLI not found"; exit 1;
                  }

                  docker ps >/dev/null || {
                    echo "Docker not usable by this process"; exit 1;
                  }

                  command -v terraform >/dev/null || {
                    echo "Terraform not found"; exit 1;
                  }

                  command -v ansible-playbook >/dev/null || {
                    echo "Ansible not found"; exit 1;
                  }

                  echo "Preflight OK"
                '''
            }
        }

        stage('Provision EC2 with Terraform') {
            steps {
                dir("terraform/ec2") {
                    echo "Provision EC2"

                    sh 'terraform init -upgrade'

                    // Demo choice: always reset infra
                    sh 'terraform destroy -auto-approve || true'
                    sh 'terraform apply -auto-approve'

                    script {
                        env.APP_IP = sh(
                            script: "terraform output -raw app_public_ip",
                            returnStdout: true
                        ).trim()
                    }

                    echo "EC2 IP: ${env.APP_IP}"
                }
            }
        }

        stage('Configure EC2 with Ansible') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    sshagent(['ansible-ssh-key-aws']) {
                        sh '''
                          echo "Waiting for SSH..."
                          for i in {1..30}; do
                            if ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 ubuntu@${APP_IP} "echo SSH ready"; then
                              break
                            fi
                            sleep 5
                          done

                          ansible-galaxy collection install -r ansible/requirements.yaml

                          export DOCKER_PASSWORD

                          ansible-playbook ansible/playbook.yaml \
                            -i "${APP_IP}," \
                            -e "ansible_host=${APP_IP} ansible_user=ubuntu"
                        '''
                    }
                }
            }
        }

        stage('Build and push Docker image') {
            steps {
                script {
                    env.IMAGE_TAG = sh(
                        script: 'git rev-parse --short=7 HEAD',
                        returnStdout: true
                    ).trim()
                }

                withCredentials([
                    usernamePassword(
                        credentialsId: 'docker-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {
                    dir("app") {
                        sh '''#!/bin/bash
                          set -euo pipefail

                          echo "$DOCKER_PASSWORD" | docker login "$DOCKER_REGISTRY" \
                            -u "$DOCKER_USERNAME" --password-stdin

                          docker build -t ecommerce-app:${IMAGE_TAG} .
                          docker tag ecommerce-app:${IMAGE_TAG} \
                            ${DOCKER_REGISTRY}/rodybothe2/ecommerce-app:${IMAGE_TAG}

                          docker push ${DOCKER_REGISTRY}/rodybothe2/ecommerce-app:${IMAGE_TAG}
                        '''
                    }
                }
            }
        }

        stage('Start application stack') {
            steps {
                dir("app") {
                    sh '''#!/bin/bash
                      set -euo pipefail

                      export IMAGE_TAG="${IMAGE_TAG}"
                      docker compose pull || true
                      docker compose up -d
                    '''
                }
            }
        }

        // Future stages intentionally commented out
        // Start application stack
        // Run tests
        // Publish Allure
        // Notifications
    }

    post {
        always {
            dir("app") {
                sh 'docker compose down -v || true'
            }

            dir("terraform/ec2") {
                sh 'terraform destroy -auto-approve || true'
            }
        }

        failure {
            echo "Pipeline failed"
        }
    }
}
