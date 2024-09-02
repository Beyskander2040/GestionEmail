pipeline {
    agent any

    tools {
        maven 'M2_HOME'
     }
    
    environment {
        DOCKERHUB_CREDENTIALS = credentials('benelbeyskander465-dockerhub')
         DOCKER_COMPOSE_VERSION = '1.29.2'
        DOCKER_IMAGE_TAG = "latest" 
    }

    stages {
        stage('Checkout Git repository') {
            steps {
                echo 'Pulling code from Git'
                git branch: 'skander', url: 'https://github.com/Beyskander2040/GestionEmail.git'
            }
        }
        
        // Uncomment these stages if you want to build the Maven packages
        // stage('Maven Install') {
        //     steps {
        //         script {
        //             dir('EurekaServeur') {  
        //                 sh 'mvn install -DskipTests'  
        //             }
        //             dir('gateway') {  
        //                 sh 'mvn install -DskipTests'
        //             }
        //             dir('MailBox') {  
        //                 sh 'mvn install -DskipTests'
        //             }
        //             dir('User') {  
        //                 sh 'mvn install -DskipTests'
        //             }
        //             dir('Mail') {  
        //                 sh 'mvn install -DskipTests'
        //             }
        //         }
        //     }
        // }

        // stage('Build Package') {
        //     steps {
        //         script {
        //             dir('EurekaServeur') {  
        //                 sh 'mvn package -DskipTests'
        //             }
        //             dir('gateway') {  
        //                 sh 'mvn package -DskipTests'
        //             }
        //             dir('Mail') {  
        //                 sh 'mvn package -DskipTests'
        //             }
        //             dir('MailBox') {  
        //                 sh 'mvn package -DskipTests'
        //             }
        //             dir('User') {  
        //                 sh 'mvn package -DskipTests'
        //             }
        //         }
        //     }
        // }
        
        
        
        
                    stage('Docker Build') {
                        steps {
                            script {
                                dir('User') {  
                                    sh 'docker build -t benelbeyskander465/user:1.0 .'  
                                }
                                dir('Mail') {  
                                    sh 'docker build -t benelbeyskander465/mail:1.0 .'  
                                }
                                 dir('gateway') {  
                                      sh 'docker build -t benelbeyskander465/gateway:1.0 .'  
                                 }
                                dir('MailBox') {  
                                    sh 'docker build -t benelbeyskander465/mailbox:1.0 .'  
                                }
                                dir('EurekaServeur') {  
                                    sh 'docker build -t benelbeyskander465/eurekaserveur:1.0 .'  
                                }
                            }
                        }
                    }

                    stage('Docker Push') {
                        steps {
                            script {
                                sh 'docker login -u benelbeyskander465 -p rim21005232'
                                sh 'docker push benelbeyskander465/user:1.0'
                                sh 'docker push benelbeyskander465/mail:1.0'
                                sh 'docker push benelbeyskander465/gateway:1.0'
                                sh 'docker push benelbeyskander465/mailbox:1.0'
                                sh 'docker push benelbeyskander465/eurekaserveur:1.0'
                            }
                        }
                    }
                      stage('Deploy with Docker Compose') {
                    steps {
                     script { 

                     sh 'docker compose up -d'

                     }
                     }
                }
            }
}
    

