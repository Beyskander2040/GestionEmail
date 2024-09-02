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
        
        // stage('Maven Install') {
        //     steps {
        //         script {
        //             dir('EurekaServeur') {  
        //                 sh 'mvn install'  
        //             }
        //             dir('gateway') {  
        //                         sh 'mvn install -DskipTests'
        //             }
                  
        //             dir('MailBox') {  
        //                         sh 'mvn install -DskipTests'
        //             }
        //             dir('User') {  
        //                         sh 'mvn install -DskipTests'
        //             }
        //               dir('Mail') {  
        //                         sh 'mvn install -DskipTests'
        //             }
        //         }
        //     }
        // }

        // stage('Build Package') {
        //     steps {
        //         script {
        //             dir('EurekaServeur') {  
        //                         sh 'mvn package -DskipTests'
        //             }
        //             dir('gateway') {  
        //                         sh 'mvn package -DskipTests'
        //             }
        //             dir('Mail') {  
        //                         sh 'mvn package -DskipTests'
        //             }
        //             dir('MailBox') {  
        //                         sh 'mvn package -DskipTests'
        //             }
        //             dir('User') {  
        //                         sh 'mvn package -DskipTests'
        //             }
        //         }
        //     }
        // }
        //  stage('SonarQube Analysis') {
        //     steps {
        //         script {
        //             dir('EurekaServeur') {  
        //                 sh "mvn sonar:sonar -Dsonar.login=admin -Dsonar.password=adminadmin"
        //             }
        //             dir('gateway') {  
        //                 sh "mvn sonar:sonar -Dsonar.login=admin -Dsonar.password=adminadmin"
        //             }
        //             dir('MailBox') {  
        //                 sh "mvn sonar:sonar -Dsonar.login=admin -Dsonar.password=adminadmin"
        //             }
        //             dir('User') {  
        //                 sh "mvn sonar:sonar -Dsonar.login=admin -Dsonar.password=adminadmin"
        //             }
        //             dir('Mail') {  
        //                 sh "mvn sonar:sonar -Dsonar.login=admin -Dsonar.password=adminadmin"
        //             }
        //         }
        //     }
        
    // }
    stage('Deploy to Nexus') {
            steps {
                script {
                    // dir('EurekaServeur') {  
                    //     sh 'mvn deploy -DskipTests'
                    // }
                    // dir('gateway') {  
                    //     sh 'mvn deploy -DskipTests'
                    // }
                    // dir('MailBox') {  
                    //     sh 'mvn deploy -DskipTests'
                    // }
                    // dir('User') {  
                    //     sh 'mvn deploy -DskipTests -X'
                    // }
                    dir('Mail') {  
                        sh 'mvn deploy -DskipTests'
                    }
                }
            }
        }
    }
}

