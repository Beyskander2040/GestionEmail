import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Mail } from 'app/Models/mail';
import { catchError, Observable, tap, throwError } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EmailService {
  private apiUrl = 'http://localhost:8083/email/api/v1/emails';

  constructor(private http: HttpClient) { }
  getEmails(page: number, size: number): Observable<Mail[]> {
    const url = `${this.apiUrl}/emails?page=${page}&size=${size}`;
    console.log('Calling getEmails with URL:', url); // Add a log statement here
    return this.http.get<Mail[]>(url).pipe(
      tap((response: Mail[]) => {
        console.log('Fetched emails:', response); // Log the response data
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('Error fetching emails:', error);
        return throwError(error); // Pass the error along
      })
    );
  }
  readEmails(): Observable<string> {
    const url = `${this.apiUrl}/read`;
    return this.http.get<string>(url);
  }

}
