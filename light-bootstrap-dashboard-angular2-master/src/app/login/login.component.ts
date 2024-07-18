import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from 'app/Services/user.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent  {



  email: string = '';
  password: string = '';
  error: string = '';
  loginError: string = '';
  showMaxAttemptsMessage: boolean = false;
  constructor(private userService: UserService,private router: Router) {}

  login(): void {
    this.userService.login(this.email, this.password).subscribe(
      response => {
        // Handle successful login
        console.log('Login successful:', response);
        localStorage.setItem('token', response.token); // Store the token
        this.router.navigate(['']); // Redirect to dashboard
      },
      error => {
        // Handle login error
        console.error('Login error:', error);
      }
    );
  }
}
