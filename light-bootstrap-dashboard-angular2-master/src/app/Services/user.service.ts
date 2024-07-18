import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private isAuthenticated: boolean = false;
  private apiUrl = 'http://localhost:8088/User/api/v1/auth';

  constructor(private http: HttpClient) { }

  login(email: string, password: string): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    // Assuming login endpoint returns something indicating success (e.g., JWT token)
    return this.http.post(`${this.apiUrl}/login`, { email, password }, { headers }).pipe(
      tap((response: any) => {
        // Example: Assuming response includes a token or user data indicating successful login
        if (response && response.token) {
          this.isAuthenticated = true; // Set isAuthenticated based on successful login response
        }
      })
    );
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }
  isLoggedIn(): boolean {
    return this.isAuthenticated;
  }
}
