import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Mail } from 'app/Models/mail';
import { Page } from 'app/Models/page';
import { catchError, Observable, tap, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EmailService {
  private apiUrl = 'http://localhost:8088/email/api/v1/emails';

  constructor(private http: HttpClient) { }
 
  getEmails(userEmail: string, userPassword: string, page: number, size: number): Observable<Mail[]> {
    const url = `${this.apiUrl}/emails?email=${userEmail}&password=${userPassword}&page=${page}&size=${size}`;
    const token = localStorage.getItem('authToken');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
    console.log('Request URL:', url); // Debugging
    console.log('Headers:', headers); // Debugging
    return this.http.get<Mail[]>(url, { headers });
  }

  readEmails(): Observable<string> {
    const url = `${this.apiUrl}/read`;
    return this.http.get<string>(url);
  }
  fetchEmails(mailboxId: number, email: string, password: string): Observable<any> {
    const token = localStorage.getItem('authToken');

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/x-www-form-urlencoded' // Set this to x-www-form-urlencoded for query parameters
    });

    const params = new URLSearchParams();
    params.set('mailboxId', mailboxId.toString());
    params.set('email', email);
    params.set('password', password);

    return this.http.post(`${this.apiUrl}/fetch`, params.toString(), { headers, responseType: 'text' })
      .pipe(
        catchError(err => {
          console.error('Error fetching emails', err);
          return throwError(err);
        })
      );
  }

  


}
