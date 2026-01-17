def testExecutor

pipeline {
    agent { label 'master' }

    tools {
        allure 'allure'
    }

    environment {
        APP_NETWORK         = "app_default"
        REST_TEST_IMAGE     = "app-rest-tests"
        ALLURE_REPORT       = "${WORKSPACE}/allure-report"

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
        stage('Provision EC2 with Terraform') {
            steps {
                script {
                    dir("terraform/ec2") {
                        echo "Provision EC2"

                        sh 'terraform init -upgrade'

                        sh """
                        terraform destroy -auto-approve \
                          -var="aws_access_key=${AWS_ACCESS_KEY_ID}" \
                          -var="aws_secret_key=${AWS_SECRET_ACCESS_KEY}" \
                          -var="aws_region=${AWS_DEFAULT_REGION}"
                         """

                        sh """
                        terraform apply -auto-approve \
                          -var="aws_access_key=${AWS_ACCESS_KEY_ID}" \
                          -var="aws_secret_key=${AWS_SECRET_ACCESS_KEY}" \
                          -var="aws_region=${AWS_DEFAULT_REGION}"
                        """

                        def appIp = sh(script: "terraform output -raw app_public_ip", returnStdout: true).trim()
                        env.APP_IP = appIp

                        echo "EC2 instances IP:"
                        echo "EC2 IP ${appIp}"
                    }
                }
            }
        }

        stage('Configure EC2 with Ansible') {
            steps {
                script {
                    echo "Configure EC2"
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sshagent(['ansible-ssh-key-aws']) {
                            echo "Running Ansible for ${env.APP_IP}"

                            sh """
                                sleep 30
                                ssh -o StrictHostKeyChecking=no ubuntu@${env.APP_IP} "echo Connected to ${env.APP_IP}"

                                ansible-galaxy collection install -r ansible/requirements.yaml

                                ansible-playbook ansible/playbook.yaml \
                                    -i '${env.APP_IP},' \
                                    -e "ansible_host=${env.APP_IP} ansible_user=ubuntu docker_password=${DOCKER_PASSWORD} nexus_host=${NEXUS_URL}"
                            """
                        }
                    }
                }
            }
        }

        stage('Build and push Docker image') {
            steps {
                script {
                    def imageTag = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = imageTag // We need this to get the latest commit hash

                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                        dir("app") {
                            sh """
                                echo "$DOCKER_PASSWORD" | docker login ${DOCKER_REGISTRY} -u "$DOCKER_USERNAME" --password-stdin

                                docker build -t ecommerce-app:${env.IMAGE_TAG} .
                                docker tag ecommerce-app:${env.IMAGE_TAG} ${DOCKER_REGISTRY}/rodybothe2/ecommerce-app:${env.IMAGE_TAG}

                                docker push ${DOCKER_REGISTRY}/rodybothe2/ecommerce-app:${env.IMAGE_TAG}
                            """
                        }
                    }
                }
            }
        }

        // stage('Start application stack') {
        //     steps {
        //         dir("pipelines/application") {
        //             sh 'docker compose up -d'
        //         }
        //     }
        // }

        // stage('Build Rest assured image') {
        //     steps {
        //         dir("pipelines/rest-assured") {
        //             sh "docker build -t ${REST_TEST_IMAGE} ."
        //         }
        //     }
        // }

        // stage('Run integration Tests') {
        //     steps {
        //         script {
        //             testExecutor.runTestsAndCollectAllure(
        //                     container: 'rest-tests-integration',
        //                     testSelector: 'IntegrationTest',
        //                     resultsDir: 'allure-results/integration',
        //                     network: APP_NETWORK,
        //                     image: REST_TEST_IMAGE
        //             )
        //         }
        //     }
        // }

        // stage('Run e2e tests') {
        //     steps {
        //         script {
        //             testExecutor.runTestsAndCollectAllure(
        //                     container: 'rest-tests-e2e',
        //                     testSelector: 'e2eTest',
        //                     resultsDir: 'allure-results/e2e',
        //                     network: APP_NETWORK,
        //                     image: REST_TEST_IMAGE
        //             )
        //         }
        //     }
        // }

        // stage('Publish Allure report') {
        //     steps {
        //         archiveArtifacts artifacts: 'allure-results/**', fingerprint: true

        //         allure(
        //                 includeProperties: false,
        //                 jdk: '',
        //                 results: [
        //                         [path: 'allure-results/integration'],
        //                         [path: 'allure-results/e2e']
        //                 ]
        //         )
        //     }
        // }

        // stage('Send Slack notification') {
        //     steps {
        //         withCredentials([string(credentialsId: 'SLACK_WEBHOOK', variable: 'SLACK_WEBHOOK_URL')]) {
        //             dir('pipelines/scripts') {
        //                 sh '''
        //                   export SLACK_WEBHOOK_URL="$SLACK_WEBHOOK_URL"
        //                   export ALLURE_REPORT_DIR="${ALLURE_REPORT}"
        //                   ./slack.sh
        //                 '''
        //             }
        //         }
        //     }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'allure-results/**, allure-report/**', fingerprint: true

            dir("pipelines/application") {
                sh 'docker compose down -v'
            }
        }

        failure {
            echo "Pipeline failed"
        }
    }
// }
