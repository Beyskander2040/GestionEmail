pipeline {
    agent any

    tools {
        maven 'M2_HOME'
    }

    stages {
        stage('Checkout Git repository') {
            steps {
                echo 'Pulling code from Git'
                git branch: 'skander', url: 'https://github.com/Beyskander2040/GestionEmail.git'
            }
        }
        
        stage('Maven Install') {
            steps {
                script {
                    dir('EurekaServeur') {  
                        sh 'mvn install'  
                    }
                    dir('gateway') {  
                        sh 'mvn install'  
                    }
                    dir('Mail') {  
                        sh 'mvn install'  
                    }
                    dir('MailBox') {  
                        sh 'mvn install'  
                    }
                    dir('User') {  
                        sh 'mvn install'  
                    }
                }
            }
        }

        stage('Build Package') {
            steps {
                script {
                    dir('EurekaServeur') {  
                        sh 'mvn package'  
                    }
                    dir('gateway') {  
                        sh 'mvn package'  
                    }
                    dir('Mail') {  
                        sh 'mvn package'  
                    }
                    dir('MailBox') {  
                        sh 'mvn package'  
                    }
                    dir('User') {  
                        sh 'mvn package'  
                    }
                }
            }
        }
    }
}
